const { searchRecipes } = require('./edamamService');
const { rankRecipes, DEFAULT_WEIGHTS } = require('./recommendationService');
const { sortByUrgency } = require('../utils/prompts');

const MAX_SEARCHES = 2;        // at most 2 Edamam calls per recommendation (protects the quota)
const RESULTS_PER_SEARCH = 20; // Edamam's page size: a bigger pool gives ranking more to choose from

/**
 * What to search Edamam for: the user's query, or the most urgent items that aren't expired.
 * @returns {string[]}
 */
function chooseSearchTerms(inventory, query) {
  if (query) return [query];
  const usable = inventory.filter((i) => i.daysUntilExpiry === null || i.daysUntilExpiry >= 0);
  const names = sortByUrgency(usable).map((i) => i.name);
  return [...new Set(names)].slice(0, MAX_SEARCHES);
}

/**
 * Runs the searches in parallel and merges them without duplicates.
 * If one search fails but another works, we still use what we got.
 * @throws {AppError} only if EVERY search failed
 */
async function searchAll(terms, preferences) {
  const results = await Promise.allSettled(
    terms.map((term) => searchRecipes({ ...preferences, query: term, limit: RESULTS_PER_SEARCH })),
  );

  const succeeded = results.filter((r) => r.status === 'fulfilled');
  if (succeeded.length === 0) {
    throw results[0].reason; // already an AppError from edamamService
  }

  const byId = new Map();
  for (const result of succeeded) {
    for (const recipe of result.value) {
      if (!byId.has(recipe.recipeId)) byId.set(recipe.recipeId, recipe);
    }
  }
  return [...byId.values()];
}

/**
 * Full recommendation pipeline: choose terms → search Edamam → merge → rank → top N.
 * @param {{inventory: object[], query?: string, preferences: object, limit: number}} input  validated
 * @param {{coverage: number, expiring: number}} [weights]  Zero-Waste Mode (4.8) passes different weights
 * @returns {Promise<{searchedFor: string[], recipes: object[]}>}
 */
async function recommendRecipes({ inventory, query, preferences, limit }, weights = DEFAULT_WEIGHTS) {
  const searchedFor = chooseSearchTerms(inventory, query);
  const candidates = await searchAll(searchedFor, preferences);
  const recipes = rankRecipes(candidates, inventory, weights).slice(0, limit);
  return { searchedFor, recipes };
}

module.exports = { recommendRecipes, chooseSearchTerms };