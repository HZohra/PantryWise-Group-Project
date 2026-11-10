const { generateAiRecipe } = require('../services/aiRecipeService');

/**
 * POST /api/v1/ai/recipe
 *
 * By the time this runs, the request has been rate-limited (aiLimiter) and validated
 * (checkAiRecipeRequest), so req.body is { inventory, preferences, locale, servings }.
 * Errors are AppErrors; Express 5 forwards them to errorHandler.
 *
 * Response: 200 { recipe: { recipeId, title, servings, totalTimeMinutes, ingredients[], ingredientLines[],
 *                 steps[], tip }, usedExpiring, expiringItems[] }
 */
async function createRecipe(req, res) {
  const result = await generateAiRecipe(req.body);
  res.status(200).json(result);
}

module.exports = { createRecipe };