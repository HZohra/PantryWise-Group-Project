const { generateReply } = require('./claudeService');
const { buildChatSystemPrompt } = require('../utils/prompts');

const MAX_REPLY_TOKENS = 1024; // about 350 words: enough for a short recipe, keeps cost predictable

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

/**
 * @param {{message: string, history: object[], inventory: object[], preferences: object, locale: string}} input
 *        already validated by checkChatRequest
 * @returns {Promise<string>} the assistant's reply
 * @throws {AppError} from claudeService
 */
async function getChatReply({ message, history, inventory, preferences, locale }) {
  return generateReply({
    system: buildChatSystemPrompt(inventory, preferences, locale),
    messages: buildMessages(history, message),
    maxTokens: MAX_REPLY_TOKENS,
  });
}

module.exports = { getChatReply, buildMessages };