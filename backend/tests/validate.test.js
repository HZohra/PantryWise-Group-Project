const { test } = require('node:test');
const assert = require('node:assert/strict');
const v = require('../src/middleware/validate');
const AppError = require('../src/utils/AppError');

const has = (errors, text) => errors.some((e) => e.includes(text));

test('chat: message is required', () => {
  assert.ok(has(v.checkChatRequest({}).errors, 'message is required'));
});

test('chat: a message over the limit is rejected', () => {
  const { errors } = v.checkChatRequest({ message: 'a'.repeat(v.LIMITS.messageMax + 1) });
  assert.ok(has(errors, `at most ${v.LIMITS.messageMax}`));
});

test('chat: a valid request is cleaned (trimmed text, lowercase inventory, default locale)', () => {
  const { errors, clean } = v.checkChatRequest({
    message: '  what can I cook?  ',
    inventory: [{ name: ' Milk ', quantity: 1, unit: 'L', daysUntilExpiry: 2 }],
  });
  assert.deepEqual(errors, []);
  assert.equal(clean.message, 'what can I cook?');
  assert.deepEqual(clean.inventory, [{ name: 'milk', quantity: 1, unit: 'L', daysUntilExpiry: 2 }]);
  assert.equal(clean.locale, 'en');
});

test('chat: history entries need role user/assistant and content', () => {
  const { errors } = v.checkChatRequest({ message: 'hi', history: [{ role: 'system', content: 'x' }] });
  assert.ok(has(errors, 'history[0]'));
});

test('inventory: negative quantity and fractional days are rejected', () => {
  const { errors } = v.checkChatRequest({
    message: 'hi',
    inventory: [{ name: 'a', quantity: -1 }, { name: 'b', daysUntilExpiry: 1.5 }],
  });
  assert.ok(has(errors, 'inventory[0].quantity'));
  assert.ok(has(errors, 'inventory[1].daysUntilExpiry'));
});

test('locale: es-MX becomes es, unsupported fr falls back to en, garbage is an error', () => {
  assert.equal(v.checkChatRequest({ message: 'x', locale: 'es-MX' }).clean.locale, 'es');
  const french = v.checkChatRequest({ message: 'x', locale: 'fr' });
  assert.equal(french.clean.locale, 'en');
  assert.deepEqual(french.errors, []);
  assert.ok(has(v.checkChatRequest({ message: 'x', locale: 'not a locale!' }).errors, 'locale'));
});

test('preferences: allergies are lowercased, single-spaced and de-duplicated', () => {
  const { errors, clean } = v.checkChatRequest({
    message: 'x', preferences: { allergies: ['Tree  Nuts', 'tree nuts', 'Maní'] },
  });
  assert.deepEqual(errors, []);
  assert.deepEqual(clean.preferences.allergies, ['tree nuts', 'maní']);
});

test('preferences: allergies with symbols are rejected', () => {
  const { errors } = v.checkChatRequest({ message: 'x', preferences: { allergies: ['<script>'] } });
  assert.ok(has(errors, 'preferences.allergies'));
});

test('preferences: allow-listed values are case-insensitive and unknown ones rejected', () => {
  const ok = v.checkChatRequest({ message: 'x', preferences: { cuisine: 'Italian', health: ['Vegan'] } });
  assert.equal(ok.clean.preferences.cuisineType, 'italian');
  assert.deepEqual(ok.clean.preferences.health, ['vegan']);
  const bad = v.checkChatRequest({ message: 'x', preferences: { cuisine: 'martian' } });
  assert.ok(has(bad.errors, 'preferences.cuisine must be one of'));
});

test('preferences: maxCalories outside 50-3000 is rejected', () => {
  const { errors } = v.checkChatRequest({ message: 'x', preferences: { maxCalories: 10 } });
  assert.ok(has(errors, 'maxCalories'));
});

test('ai recipe: a pantry with only expired food is rejected', () => {
  const { errors } = v.checkAiRecipeRequest({ inventory: [{ name: 'milk', daysUntilExpiry: -2 }] });
  assert.ok(has(errors, 'at least one item that is not expired'));
});

test('ai recipe: servings default to 2 and must be 1-12', () => {
  assert.equal(v.checkAiRecipeRequest({ inventory: [{ name: 'rice' }] }).clean.servings, 2);
  const { errors } = v.checkAiRecipeRequest({ inventory: [{ name: 'rice' }], servings: 13 });
  assert.ok(has(errors, 'servings'));
});

test('search: q is required and a repeated parameter is rejected', () => {
  assert.ok(has(v.checkRecipeSearchQuery({}).errors, 'q is required'));
  assert.ok(has(v.checkRecipeSearchQuery({ q: ['a', 'b'] }).errors, 'given only once'));
});

test('search: health list and maxTime are parsed, limit defaults to 10', () => {
  const { errors, clean } = v.checkRecipeSearchQuery({ q: 'pasta', health: 'vegan,Gluten-Free', maxTime: '30' });
  assert.deepEqual(errors, []);
  assert.deepEqual(clean.health, ['vegan', 'gluten-free']);
  assert.equal(clean.maxTimeMinutes, 30);
  assert.equal(clean.limit, 10);
});

test('search: limit above 20 is rejected', () => {
  assert.ok(has(v.checkRecipeSearchQuery({ q: 'pasta', limit: '50' }).errors, 'limit must be'));
});

test('recommend: defaults (limit 5, standard) and an unknown mode is rejected', () => {
  const { errors, clean } = v.checkRecommendRequest({});
  assert.deepEqual(errors, []);
  assert.equal(clean.limit, 5);
  assert.equal(clean.mode, 'standard');
  assert.ok(has(v.checkRecommendRequest({ mode: 'turbo' }).errors, 'mode must be one of'));
});

test('substitute: recipeTitle and ingredient are required', () => {
  const { errors } = v.checkSubstituteRequest({});
  assert.ok(has(errors, 'recipeTitle is required'));
  assert.ok(has(errors, 'ingredient is required'));
});

test('validateBody: replaces req.body when valid, passes a 400 AppError when not', () => {
  const middleware = v.validateBody(v.checkRecommendRequest);

  let error = 'not called';
  const good = { body: { limit: 3 } };
  middleware(good, {}, (err) => { error = err; });
  assert.equal(error, undefined);
  assert.equal(good.body.limit, 3);
  assert.equal(good.body.mode, 'standard');

  middleware({ body: { limit: 99 } }, {}, (err) => { error = err; });
  assert.ok(error instanceof AppError);
  assert.equal(error.statusCode, 400);
  assert.equal(error.code, 'VALIDATION_ERROR');
  assert.ok(error.details.length > 0);
});


test('recommend: hideAllergens defaults to false and must be true or false', () => {
  assert.equal(v.checkRecommendRequest({}).clean.hideAllergens, false);
  assert.equal(v.checkRecommendRequest({ hideAllergens: true }).clean.hideAllergens, true);
  assert.ok(has(v.checkRecommendRequest({ hideAllergens: 'yes' }).errors, 'hideAllergens'));
});