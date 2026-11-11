/**
 * Shared test helpers: placeholder settings, a fake Edamam (fetch) and a fake Claude.
 * Nothing here calls the network, so the tests need no API keys and use no quota.
 */

// Placeholder settings so config/env.js loads without a real .env. Real values are never used:
// fetch and Claude are replaced below, so no request leaves this machine.
const PLACEHOLDERS = {
  PORT: '3000',
  ANTHROPIC_MODEL: 'claude-haiku-5-5',
  ANTHROPIC_API_KEY: 'test-anthropic-key',
  EDAMAM_APP_ID: 'test-app-id',
  EDAMAM_APP_KEY: 'test-app-key',
};
for (const [name, value] of Object.entries(PLACEHOLDERS)) {
  if (!process.env[name]) process.env[name] = value;
}

// The services log on purpose; keep test output readable. Set SHOW_TEST_LOGS=1 to see the logs.
if (!process.env.SHOW_TEST_LOGS) {
  console.log = () => {};
  console.warn = () => {};
  console.error = () => {};
}

const AppError = require('../src/utils/AppError');

// =====================================================================
// Fake Edamam
// =====================================================================

/** An Edamam hit as the real API returns it (only the fields we ask for). */
function rawRecipe({
  id, label = `Recipe ${id}`, source = 'Test Kitchen', ingredients = [],
  servings = 2, totalTime = 20, calories = 800,
}) {
  return {
    uri: `http://www.edamam.com/ontologies/edamam.owl#recipe_${id}`,
    label,
    image: `https://img.example/${id}.jpg`,
    source,
    url: `https://example.com/${id}`,
    yield: servings,
    totalTime,
    calories,
    totalNutrients: { PROCNT: { quantity: 40 }, CHOCDF: { quantity: 60 }, FAT: { quantity: 20 } },
    ingredientLines: ingredients.map((name) => `1 ${name}`),
    ingredients: ingredients.map((food) => ({ food })),
  };
}

/** A successful Edamam response with these raw recipes. */
const okResponse = (recipes) => ({
  ok: true,
  status: 200,
  json: async () => ({ hits: recipes.map((recipe) => ({ recipe })) }),
});

/** An Edamam error response (e.g. 401, 429). */
const errorResponse = (status) => ({ ok: false, status, json: async () => ({}) });

/**
 * Replaces the global fetch. `handler(query, callNumber)` returns a response or throws.
 * @returns {string[]} the search words of every call (never the URL: it contains the key)
 */
function installFakeFetch(handler) {
  const queries = [];
  globalThis.fetch = async (url) => {
    const query = new URL(String(url)).searchParams.get('q');
    queries.push(query);
    return handler(query, queries.length);
  };
  return queries;
}

// =====================================================================
// Fake Claude
// =====================================================================

/**
 * Puts a fake claudeService in Node's module cache. MUST be called before requiring any service
 * that uses Claude (they keep a reference to generateReply when they load).
 * Queue replies with fake.queue('text', new AppError(...)); each call takes the next one.
 */
function installFakeClaude() {
  const file = require.resolve('../src/services/claudeService');
  const fake = {
    replies: [],
    calls: [],
    reset() {
      this.replies = [];
      this.calls = [];
    },
    queue(...replies) {
      this.replies.push(...replies);
    },
  };
  async function generateReply(params) {
    fake.calls.push(params);
    if (fake.replies.length === 0) throw new Error('fake Claude: no reply queued for this call');
    const next = fake.replies.shift();
    if (next instanceof Error) throw next;
    return next;
  }
  require.cache[file] = {
    id: file,
    filename: file,
    loaded: true,
    exports: { generateReply, mapClaudeError: () => new AppError(503, 'AI_UNAVAILABLE', 'fake') },
  };
  return fake;
}

/** An inventory item as validate.js produces it. */
const item = (name, daysUntilExpiry = null) => ({ name, quantity: null, unit: '', daysUntilExpiry });

/** Empty preferences as validate.js produces them. */
const noPreferences = () => ({ health: [], allergies: [] });

module.exports = {
  rawRecipe, okResponse, errorResponse, installFakeFetch, installFakeClaude, item, noPreferences,
};