const express = require('express');
const { recipeLimiter } = require('../middleware/rateLimiter');
const {
  validateQuery, validateBody, checkRecipeSearchQuery, checkRecommendRequest,
} = require('../middleware/validate');
const { search, recommend } = require('../controllers/recipeController');

const router = express.Router();

// GET /api/v1/recipes/search
router.get('/search', recipeLimiter, validateQuery(checkRecipeSearchQuery), search);

// POST /api/v1/recipes/recommend
router.post('/recommend', recipeLimiter, validateBody(checkRecommendRequest), recommend);

// /substitute (4.7) will be added here

module.exports = router;