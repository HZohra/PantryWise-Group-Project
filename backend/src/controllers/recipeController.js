const { searchRecipes } = require('../services/edamamService');
const { recommendRecipes } = require('../services/recipeService');
const { suggestSubstitutes } = require('../services/substituteService');

/** GET /api/v1/recipes/search → 200 { count, recipes } */
async function search(req, res) {
  const recipes = await searchRecipes(req.validatedQuery);
  res.status(200).json({ count: recipes.length, recipes });
}

/** POST /api/v1/recipes/recommend → 200 { searchedFor, count, recipes } */
async function recommend(req, res) {
  const { searchedFor, recipes } = await recommendRecipes(req.body);
  res.status(200).json({ searchedFor, count: recipes.length, recipes });
}

/** POST /api/v1/recipes/substitute → 200 { ingredient, substitutes, tip } */
async function substitute(req, res) {
  const result = await suggestSubstitutes(req.body);
  res.status(200).json(result);
}

module.exports = { search, recommend, substitute };