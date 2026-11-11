const { rawRecipe, okResponse, errorResponse, installFakeFetch } = require('./helpers');
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const edamam = require('../src/services/edamamService');

beforeEach(() => edamam.clearCache());

test('normalizeRecipe: id from the uri, unknown time is null, nutrition per serving', () => {
  const recipe = edamam.normalizeRecipe(rawRecipe({
    id: 'abc', label: 'Omelette', ingredients: ['Egg', 'egg', 'Spinach'], servings: 2, totalTime: 0, calories: 800,
  }));
  assert.equal(recipe.recipeId, 'edamam:abc');
  assert.equal(recipe.totalTimeMinutes, null);
  assert.deepEqual(recipe.ingredientNames, ['egg', 'spinach']);
  assert.deepEqual(recipe.nutrition, { perServing: true, calories: 400, protein: 20, carbs: 30, fat: 10 });
});

test('buildNutrition: without servings the whole-recipe totals are kept', () => {
  assert.deepEqual(edamam.buildNutrition({ calories: 800, totalNutrients: {} }, null),
    { perServing: false, calories: 800, protein: null, carbs: null, fat: null });
});

test('mapEdamamError: timeouts, rate limits and bad keys get safe codes', () => {
  assert.equal(edamam.mapEdamamError({ name: 'TimeoutError' }).code, 'RECIPES_TIMEOUT');
  assert.equal(edamam.mapEdamamError(null, 429).code, 'RECIPES_RATE_LIMITED');
  assert.equal(edamam.mapEdamamError(null, 401).code, 'RECIPES_CONFIG_ERROR');
  assert.equal(edamam.mapEdamamError(null, 500).statusCode, 503);
});

test('searchRecipes: maxCalories uses the per-serving value and drops unknown calories', async () => {
  installFakeFetch(() => okResponse([
    rawRecipe({ id: 'light', calories: 500 }),   // 250 per serving
    rawRecipe({ id: 'heavy', calories: 1000 }),  // 500 per serving
    rawRecipe({ id: 'unknown', calories: 0 }),   // unknown
  ]));

  const found = await edamam.searchRecipes({ query: 'soup', maxCalories: 300 });

  assert.deepEqual(found.map((x) => x.recipeId), ['edamam:light']);
});

test('searchRecipes: the same search is answered from the cache (one Edamam call)', async () => {
  const queries = installFakeFetch(() => okResponse([rawRecipe({ id: 'a' })]));

  await edamam.searchRecipes({ query: 'Soup' });
  await edamam.searchRecipes({ query: 'soup ' });
  await edamam.searchRecipes({ query: 'pasta' });

  assert.equal(queries.length, 2); // "Soup" and "soup " share a cache entry
});

test('searchRecipes: a failure is not cached, so a retry asks Edamam again', async () => {
  const queries = installFakeFetch((q, call) => {
    if (call === 1) throw new TypeError('fetch failed');
    return okResponse([rawRecipe({ id: 'a' })]);
  });

  await assert.rejects(edamam.searchRecipes({ query: 'soup' }), (err) => err.code === 'RECIPES_UNAVAILABLE');
  const found = await edamam.searchRecipes({ query: 'soup' });

  assert.equal(found.length, 1);
  assert.equal(queries.length, 2);
});

test('searchRecipes: an HTTP 401 becomes RECIPES_CONFIG_ERROR', async () => {
  installFakeFetch(() => errorResponse(401));

  await assert.rejects(edamam.searchRecipes({ query: 'soup' }),
    (err) => err.statusCode === 502 && err.code === 'RECIPES_CONFIG_ERROR');
});