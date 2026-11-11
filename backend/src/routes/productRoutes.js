const express = require('express');
const { recipeLimiter } = require('../middleware/rateLimiter');
const { validateBarcodeParam } = require('../middleware/validate');
const { barcode } = require('../controllers/productController');

const router = express.Router();

// GET /api/v1/products/barcode/:code   (USDA FoodData Central; shares the recipe limiter)
router.get('/barcode/:code', recipeLimiter, validateBarcodeParam, barcode);

module.exports = router;
