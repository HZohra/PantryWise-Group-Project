const express = require('express');
const { recipeLimiter, aiLimiter } = require('../middleware/rateLimiter');
const {
  validateQuery, validateBody, checkRecipeSearchQuery, checkRecommendRequest, checkSubstituteRequest,
} = require('../middleware/validate');
const { search, recommend, substitute } = require('../controllers/recipeController');

const router = express.Router();

// GET /api/v1/recipes/search      (Edamam)
router.get('/search', recipeLimiter, validateQuery(checkRecipeSearchQuery), search);

// POST /api/v1/recipes/recommend  (Edamam)
router.post('/recommend', recipeLimiter, validateBody(checkRecommendRequest), recommend);

// POST /api/v1/recipes/substitute (Claude → uses the AI limiter)
router.post('/substitute', aiLimiter, validateBody(checkSubstituteRequest), substitute);

module.exports = router;