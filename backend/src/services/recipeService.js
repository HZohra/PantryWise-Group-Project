const { searchRecipes } = require('./edamamService');
const { rankRecipes, WEIGHTS_BY_MODE, normalizeName, namesMatch } = require('./recommendationService');
const { sortByUrgency } = require('../utils/prompts');

const MAX_SEARCHES = 2;        // at most 2 Edamam calls per attempt (protects the quota)
const RESULTS_PER_SEARCH = 20; // Edamam's page size: a bigger pool gives ranking more to choose from

/** Used when there is nothing to personalize (empty pantry). Dishes that exist for almost every diet. */
const DEFAULT_SEARCHES = Object.freeze(['pasta', 'soup']);

/** Filters that may be dropped to broaden a search. Diet and health labels never are: they are needs, not tastes. */
const SOFT_FILTERS = Object.freeze(['cuisineType', 'mealType', 'maxTimeMinutes']);

/** Inventory names that aren't expired, most urgent first, without duplicates. */
function urgentNames(inventory) {
  const usable = inventory.filter((i) => i.daysUntilExpiry === null || i.daysUntilExpiry >= 0);
  return [...new Set(sortByUrgency(usable).map((i) => i.name))];
}

/**
 * What to search Edamam for: the user's query, or the most urgent items that aren't expired.
 * Returns [] when there is nothing to personalize (no query, no usable items).
 * @returns {string[]}
 */
function chooseSearchTerms(inventory, query) {
  if (query) return [query];
  return urgentNames(inventory).slice(0, MAX_SEARCHES);
}

/** A copy of the preferences without the soft filters. */
function withoutSoftFilters(preferences) {
  const relaxed = { ...preferences };
  for (const key of SOFT_FILTERS) delete relaxed[key];
  return relaxed;
}

/**
 * One wider search to try when the first one found nothing (UC 4c):
 *  - a multi-word query loses its last word ("chicken tikka masala" → "chicken tikka");
 *  - an inventory search moves on to the next most urgent items;
 *  - soft filters (cuisine, meal type, max time) are dropped in every case.
 * @returns {{terms: string[], preferences: object} | null}  null when broadening would change nothing
 */
function broadenSearch({ inventory, query, preferences, terms }) {
  const relaxed = withoutSoftFilters(preferences);
  const droppedFilters = SOFT_FILTERS.some((key) => preferences[key] !== undefined);

  let widerTerms = terms;
  if (query) {
    const words = query.split(/\s+/);
    if (words.length > 1) widerTerms = [words.slice(0, -1).join(' ')];
  } else {
    const next = urgentNames(inventory).filter((name) => !terms.includes(name)).slice(0, MAX_SEARCHES);
    if (next.length > 0) widerTerms = next;
  }

  const sameTerms = widerTerms.length === terms.length && widerTerms.every((t, i) => t === terms[i]);
  if (sameTerms && !droppedFilters) return null;
  return { terms: widerTerms, preferences: relaxed };
}

/**
 * Drops recipes that use an ingredient the user is allergic to.
 * Same whole-word matching as the pantry, so "peanut" also catches "peanut butter".
 */
function removeRecipesWithAllergens(recipes, allergies = []) {
  const blocked = allergies.map(normalizeName).filter(Boolean);
  if (blocked.length === 0) return recipes;
  return recipes.filter((recipe) => !(recipe.ingredientNames || []).some((ingredient) => {
    const key = normalizeName(ingredient);
    return key !== '' && blocked.some((allergy) => namesMatch(key, allergy));
  }));
}

/** "Grilled Butterflied Chicken Recipe" + "Serious Eats" → one key, so Edamam's near-copies collapse. */
function sameDishKey(recipe) {
  return `${normalizeName(recipe.title)}|${String(recipe.sourceName || '').trim().toLowerCase()}`;
}

/**
 * Runs the searches in parallel and merges them without duplicates:
 * the same id, or the same title from the same source (Edamam sometimes lists a dish twice).
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

  const seenIds = new Set();
  const seenDishes = new Set();
  const merged = [];
  for (const result of succeeded) {
    for (const recipe of result.value) {
      const dish = sameDishKey(recipe);
      if (seenIds.has(recipe.recipeId) || seenDishes.has(dish)) continue;
      seenIds.add(recipe.recipeId);
      seenDishes.add(dish);
      merged.push(recipe);
    }
  }
  return merged;
}

/** Searches, then removes recipes with the user's allergens. */
async function searchSafe(terms, preferences) {
  return removeRecipesWithAllergens(await searchAll(terms, preferences), preferences.allergies);
}

/**
 * Full recommendation pipeline:
 * choose terms (or defaults) → search → remove allergens → broaden once if empty → rank → top N.
 * In zeroWaste mode, recipes that use no expiring item are dropped (unless that would leave nothing).
 * @param {{inventory, query?, preferences, limit, mode}} input  validated
 * @returns {Promise<{mode: string, personalized: boolean, broadened: boolean,
 *                    searchedFor: string[], recipes: object[]}>}
 */
async function recommendRecipes({ inventory, query, preferences, limit, mode }) {
  let searchedFor = chooseSearchTerms(inventory, query);
  const personalized = searchedFor.length > 0;
  if (!personalized) searchedFor = [...DEFAULT_SEARCHES]; // UC 2a: empty pantry still gets ideas

  let candidates = await searchSafe(searchedFor, preferences);

  let broadened = false;
  if (candidates.length === 0) {
    const wider = broadenSearch({ inventory, query, preferences, terms: searchedFor });
    if (wider) {
      candidates = await searchSafe(wider.terms, wider.preferences);
      searchedFor = wider.terms;
      broadened = true;
    }
  }

  let ranked = rankRecipes(candidates, inventory, WEIGHTS_BY_MODE[mode]);

  if (mode === 'zeroWaste') {
    const rescuing = ranked.filter((r) => r.match.expiringUsed.length > 0);
    if (rescuing.length > 0) ranked = rescuing; // fallback: never return an empty list just because of the filter
  }

  return { mode, personalized, broadened, searchedFor, recipes: ranked.slice(0, limit) };
}

module.exports = {
  recommendRecipes,
  chooseSearchTerms,
  broadenSearch,
  removeRecipesWithAllergens,
  DEFAULT_SEARCHES,
};