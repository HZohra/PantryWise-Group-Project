const express = require('express');
const { recipeLimiter } = require('../middleware/rateLimiter');
const { validateQuery, checkRecipeSearchQuery } = require('../middleware/validate');
const { search } = require('../controllers/recipeController');

const router = express.Router();

// GET /api/v1/recipes/search
router.get('/search', recipeLimiter, validateQuery(checkRecipeSearchQuery), search);

// /recommend (4.6) and /substitute (4.7) will be added here

module.exports = router;