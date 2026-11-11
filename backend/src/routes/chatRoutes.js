const express = require('express');
const { aiLimiter } = require('../middleware/rateLimiter');
const { validateBody, checkChatRequest } = require('../middleware/validate');
const { chat } = require('../controllers/chatController');

const router = express.Router();

// Order: rate limit → validate → controller
router.post('/', aiLimiter, validateBody(checkChatRequest), chat);

module.exports = router;