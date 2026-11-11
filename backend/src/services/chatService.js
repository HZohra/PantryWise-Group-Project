const { generateReplyWithTools } = require('./claudeService');
const { buildChatSystemPrompt } = require('../utils/prompts');
const { normalizeName, namesMatch } = require('./recommendationService');
const AppError = require('../utils/AppError');

const MAX_REPLY_TOKENS = 1024; // about 350 words: enough for a short recipe, keeps cost predictable
const MAX_SHOPPING_ITEMS = 30;

/**
 * Lets Claude propose shopping-list items in a structured way. Nothing is added by the server:
 * the app shows the items and the user confirms.
 */
const SHOPPING_TOOL = Object.freeze({
  name: 'add_to_shopping_list',
  description: 'Propose items for the user\'s shopping list. Use it ONLY when the user asks to add '
    + 'things to their shopping list (or clearly agrees to it). The app shows the items and asks the '
    + 'user to confirm, so never say they were already added.',
  input_schema: {
    type: 'object',
    properties: {
      items: {
        type: 'array',
        maxItems: MAX_SHOPPING_ITEMS,
        items: {
          type: 'object',
          properties: {
            name: { type: 'string', description: 'Lowercase English food name, without amount, e.g. "garlic".' },
            amount: { type: 'string', description: 'Short amount such as "2 cloves" or "1 kg", or "".' },
          },
          required: ['name'],
        },
      },
    },
    required: ['items'],
  },
});

/** Shown when Claude only called the tool and wrote no text. */
const LIST_ONLY_REPLY = Object.freeze({
  en: 'Here are the items for your shopping list. Confirm below to add them.',
  es: 'Estos son los productos para tu lista de compras. Confírmalos abajo para añadirlos.',
});

/**
 * Turns the app's history + new message into a valid Claude message list:
 * it must start with "user", and roles must alternate.
 * Leading assistant turns are dropped; back-to-back same-role turns are merged.
 */
function buildMessages(history, message) {
  const messages = [];
  for (const turn of history) {
    if (messages.length === 0 && turn.role !== 'user') continue;
    const last = messages[messages.length - 1];
    if (last && last.role === turn.role) {
      last.content += `\n\n${turn.content}`;
    } else {
      messages.push({ role: turn.role, content: turn.content }); // new object: never modify req.body
    }
  }
  const last = messages[messages.length - 1];
  if (last && last.role === 'user') {
    last.content += `\n\n${message}`;
  } else {
    messages.push({ role: 'user', content: message });
  }
  return messages;
}

/** Trimmed string capped at `max` characters; anything that isn't a string becomes "". */
const shortText = (value, max) => (typeof value === 'string' ? value.trim().slice(0, max) : '');

/**
 * The items Claude proposed, cleaned: lowercase names, short amounts, no duplicates,
 * at most 30, and never anything that matches the user's allergies.
 * @returns {{name: string, amount: string}[]}
 */
function readShoppingItems(toolCalls = [], allergies = []) {
  const blocked = allergies.map(normalizeName).filter(Boolean);
  const seen = new Set();
  const items = [];
  for (const call of toolCalls) {
    if (call.name !== SHOPPING_TOOL.name || !Array.isArray(call.input.items)) continue;
    for (const raw of call.input.items) {
      if (!raw || typeof raw !== 'object') continue;
      const name = shortText(raw.name, 60).toLowerCase();
      const key = normalizeName(name);
      if (!key || seen.has(key)) continue;
      if (blocked.some((allergy) => namesMatch(key, allergy))) {
        console.warn('[chat] dropped a shopping item that matches an allergy');
        continue;
      }
      seen.add(key);
      items.push({ name, amount: shortText(raw.amount, 40) });
      if (items.length === MAX_SHOPPING_ITEMS) return items;
    }
  }
  return items;
}

/**
 * @param {{message: string, history: object[], inventory: object[], preferences: object, locale: string}} input
 *        already validated by checkChatRequest
 * @returns {Promise<{reply: string, shoppingItems: {name: string, amount: string}[]}>}
 * @throws {AppError} from claudeService, or AI_EMPTY_REPLY
 */
async function getChatReply({ message, history, inventory, preferences, locale }) {
  const { text, toolCalls } = await generateReplyWithTools({
    system: buildChatSystemPrompt(inventory, preferences, locale),
    messages: buildMessages(history, message),
    maxTokens: MAX_REPLY_TOKENS,
    tools: [SHOPPING_TOOL],
  });
  const shoppingItems = readShoppingItems(toolCalls, preferences.allergies);
  const reply = text || (shoppingItems.length > 0 ? (LIST_ONLY_REPLY[locale] ?? LIST_ONLY_REPLY.en) : '');
  if (!reply) {
    throw new AppError(502, 'AI_EMPTY_REPLY', 'The assistant returned an empty reply. Please try again.');
  }
  return { reply, shoppingItems };
}

module.exports = { getChatReply, buildMessages, readShoppingItems, SHOPPING_TOOL };