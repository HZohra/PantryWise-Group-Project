const Anthropic = require('@anthropic-ai/sdk').default ?? require('@anthropic-ai/sdk').Anthropic;
const { config } = require('../config/env');
const AppError = require('../utils/AppError');

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
 * Turns any error from the Claude SDK into an AppError that is safe to send to the app.
 * The real details are logged here, on the server only.
 * @param {Error} err  error thrown by the SDK
 * @returns {AppError}
 */
function mapClaudeError(err) {
  console.error('[claude] request failed:', err.status ?? '-', err.name, '-', err.message);

  if (err.name === 'APIConnectionTimeoutError') {
    return new AppError(504, 'AI_TIMEOUT', 'The assistant took too long to respond. Please try again.');
  }
  if (err.name === 'APIConnectionError') {
    return new AppError(503, 'AI_UNAVAILABLE', 'The assistant is unavailable right now. Please try again later.');
  }

  const status = err.status;
  if (status === 429) {
    return new AppError(429, 'AI_RATE_LIMITED', 'The assistant is busy. Please wait a moment and try again.');
  }
  if (status === 401 || status === 403 || status === 404) {
    return new AppError(502, 'AI_CONFIG_ERROR', 'The assistant is not configured correctly.');
  }
  if (status === 400) {
    return new AppError(502, 'AI_REQUEST_FAILED', 'The assistant could not process this request.');
  }
  return new AppError(503, 'AI_UNAVAILABLE', 'The assistant is unavailable right now. Please try again later.');
}

/**
 * Sends a conversation to Claude and returns its text reply.
 * @param {object} params
 * @param {string} params.system  instructions that shape Claude's behaviour
 * @param {{role: 'user'|'assistant', content: string}[]} params.messages  conversation so far, ending with the user
 * @param {number} [params.maxTokens]  upper limit on reply length (and cost)
 * @returns {Promise<string>}
 * @throws {AppError} always an AppError, never a raw SDK error
 */
async function generateReply({ system, messages, maxTokens = 600 }) {
  let response;
  try {
    response = await getClient().messages.create({
      model: config.anthropic.model,
      max_tokens: maxTokens,
      system,
      messages,
    });
  } catch (err) {
    throw mapClaudeError(err);
  }

  // A reply is a list of content blocks; we only want the text ones
  const text = response.content
    .filter((block) => block.type === 'text')
    .map((block) => block.text)
    .join('')
    .trim();

  if (!text) {
    throw new AppError(502, 'AI_EMPTY_REPLY', 'The assistant returned an empty reply. Please try again.');
  }
  return text;
}

module.exports = { generateReply, mapClaudeError };