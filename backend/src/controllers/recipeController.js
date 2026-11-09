const { searchRecipes } = require('../services/edamamService');

/**
 * GET /api/v1/recipes/search
 * Query already validated → req.validatedQuery = { query, diet, health, cuisineType, mealType, maxTimeMinutes, limit }
 * Response: 200 { "count": n, "recipes": [ ... ] }
 */
async function search(req, res) {
  const recipes = await searchRecipes(req.validatedQuery);
  res.status(200).json({ count: recipes.length, recipes });
}

module.exports = { search };