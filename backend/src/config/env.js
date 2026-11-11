const path = require('path');

// Load backend/.env no matter which folder the command is run from
require('dotenv').config({ path: path.join(__dirname, '..', '..', '.env'), quiet: true });

const REQUIRED_VARS = ['ANTHROPIC_API_KEY', 'EDAMAM_APP_ID', 'EDAMAM_APP_KEY'];

// The only place in the backend that reads process.env
const config = Object.freeze({
  port: Number(process.env.PORT) || 3000,
  anthropic: {
    apiKey: process.env.ANTHROPIC_API_KEY,
    model: process.env.ANTHROPIC_MODEL || 'claude-haiku-5-5',
    timeoutMs: Number(process.env.ANTHROPIC_TIMEOUT_MS) || 20000,
  },
  edamam: {
    appId: process.env.EDAMAM_APP_ID,
    appKey: process.env.EDAMAM_APP_KEY,
    timeoutMs: Number(process.env.EDAMAM_TIMEOUT_MS) || 10000, // NEW
  },

   rateLimit: {
     apiPer15Min: Number(process.env.API_RATE_LIMIT_PER_15_MIN) || 100,
     aiPerMinute: Number(process.env.AI_RATE_LIMIT_PER_MINUTE) || 10,
     recipesPerMinute: Number(process.env.RECIPES_RATE_LIMIT_PER_MINUTE) || 20,
   },
});

/** @returns {string[]} names of required variables that are missing or blank */
function findMissingEnv() {
  return REQUIRED_VARS.filter((name) => !process.env[name] || !process.env[name].trim());
}

module.exports = { config, findMissingEnv };