const express = require('express');
const { validateBody, checkChatRequest } = require('../middleware/validate');
const { chat } = require('../controllers/chatController');

const router = express.Router();

// Validation runs first; the controller only ever sees clean data
router.post('/', validateBody(checkChatRequest), chat);

module.exports = router;