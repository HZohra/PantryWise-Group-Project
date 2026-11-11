const { installFakeClaude, item, noPreferences } = require('./helpers');
const fakeClaude = installFakeClaude(); // before the service loads
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const chat = require('../src/services/chatService');

beforeEach(() => fakeClaude.reset());

const input = (overrides = {}) => ({
  message: 'Add the missing items to my shopping list',
  history: [],
  inventory: [item('rice'), item('yogurt', 2)],
  preferences: noPreferences(),
  locale: 'en',
  ...overrides,
});

const toolCall = (items) => ({ name: 'add_to_shopping_list', input: { items } });

test('chat: Claude gets the shopping-list tool; a normal answer has no items', async () => {
  fakeClaude.queue('Try a yogurt rice bowl!');

  const result = await chat.getChatReply(input({ message: 'What can I cook?' }));

  assert.equal(result.reply, 'Try a yogurt rice bowl!');
  assert.deepEqual(result.shoppingItems, []);
  assert.equal(fakeClaude.calls[0].tools[0].name, 'add_to_shopping_list');
});

test('chat: proposed items are cleaned (lowercase, trimmed, no duplicates)', async () => {
  fakeClaude.queue({
    text: 'Here you go, confirm below.',
    toolCalls: [toolCall([
      { name: '  Garlic ', amount: ' 2 cloves ' },
      { name: 'garlic', amount: '1 head' },
      { name: 'Olive Oil' },
      { name: '' },
    ])],
  });

  const result = await chat.getChatReply(input());

  assert.deepEqual(result.shoppingItems, [
    { name: 'garlic', amount: '2 cloves' },
    { name: 'olive oil', amount: '' },
  ]);
});

test('chat: items matching an allergy are never proposed', async () => {
  fakeClaude.queue({
    text: 'Confirm below.',
    toolCalls: [toolCall([{ name: 'peanut butter' }, { name: 'bread' }])],
  });

  const result = await chat.getChatReply(input({ preferences: { health: [], allergies: ['peanuts'] } }));

  assert.deepEqual(result.shoppingItems.map((i) => i.name), ['bread']);
});

test('chat: a tool call without text gets a short reply in the app language', async () => {
  fakeClaude.queue({ text: '', toolCalls: [toolCall([{ name: 'cilantro' }])] });

  const result = await chat.getChatReply(input({ locale: 'es' }));

  assert.ok(result.reply.startsWith('Estos son los productos'));
  assert.equal(result.shoppingItems.length, 1);
});

test('chat: no text and no usable items is an empty reply error', async () => {
  fakeClaude.queue({ text: '', toolCalls: [toolCall([{ name: 'peanut' }])] });

  await assert.rejects(
    chat.getChatReply(input({ preferences: { health: [], allergies: ['peanut'] } })),
    (err) => err.code === 'AI_EMPTY_REPLY',
  );
});

test('readShoppingItems: ignores other tools and keeps at most 30 items', () => {
  const many = Array.from({ length: 40 }, (_, i) => ({ name: `food ${String.fromCharCode(97 + Math.floor(i / 26))}${String.fromCharCode(97 + (i % 26))}` })); // letters only: digits are ignored when comparing names
  const items = chat.readShoppingItems([{ name: 'other_tool', input: { items: [{ name: 'x' }] } }, toolCall(many)]);

  assert.equal(items.length, 30);
  assert.ok(!items.some((i) => i.name === 'x'));
});