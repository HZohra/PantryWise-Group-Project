const { config } = require('../config/env');
const AppError = require('../utils/AppError');

const BASE_URL = 'https://api.edamam.com/api/recipes/v2';

// Only ask Edamam for the fields we use: smaller, faster responses
const FIELDS = [
  'uri', 'label', 'image', 'source', 'url', 'yield', 'totalTime', 'calories', 'totalNutrients',
  'ingredientLines', 'ingredients', 'dietLabels', 'healthLabels', 'cuisineType',
];

/** Builds the request URL. Contains the API key: NEVER log the result. */
function buildUrl({ query, diet, health = [], cuisineType, mealType, maxTimeMinutes }) {
  const params = new URLSearchParams({
    type: 'public',
    q: query,
    app_id: config.edamam.appId,
    app_key: config.edamam.appKey,
  });
  if (diet) params.append('diet', diet);
  health.forEach((h) => params.append('health', h)); // can repeat: vegan AND gluten-free
  if (cuisineType) params.append('cuisineType', cuisineType);
  if (mealType) params.append('mealType', mealType);
  if (maxTimeMinutes) params.append('time', `1-${maxTimeMinutes}`);
  FIELDS.forEach((f) => params.append('field', f));
  return `${BASE_URL}?${params.toString()}`;
}

/** Grams of one nutrient for the whole recipe (Edamam codes: PROCNT, CHOCDF, FAT), or null. */
function nutrientTotal(totalNutrients, code) {
  const quantity = totalNutrients && totalNutrients[code] ? totalNutrients[code].quantity : null;
  return Number.isFinite(quantity) && quantity >= 0 ? quantity : null;
}

/**
 * Calories, protein, carbs and fat PER SERVING, rounded.
 * Edamam gives totals for the whole dish; without a serving count we keep the totals
 * and say so with perServing: false.
 */
function buildNutrition(raw, servings) {
  const divideBy = servings || 1;
  const per = (value) => (value === null ? null : Math.round(value / divideBy));
  const calories = Number.isFinite(raw.calories) && raw.calories > 0 ? raw.calories : null;
  const nutrients = raw.totalNutrients || {};
  return {
    perServing: servings !== null,
    calories: per(calories),
    protein: per(nutrientTotal(nutrients, 'PROCNT')),
    carbs: per(nutrientTotal(nutrients, 'CHOCDF')),
    fat: per(nutrientTotal(nutrients, 'FAT')),
  };
}

/** Converts one Edamam recipe into our own shape. */
function normalizeRecipe(raw) {
  const id = (raw.uri || '').split('#recipe_')[1] || '';
  const servings = raw.yield > 0 ? raw.yield : null;
  return {
    recipeId: `edamam:${id}`,
    title: raw.label || 'Untitled recipe',
    image: raw.image || null,
    sourceName: raw.source || null,
    sourceUrl: raw.url || null,
    servings,
    totalTimeMinutes: raw.totalTime > 0 ? raw.totalTime : null, // Edamam uses 0 for "unknown"
    calories: raw.calories ? Math.round(raw.calories) : null,   // whole recipe (kept for compatibility)
    nutrition: buildNutrition(raw, servings),                  // per serving
    ingredientLines: raw.ingredientLines || [],
    ingredientNames: [...new Set((raw.ingredients || [])
      .map((i) => (i.food || '').toLowerCase().trim())
      .filter(Boolean))],
    dietLabels: raw.dietLabels || [],
    healthLabels: raw.healthLabels || [],
    cuisineType: raw.cuisineType || [],
  };
}

/** Converts fetch/HTTP failures into safe AppErrors. Logs status only, never the URL. */
function mapEdamamError(err, status) {
  if (err && (err.name === 'TimeoutError' || err.name === 'AbortError')) {
    console.error('[edamam] request timed out');
    return new AppError(504, 'RECIPES_TIMEOUT', 'Recipe search took too long. Please try again.');
  }
  if (err) {
    console.error('[edamam] network error:', err.name);
    return new AppError(503, 'RECIPES_UNAVAILABLE', 'Recipe search is unavailable right now. Please try again later.');
  }
  console.error('[edamam] request failed with status', status);
  if (status === 429) {
    return new AppError(429, 'RECIPES_RATE_LIMITED', 'Too many recipe searches. Please wait a moment and try again.');
  }
  if (status === 401 || status === 403) {
    return new AppError(502, 'RECIPES_CONFIG_ERROR', 'Recipe search is not configured correctly.');
  }
  if (status === 400) {
    return new AppError(502, 'RECIPES_REQUEST_FAILED', 'Recipe search could not process this request.');
  }
  return new AppError(503, 'RECIPES_UNAVAILABLE', 'Recipe search is unavailable right now. Please try again later.');
}

/**
 * Searches Edamam and returns recipes in our own format.
 * maxCalories is checked here on the per-serving value the app shows (unknown calories are left out).
 * @param {object} options  { query, diet, health[], cuisineType, mealType, maxTimeMinutes, maxCalories, limit }
 * @returns {Promise<object[]>}
 * @throws {AppError}
 */
async function searchRecipes(options) {
  const { limit = 10, maxCalories } = options;
  let response;
  try {
    response = await fetch(buildUrl(options), {
      signal: AbortSignal.timeout(config.edamam.timeoutMs),
    });
  } catch (err) {
    throw mapEdamamError(err); // network problem or timeout
  }

  if (!response.ok) {
    throw mapEdamamError(null, response.status); // Edamam answered, but with an error
  }

  const data = await response.json();
  return (data.hits || [])
    .map((hit) => normalizeRecipe(hit.recipe))
    .filter((r) => r.recipeId !== 'edamam:') // drop recipes without a usable id
    .filter((r) => !maxCalories || (r.nutrition.calories !== null && r.nutrition.calories <= maxCalories))
    .slice(0, limit);
}

module.exports = { searchRecipes, normalizeRecipe, mapEdamamError, buildNutrition };