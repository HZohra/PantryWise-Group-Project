//Defines API endpoints that check if the backend server is running properly.

const express = require('express');
const router = express.Router();

// GET /api/v1/health -> confirms the server is running
router.get('/', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: 'pantrywise-backend',
    timestamp: new Date().toISOString(),
  });
});

module.exports = router;