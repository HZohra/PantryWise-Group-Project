const express = require('express');
const { aiLimiter } = require('../middleware/rateLimiter');
const { validateBody, checkAiRecipeRequest } = require('../middleware/validate');
const { createRecipe } = require('../controllers/aiRecipeController');

const router = express.Router();

// Order: rate limit → validate → controller
router.post('/', aiLimiter, validateBody(checkAiRecipeRequest), createRecipe);

module.exports = router;