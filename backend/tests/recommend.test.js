const { rawRecipe, okResponse, installFakeFetch, item, noPreferences } = require('./helpers');
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const r = require('../src/services/recommendationService');
const recipes = require('../src/services/recipeService');
const { clearCache } = require('../src/services/edamamService');

beforeEach(() => clearCache());

// ---------------------------------------------------------------- matching and scoring

test('normalizeName: lowercase, no digits or symbols, simple plurals', () => {
  assert.equal(r.normalizeName('2% Whole Milk!'), 'whole milk');
  assert.equal(r.normalizeName('Eggs'), 'egg');
  assert.equal(r.normalizeName('Tomatoes'), 'tomato');
  assert.equal(r.normalizeName('Berries'), 'berry');
  assert.equal(r.normalizeName('hummus'), 'hummus');
});

test('namesMatch: every word of one name in the other', () => {
  assert.equal(r.namesMatch('red onion', 'onion'), true);
  assert.equal(r.namesMatch('peanut butter', 'peanut'), true);
  assert.equal(r.namesMatch('rice', 'spinach'), false);
});

test('preparePantry: expired items are never offered', () => {
  const pantry = r.preparePantry([item('milk', -1), item('egg', 2), item('rice')]);
  assert.deepEqual(pantry.map((p) => p.key), ['egg', 'rice']);
});

test('scoreRecipe: staples are ignored and expiring items add urgency points', () => {
  const pantry = r.preparePantry([item('spinach', 1), item('egg', 5)]);
  const match = r.scoreRecipe({ ingredientNames: ['spinach', 'eggs', 'salt'] }, pantry);
  assert.equal(match.haveCount, 2);
  assert.equal(match.totalCount, 2);
  assert.deepEqual(match.missing, []);
  assert.deepEqual(match.expiringUsed, [{ name: 'spinach', daysUntilExpiry: 1 }]);
  assert.equal(match.score, 130); // full coverage 100 + spinach in 1 day (3 points x 10)
});

test('scoreRecipe: missing ingredients are listed', () => {
  const pantry = r.preparePantry([item('spinach')]);
  const match = r.scoreRecipe({ ingredientNames: ['spinach', 'feta'] }, pantry);
  assert.deepEqual(match.missing, ['feta']);
  assert.equal(match.score, 50);
});

test('rankRecipes: Zero-Waste weights put expiring food ahead of full coverage', () => {
  const inventory = [item('rice'), item('bean'), item('spinach', 0)];
  const full = { recipeId: 'full', ingredientNames: ['rice', 'bean'] };
  const rescue = { recipeId: 'rescue', ingredientNames: ['spinach', 'feta'] };

  assert.deepEqual(r.rankRecipes([full, rescue], inventory).map((x) => x.recipeId), ['full', 'rescue']);
  assert.deepEqual(r.rankRecipes([full, rescue], inventory, r.ZERO_WASTE_WEIGHTS).map((x) => x.recipeId),
    ['rescue', 'full']);
});

test('rankRecipes: ties go to the quicker recipe, unknown time last, inputs unchanged', () => {
  const inventory = [item('rice')];
  const slow = { recipeId: 'slow', ingredientNames: ['rice'], totalTimeMinutes: 40 };
  const quick = { recipeId: 'quick', ingredientNames: ['rice'], totalTimeMinutes: 10 };
  const unknown = { recipeId: 'unknown', ingredientNames: ['rice'], totalTimeMinutes: null };

  const ranked = r.rankRecipes([unknown, slow, quick], inventory);
  assert.deepEqual(ranked.map((x) => x.recipeId), ['quick', 'slow', 'unknown']);
  assert.equal('match' in slow, false);
});

// ---------------------------------------------------------------- search terms and filters

test('chooseSearchTerms: the query wins, otherwise the 2 most urgent usable items', () => {
  const inventory = [item('milk', -1), item('egg', 5), item('spinach', 1), item('rice')];
  assert.deepEqual(recipes.chooseSearchTerms(inventory, 'pasta'), ['pasta']);
  assert.deepEqual(recipes.chooseSearchTerms(inventory), ['spinach', 'egg']);
});

test('broadenSearch: drops soft filters but never diet, and returns null when nothing changes', () => {
  const terms = ['pasta'];
  assert.equal(recipes.broadenSearch({ inventory: [], query: 'pasta', preferences: noPreferences(), terms }), null);

  const wider = recipes.broadenSearch({
    inventory: [], query: 'pasta', terms,
    preferences: { ...noPreferences(), diet: 'low-fat', cuisineType: 'italian' },
  });
  assert.equal(wider.preferences.diet, 'low-fat');
  assert.equal(wider.preferences.cuisineType, undefined);
});

test('removeRecipesWithAllergens: "peanuts" also removes recipes with peanut butter', () => {
  const kept = recipes.removeRecipesWithAllergens([
    { recipeId: 'a', ingredientNames: ['peanut butter', 'bread'] },
    { recipeId: 'b', ingredientNames: ['jam'] },
  ], ['peanuts']);
  assert.deepEqual(kept.map((x) => x.recipeId), ['b']);
});

// ---------------------------------------------------------------- full pipeline with a fake Edamam

const request = (overrides) => ({
  inventory: [], query: undefined, preferences: noPreferences(), limit: 5, mode: 'standard', ...overrides,
});

test('recommendRecipes: searches urgent items, drops allergens and duplicates, ranks the rest', async () => {
  const queries = installFakeFetch((q) => okResponse({
    spinach: [
      rawRecipe({ id: 'a', label: 'Spinach Omelette', source: 'BBC', ingredients: ['spinach', 'egg', 'salt'] }),
      rawRecipe({ id: 'b', label: 'Peanut Noodles', ingredients: ['spinach', 'peanut butter', 'noodle'] }),
    ],
    egg: [
      rawRecipe({ id: 'c', label: 'Spinach Omelette', source: 'BBC', ingredients: ['spinach', 'egg'] }),
      rawRecipe({ id: 'd', label: 'Boiled Egg', ingredients: ['egg', 'water'] }),
    ],
  }[q] || []));

  const result = await recipes.recommendRecipes(request({
    inventory: [item('spinach', 1), item('egg', 5)],
    preferences: { ...noPreferences(), allergies: ['peanut'] },
  }));

  assert.deepEqual(queries.sort(), ['egg', 'spinach']);
  assert.deepEqual(result.recipes.map((x) => x.recipeId), ['edamam:a', 'edamam:d']);
  assert.equal(result.personalized, true);
  assert.equal(result.broadened, false);
});

test('recommendRecipes: Zero-Waste keeps only recipes that rescue expiring food', async () => {
  installFakeFetch((q) => okResponse({
    spinach: [rawRecipe({ id: 's', ingredients: ['spinach', 'feta'] })],
    rice: [rawRecipe({ id: 'r', ingredients: ['rice'] })],
  }[q] || []));
  const inventory = [item('spinach', 0), item('rice')];

  const standard = await recipes.recommendRecipes(request({ inventory }));
  const zeroWaste = await recipes.recommendRecipes(request({ inventory, mode: 'zeroWaste' }));

  assert.deepEqual(standard.recipes.map((x) => x.recipeId), ['edamam:r', 'edamam:s']);
  assert.deepEqual(zeroWaste.recipes.map((x) => x.recipeId), ['edamam:s']);
});

test('recommendRecipes: no results broadens once by dropping the last word', async () => {
  const queries = installFakeFetch((q) => okResponse(
    q === 'chicken tikka' ? [rawRecipe({ id: 't', ingredients: ['chicken'] })] : [],
  ));

  const result = await recipes.recommendRecipes(request({ query: 'chicken tikka masala' }));

  assert.deepEqual(queries, ['chicken tikka masala', 'chicken tikka']);
  assert.equal(result.broadened, true);
  assert.deepEqual(result.searchedFor, ['chicken tikka']);
  assert.equal(result.recipes.length, 1);
});

test('recommendRecipes: an empty pantry gets default ideas and is not personalized', async () => {
  installFakeFetch(() => okResponse([]));

  const result = await recipes.recommendRecipes(request());

  assert.equal(result.personalized, false);
  assert.deepEqual(result.searchedFor, ['pasta', 'soup']);
});

test('recommendRecipes: when every search fails, the Edamam error reaches the caller', async () => {
  installFakeFetch(() => { throw new TypeError('fetch failed'); });

  await assert.rejects(recipes.recommendRecipes(request({ query: 'pasta' })),
    (err) => err.statusCode === 503 && err.code === 'RECIPES_UNAVAILABLE');
});