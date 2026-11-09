const { generateReply } = require('./claudeService');
const { buildSubstitutePrompt } = require('../utils/prompts');
const { preparePantry, normalizeName, namesMatch } = require('./recommendationService');
const AppError = require('../utils/AppError');

const MAX_REPLY_TOKENS = 1024;
const MAX_SUBSTITUTES = 3;

/** Finds the JSON object inside a reply, even if Claude added code fences or a sentence around it. */
function extractJson(text) {
  const start = text.indexOf('{');
  const end = text.lastIndexOf('}');
  if (start === -1 || end <= start) return null;
  try {
    return JSON.parse(text.slice(start, end + 1));
  } catch {
    return null;
  }
}

/** Trimmed string capped at `max` characters; anything that isn't a string becomes "". */
const shortText = (value, max) => (typeof value === 'string' ? value.trim().slice(0, max) : '');

/**
 * Turns Claude's text into { substitutes, tip }, keeping only the fields we expect.
 * @throws {AppError} 502 AI_BAD_FORMAT if the reply isn't usable JSON
 */
function parseSubstitutes(text) {
  const data = extractJson(text);
  if (!data || !Array.isArray(data.substitutes)) {
    console.error('[substitute] reply was not the expected JSON:', text.slice(0, 200));
    throw new AppError(502, 'AI_BAD_FORMAT', 'The assistant gave an unexpected answer. Please try again.');
  }
  return {
    substitutes: data.substitutes
      .filter((s) => s && typeof s.name === 'string' && s.name.trim())
      .slice(0, MAX_SUBSTITUTES)
      .map((s) => ({
        name: shortText(s.name, 60).toLowerCase(),
        amount: shortText(s.amount, 160),
        notes: shortText(s.notes, 300),
      })),
    tip: shortText(data.tip, 200),
  };
}

/** Our code decides inPantry, not Claude: reuses the 4.5 matching rules. */
function markPantryItems(substitutes, inventory) {
  const pantry = preparePantry(inventory); // also drops expired items
  return substitutes.map((s) => {
    const key = normalizeName(s.name);
    return { ...s, inPantry: key !== '' && pantry.some((p) => namesMatch(key, p.key)) };
  });
}

/**
 * @param {{recipeTitle, ingredient, ingredientLines, inventory, preferences}} input  validated
 * @returns {Promise<{ingredient: string, substitutes: object[], tip: string}>}
 */
async function suggestSubstitutes(input) {
  const { system, userMessage } = buildSubstitutePrompt(input);
  const text = await generateReply({
    system,
    messages: [{ role: 'user', content: userMessage }],
    maxTokens: MAX_REPLY_TOKENS,
  });

  const { substitutes, tip } = parseSubstitutes(text);
  const marked = markPantryItems(substitutes, input.inventory);
  marked.sort((a, b) => Number(b.inPantry) - Number(a.inPantry)); // pantry items first; otherwise keeps Claude's order

  return { ingredient: input.ingredient, substitutes: marked, tip };
}

module.exports = { suggestSubstitutes, parseSubstitutes, extractJson };