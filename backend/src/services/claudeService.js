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

const emptyReply = () => new AppError(502, 'AI_EMPTY_REPLY', 'The assistant returned an empty reply. Please try again.');

/** One call to Claude; SDK errors become AppErrors. */
async function createMessage(params) {
  try {
    return await getClient().messages.create(params);
  } catch (err) {
    throw mapClaudeError(err);
  }
}

/** Diagnostics: tell us when a reply was cut off, and why. */
function warnIfCutOff(response, maxTokens) {
  if (response.stop_reason === 'max_tokens') {
    console.warn(
      '[claude] reply cut off by max_tokens =', maxTokens,
      '| output tokens =', response.usage?.output_tokens,
      '| blocks =', response.content.map((b) => b.type).join(','),
    );
  }
}

/** A reply is a list of content blocks; this joins the text ones. */
function textOf(response) {
  return response.content
    .filter((block) => block.type === 'text')
    .map((block) => block.text)
    .join('')
    .trim();
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
  const response = await createMessage({ model: config.anthropic.model, max_tokens: maxTokens, system, messages });
  warnIfCutOff(response, maxTokens);
  const text = textOf(response);
  if (!text) throw emptyReply();
  return text;
}

/**
 * Like generateReply, but Claude may also call the given tools. The calls are returned to the
 * caller (they are NOT executed here: the app shows them and the user confirms).
 * @param {{system: string, messages: object[], maxTokens?: number, tools: object[]}} params
 * @returns {Promise<{text: string, toolCalls: {name: string, input: object}[]}>}
 * @throws {AppError} AI_EMPTY_REPLY when there is neither text nor a tool call
 */
async function generateReplyWithTools({ system, messages, maxTokens = 600, tools = [] }) {
  const response = await createMessage({
    model: config.anthropic.model, max_tokens: maxTokens, system, messages, tools,
  });
  warnIfCutOff(response, maxTokens);
  const text = textOf(response);
  const toolCalls = response.content
    .filter((block) => block.type === 'tool_use')
    .map((block) => ({ name: block.name, input: block.input || {} }));
  if (!text && toolCalls.length === 0) throw emptyReply();
  return { text, toolCalls };
}

module.exports = { generateReply, generateReplyWithTools, mapClaudeError };