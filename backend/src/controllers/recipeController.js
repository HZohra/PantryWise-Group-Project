const { searchRecipes } = require('../services/edamamService');
const { recommendRecipes } = require('../services/recipeService');

/**
 * GET /api/v1/recipes/search
 * Response: 200 { "count": n, "recipes": [ ... ] }
 */
async function search(req, res) {
  const recipes = await searchRecipes(req.validatedQuery);
  res.status(200).json({ count: recipes.length, recipes });
}

/**
 * POST /api/v1/recipes/recommend
 * Response: 200 { "searchedFor": [...], "count": n, "recipes": [ { ...recipe, match } ] }
 */
async function recommend(req, res) {
  const { searchedFor, recipes } = await recommendRecipes(req.body);
  res.status(200).json({ searchedFor, count: recipes.length, recipes });
}

module.exports = { search, recommend };