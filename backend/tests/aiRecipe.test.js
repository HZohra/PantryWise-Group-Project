const { installFakeClaude, item, noPreferences } = require('./helpers');
const fakeClaude = installFakeClaude(); // before the service loads
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const ai = require('../src/services/aiRecipeService');
const AppError = require('../src/utils/AppError');

beforeEach(() => fakeClaude.reset());

const INVENTORY = [item('rice'), item('yogurt', 2), item('chicken', 1), item('milk', -1)];

const goodReply = (overrides = {}) => JSON.stringify({
  title: 'Yogurt Chicken',
  servings: 2,
  timeMinutes: 30,
  ingredients: [
    { name: 'Chicken', amount: '500 g' },
    { name: 'yogurt', amount: '2 tbsp' },
    { name: 'rice', amount: '1 cup' },
    { name: 'garlic', amount: '2 cloves' },
    { name: 'salt', amount: '' },
  ],
  steps: ['1. Cut the chicken', '2) Mix with yogurt', 'Cook and serve'],
  tip: 'Eat it today.',
  ...overrides,
});

const generate = (overrides = {}) => ai.generateAiRecipe({
  inventory: INVENTORY, preferences: noPreferences(), locale: 'en', servings: 2, ...overrides,
});

test('readRecipe: too few steps is unusable, out-of-range servings become null', () => {
  assert.equal(ai.readRecipe(goodReply({ steps: ['only one'] })), null);
  assert.equal(ai.readRecipe('not json'), null);
  assert.equal(ai.readRecipe(goodReply({ servings: 99 })).servings, null);
});

test('findAllergen: "peanuts" catches "peanut butter"', () => {
  assert.equal(ai.findAllergen([{ name: 'rice' }, { name: 'peanut butter' }], ['peanuts']), 'peanut butter');
  assert.equal(ai.findAllergen([{ name: 'rice' }], ['peanuts']), null);
});

test('generateAiRecipe: our code marks pantry, expiring and staple items; step numbers removed', async () => {
  fakeClaude.queue(goodReply());

  const result = await generate();
  const byName = Object.fromEntries(result.recipe.ingredients.map((i) => [i.name, i]));

  assert.ok(result.recipe.recipeId.startsWith('ai:'));
  assert.deepEqual(result.recipe.steps, ['Cut the chicken', 'Mix with yogurt', 'Cook and serve']);
  assert.equal(byName.chicken.inPantry && byName.chicken.expiring, true);
  assert.equal(byName.rice.inPantry, true);
  assert.equal(byName.rice.expiring, false);
  assert.equal(byName.garlic.inPantry, false);
  assert.equal(byName.salt.staple, true);
  assert.equal(result.recipe.ingredientLines[0], '500 g chicken');
  assert.equal(result.usedExpiring, true);
  assert.deepEqual(result.expiringItems, ['chicken', 'yogurt']);

  const sent = fakeClaude.calls[0].messages[0].content;
  assert.ok(sent.includes('chicken'));
  assert.ok(!sent.includes('milk')); // expired food is never sent
});

test('generateAiRecipe: an unusable reply is retried once with a stricter note', async () => {
  fakeClaude.queue('Here is a lovely recipe: {', goodReply());

  const result = await generate();

  assert.equal(fakeClaude.calls.length, 2);
  assert.ok(fakeClaude.calls[1].messages[0].content.includes('could not be used'));
  assert.equal(result.recipe.title, 'Yogurt Chicken');
});

test('generateAiRecipe: an allergen in both answers gives AI_UNSAFE_RECIPE', async () => {
  fakeClaude.queue(goodReply(), goodReply());

  await assert.rejects(generate({ preferences: { health: [], allergies: ['garlic'] } }),
    (err) => err.statusCode === 502 && err.code === 'AI_UNSAFE_RECIPE');
});

test('generateAiRecipe: two unusable answers give AI_BAD_FORMAT', async () => {
  fakeClaude.queue('nope', 'still nope');

  await assert.rejects(generate(), (err) => err.statusCode === 502 && err.code === 'AI_BAD_FORMAT');
});

test('generateAiRecipe: a Claude error is passed on without retrying', async () => {
  fakeClaude.queue(new AppError(429, 'AI_RATE_LIMITED', 'busy'));

  await assert.rejects(generate(), (err) => err.code === 'AI_RATE_LIMITED');
  assert.equal(fakeClaude.calls.length, 1);
});

test('generateAiRecipe: nothing expiring still gives a recipe from what the user has', async () => {
  fakeClaude.queue(goodReply());

  const result = await generate({ inventory: [item('rice'), item('chicken', 10)] });

  assert.equal(result.usedExpiring, false);
  assert.deepEqual(result.expiringItems, []);
  assert.ok(fakeClaude.calls[0].messages[0].content.includes('Nothing is expiring soon'));
});