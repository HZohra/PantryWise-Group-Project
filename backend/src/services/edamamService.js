const { config } = require('../config/env');
const AppError = require('../utils/AppError');

const BASE_URL = 'https://api.edamam.com/api/recipes/v2';

// Only ask Edamam for the fields we use: smaller, faster responses
const FIELDS = [
  'uri', 'label', 'image', 'source', 'url', 'yield', 'totalTime', 'calories',
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

/** Converts one Edamam recipe into our own shape. */
function normalizeRecipe(raw) {
  const id = (raw.uri || '').split('#recipe_')[1] || '';
  return {
    recipeId: `edamam:${id}`,
    title: raw.label || 'Untitled recipe',
    image: raw.image || null,
    sourceName: raw.source || null,
    sourceUrl: raw.url || null,
    servings: raw.yield > 0 ? raw.yield : null,
    totalTimeMinutes: raw.totalTime > 0 ? raw.totalTime : null, // Edamam uses 0 for "unknown"
    calories: raw.calories ? Math.round(raw.calories) : null,
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
 * @param {object} options  { query, diet, health[], cuisineType, mealType, maxTimeMinutes, limit }
 * @returns {Promise<object[]>}
 * @throws {AppError}
 */
async function searchRecipes(options) {
  const { limit = 10 } = options;
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
    .slice(0, limit);
}

module.exports = { searchRecipes, normalizeRecipe, mapEdamamError };