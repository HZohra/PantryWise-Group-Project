const Anthropic = require('@anthropic-ai/sdk').default ?? require('@anthropic-ai/sdk').Anthropic;
const { config } = require('../config/env');

let client = null;

// Create the client on first use, not when the file is loaded.
// Tests can then import the app without a real API key.
function getClient() {
  if (!client) {
    client = new Anthropic({
      apiKey: config.anthropic.apiKey,
      timeout: config.anthropic.timeoutMs,
      maxRetries: 1, // retry once on network blips, then fail fast
    });
  }
  return client;
}

/**
 * Sends a conversation to Claude and returns its text reply.
 * @param {object} params
 * @param {string} params.system  instructions that shape Claude's behaviour
 * @param {{role: 'user'|'assistant', content: string}[]} params.messages  conversation so far, ending with the user
 * @param {number} [params.maxTokens]  upper limit on reply length (and cost)
 * @returns {Promise<string>}
 */
async function generateReply({ system, messages, maxTokens = 600 }) {
  const response = await getClient().messages.create({
    model: config.anthropic.model,
    max_tokens: maxTokens,
    system,
    messages,
  });

  // A reply is a list of content blocks; we only want the text ones
  return response.content
    .filter((block) => block.type === 'text')
    .map((block) => block.text)
    .join('')
    .trim();
}

module.exports = { generateReply };