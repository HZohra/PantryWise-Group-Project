require('./helpers');
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { mapClaudeError } = require('../src/services/claudeService');
const { buildMessages } = require('../src/services/chatService');
const p = require('../src/utils/prompts');

test('mapClaudeError: SDK failures become safe AppErrors', () => {
  const cases = [
    [{ name: 'APIConnectionTimeoutError' }, 504, 'AI_TIMEOUT'],
    [{ name: 'APIConnectionError' }, 503, 'AI_UNAVAILABLE'],
    [{ status: 429 }, 429, 'AI_RATE_LIMITED'],
    [{ status: 401 }, 502, 'AI_CONFIG_ERROR'],
    [{ status: 400 }, 502, 'AI_REQUEST_FAILED'],
    [{ status: 500 }, 503, 'AI_UNAVAILABLE'],
  ];
  for (const [error, status, code] of cases) {
    const mapped = mapClaudeError(error);
    assert.equal(mapped.statusCode, status, code);
    assert.equal(mapped.code, code);
  }
});

test('buildMessages: starts with user, merges same-role turns, never changes the input', () => {
  const history = [
    { role: 'assistant', content: 'Welcome!' },
    { role: 'user', content: 'a' },
    { role: 'user', content: 'b' },
    { role: 'assistant', content: 'c' },
  ];

  assert.deepEqual(buildMessages(history, 'd'), [
    { role: 'user', content: 'a\n\nb' },
    { role: 'assistant', content: 'c' },
    { role: 'user', content: 'd' },
  ]);
  assert.equal(history[1].content, 'a');
  assert.deepEqual(buildMessages([{ role: 'user', content: 'a' }], 'b'), [{ role: 'user', content: 'a\n\nb' }]);
});

test('describeItem: amount and expiry status for the prompt', () => {
  assert.equal(p.describeItem({ name: 'milk', quantity: 1, unit: 'L', daysUntilExpiry: 2 }),
    '- milk (1 L) - EXPIRING SOON: expires in 2 days');
  assert.equal(p.describeItem({ name: 'egg', quantity: 6, unit: '', daysUntilExpiry: 1 }),
    '- egg (6) - EXPIRING SOON: expires in 1 day');
  assert.equal(p.describeItem({ name: 'ham', quantity: null, unit: '', daysUntilExpiry: 0 }),
    '- ham - EXPIRING SOON: expires today');
  assert.equal(p.describeItem({ name: 'cream', quantity: null, unit: '', daysUntilExpiry: -1 }), '- cream - EXPIRED');
  assert.equal(p.describeItem({ name: 'salt', quantity: null, unit: '', daysUntilExpiry: null }), '- salt');
});

test('describePreferences: allergies first, fitness goal explained, empty is "(none saved)"', () => {
  const text = p.describePreferences({ allergies: ['peanut'], health: ['vegan'], fitnessGoal: 'build-muscle' });
  assert.equal(text.split('\n')[0], '- ALLERGIES (never use): peanut');
  assert.ok(text.includes('protein-rich'));
  assert.equal(p.describePreferences({}), '(none saved)');
});

test('buildChatSystemPrompt: app language and most urgent items first', () => {
  const prompt = p.buildChatSystemPrompt(
    [{ name: 'rice', quantity: null, unit: '', daysUntilExpiry: null },
      { name: 'milk', quantity: null, unit: '', daysUntilExpiry: 1 }],
    {}, 'es',
  );
  assert.ok(prompt.includes('Reply in Spanish'));
  assert.ok(prompt.indexOf('- milk') < prompt.indexOf('- rice'));
});