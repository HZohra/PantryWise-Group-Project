const { installFakeClaude, item, noPreferences } = require('./helpers');
const fakeClaude = installFakeClaude(); // before the service loads
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const s = require('../src/services/substituteService');

beforeEach(() => fakeClaude.reset());

const input = (overrides) => ({
  recipeTitle: 'Pancakes', ingredient: 'butter', ingredientLines: ['1 cup flour'],
  inventory: [], preferences: noPreferences(), locale: 'en', ...overrides,
});

const reply = (substitutes, tip = '') => JSON.stringify({ substitutes, tip });

test('extractJson: code fences, text around it, braces inside strings, cut-off replies', () => {
  assert.deepEqual(s.extractJson('```json\n{"a":1}\n```'), { a: 1 });
  assert.deepEqual(s.extractJson('Here it is: {"a":"}"} hope it helps'), { a: '}' });
  assert.equal(s.extractJson('{"a": 1'), null);
  assert.equal(s.extractJson('no json here'), null);
});

test('local: dairy-free removes yogurt, pantry items come first, no Claude call', async () => {
  const result = await s.suggestSubstitutes(input({
    inventory: [item('olive oil')],
    preferences: { health: ['dairy-free'], allergies: [] },
  }));

  assert.equal(result.source, 'local');
  assert.equal(fakeClaude.calls.length, 0);
  assert.equal(result.substitutes[0].name, 'olive oil');
  assert.equal(result.substitutes[0].inPantry, true);
  assert.ok(!result.substitutes.some((x) => x.name === 'plain yogurt'));
  assert.ok(result.substitutes.length <= 3);
});

test('local: Spanish texts, and vegan removes the dairy option for eggs', () => {
  const found = s.findLocalSubstitutes('Eggs', { health: ['vegan'], allergies: [] }, 'es');
  assert.ok(!found.some((x) => x.name === 'plain yogurt'));
  assert.ok(found[0].amount.includes('linaza'));
});

test('local: "peanut butter" is not mistaken for "butter"', () => {
  assert.equal(s.findLocalSubstitutes('peanut butter', noPreferences(), 'en'), null);
});

test('ai: allergens are dropped, names lowercased, pantry items first', async () => {
  fakeClaude.queue(reply([
    { name: 'Sunflower Seed Butter', amount: '1:1', notes: 'Same texture.' },
    { name: 'Peanut Butter', amount: '1:1', notes: 'Classic.' },
    { name: 'Greek Yogurt', amount: '1:1', notes: 'Tangy.' },
  ], 'Stir well.'));

  const result = await s.suggestSubstitutes(input({
    ingredient: 'tahini',
    inventory: [item('greek yogurt')],
    preferences: { health: [], allergies: ['peanut'] },
  }));

  assert.equal(result.source, 'ai');
  assert.deepEqual(result.substitutes.map((x) => x.name), ['greek yogurt', 'sunflower seed butter']);
  assert.equal(result.substitutes[0].inPantry, true);
  assert.equal(result.tip, 'Stir well.');
});

test('ai: an unusable reply is retried once', async () => {
  fakeClaude.queue('Sorry, let me think', reply([{ name: 'miso', amount: '1 tbsp', notes: '' }]));

  const result = await s.suggestSubstitutes(input({ ingredient: 'tahini' }));

  assert.equal(fakeClaude.calls.length, 2);
  assert.equal(result.substitutes[0].name, 'miso');
});

test('ai: two unusable replies give AI_BAD_FORMAT', async () => {
  fakeClaude.queue('nope', '{"substitutes": [');

  await assert.rejects(s.suggestSubstitutes(input({ ingredient: 'tahini' })),
    (err) => err.statusCode === 502 && err.code === 'AI_BAD_FORMAT');
});

test('local options all ruled out by diet and allergies: falls back to Claude', async () => {
  fakeClaude.queue(reply([{ name: 'coconut cream', amount: 'same amount', notes: '' }]));

  const result = await s.suggestSubstitutes(input({
    ingredient: 'sour cream',
    preferences: { health: ['dairy-free'], allergies: ['cashew'] },
  }));

  assert.equal(result.source, 'ai');
  assert.equal(fakeClaude.calls.length, 1);
  assert.equal(result.substitutes[0].name, 'coconut cream');
});