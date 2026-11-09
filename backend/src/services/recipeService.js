const { searchRecipes } = require('./edamamService');
const { rankRecipes, WEIGHTS_BY_MODE } = require('./recommendationService');
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
 * In zeroWaste mode, recipes that use no expiring item are dropped (unless that would leave nothing).
 * @param {{inventory, query?, preferences, limit, mode}} input  validated
 * @returns {Promise<{mode: string, searchedFor: string[], recipes: object[]}>}
 */
async function recommendRecipes({ inventory, query, preferences, limit, mode }) {
  const searchedFor = chooseSearchTerms(inventory, query);
  const candidates = await searchAll(searchedFor, preferences);

  let ranked = rankRecipes(candidates, inventory, WEIGHTS_BY_MODE[mode]);

  if (mode === 'zeroWaste') {
    const rescuing = ranked.filter((r) => r.match.expiringUsed.length > 0);
    if (rescuing.length > 0) ranked = rescuing; // fallback: never return an empty list just because of the filter
  }

  return { mode, searchedFor, recipes: ranked.slice(0, limit) };
}

module.exports = { recommendRecipes, chooseSearchTerms };