const { EXPIRING_SOON_DAYS } = require('../utils/prompts');

/** Weights for the score. Zero-Waste Mode (4.8) will use different ones. */
const DEFAULT_WEIGHTS = Object.freeze({ coverage: 100, expiring: 10 });

/** Zero-Waste Mode: rescuing expiring food matters more than having every ingredient. */
const ZERO_WASTE_WEIGHTS = Object.freeze({ coverage: 40, expiring: 30 });

/** Which weights each mode uses. */
const WEIGHTS_BY_MODE = Object.freeze({ standard: DEFAULT_WEIGHTS, zeroWaste: ZERO_WASTE_WEIGHTS });

/** Assumed to be in every kitchen: never counted as needed or missing. */
const STAPLES = new Set(['water', 'salt', 'pepper', 'black pepper', 'sea salt', 'kosher salt', 'ice']);

/** "eggs" → "egg", "tomatoes" → "tomato", "berries" → "berry". Simple rules, not a dictionary. */
function singularize(word) {
  if (word.length <= 3 || /(ss|us|is)$/.test(word)) return word; // "glass", "hummus", "kiss"... keep
  if (word.endsWith('ies')) return `${word.slice(0, -3)}y`;
  if (/(sses|ches|shes|xes|oes)$/.test(word)) return word.slice(0, -2);
  if (word.endsWith('s')) return word.slice(0, -1);
  return word;
}

/** "2% Whole Milk!" → "whole milk", "Eggs" → "egg" */
function normalizeName(name) {
  return String(name || '')
    .toLowerCase()
    .replace(/[^a-z\s]/g, ' ') // drop digits, %, punctuation
    .split(/\s+/)
    .filter(Boolean)
    .map(singularize)
    .join(' ');
}

/** True if every word of one name appears in the other (both already normalized). */
function namesMatch(a, b) {
  if (a === b) return true;
  const wordsA = a.split(' ');
  const wordsB = b.split(' ');
  const containsAll = (big, small) => small.every((w) => big.includes(w));
  return containsAll(wordsA, wordsB) || containsAll(wordsB, wordsA);
}

/** Normalizes inventory once and removes expired items (never recommend cooking expired food). */
function preparePantry(inventory) {
  return inventory
    .filter((item) => item.daysUntilExpiry === null || item.daysUntilExpiry >= 0)
    .map((item) => ({
      key: normalizeName(item.name),
      name: item.name,
      daysUntilExpiry: item.daysUntilExpiry,
    }))
    .filter((item) => item.key);
}

/** Points for using an expiring item: today = 4, tomorrow = 3, 2 days = 2, 3 days = 1. */
function urgencyPoints(daysUntilExpiry) {
  return EXPIRING_SOON_DAYS + 1 - daysUntilExpiry;
}

/**
 * Scores one recipe against a prepared pantry.
 * @returns {{score:number, haveCount:number, totalCount:number, have:string[], missing:string[],
 *            expiringUsed:{name:string, daysUntilExpiry:number}[]}}
 */
function scoreRecipe(recipe, pantry, weights = DEFAULT_WEIGHTS) {
  const needed = [...new Set((recipe.ingredientNames || []).map(normalizeName))]
    .filter((name) => name && !STAPLES.has(name));

  const have = [];
  const missing = [];
  const expiring = new Map(); // pantry item name → days (a Map so each item counts once)

  for (const ingredient of needed) {
    const matches = pantry.filter((p) => namesMatch(ingredient, p.key));
    if (matches.length === 0) {
      missing.push(ingredient);
      continue;
    }
    have.push(ingredient);
    for (const item of matches) {
      if (item.daysUntilExpiry !== null && item.daysUntilExpiry <= EXPIRING_SOON_DAYS) {
        expiring.set(item.name, item.daysUntilExpiry);
      }
    }
  }

  const coverage = needed.length > 0 ? have.length / needed.length : 0;
  let urgency = 0;
  for (const days of expiring.values()) urgency += urgencyPoints(days);

  return {
    score: Math.round(coverage * weights.coverage + urgency * weights.expiring),
    haveCount: have.length,
    totalCount: needed.length,
    have,
    missing,
    expiringUsed: [...expiring]
      .map(([name, daysUntilExpiry]) => ({ name, daysUntilExpiry }))
      .sort((a, b) => a.daysUntilExpiry - b.daysUntilExpiry),
  };
}

/**
 * Ranks recipes for this inventory. Does not modify the inputs.
 * @param {object[]} recipes   in the shape returned by edamamService
 * @param {object[]} inventory validated items { name, quantity, unit, daysUntilExpiry }
 * @param {{coverage:number, expiring:number}} [weights]
 * @returns {object[]} each recipe plus a `match` object, best first
 */
function rankRecipes(recipes, inventory, weights = DEFAULT_WEIGHTS) {
  const pantry = preparePantry(inventory);
  const time = (r) => r.totalTimeMinutes ?? Number.MAX_SAFE_INTEGER; // unknown time sorts last

  return recipes
    .map((recipe) => ({ ...recipe, match: scoreRecipe(recipe, pantry, weights) }))
    .sort((a, b) =>
      b.match.score - a.match.score
      || a.match.missing.length - b.match.missing.length
      || time(a) - time(b));
}

module.exports = {
  rankRecipes,
  scoreRecipe,
  preparePantry,
  normalizeName,
  namesMatch,
  DEFAULT_WEIGHTS,
  ZERO_WASTE_WEIGHTS,
  WEIGHTS_BY_MODE,
  STAPLES,
};