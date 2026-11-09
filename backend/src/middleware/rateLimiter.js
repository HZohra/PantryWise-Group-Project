const { rateLimit } = require('express-rate-limit');
const AppError = require('../utils/AppError');
const { config } = require('../config/env');

// Hand "too many requests" to our errorHandler so it uses the same JSON shape as every other error
function tooMany(message) {
  return (req, res, next) => next(new AppError(429, 'RATE_LIMITED', message));
}

/** General limit for every /api route. */
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,              // 15 minutes
  limit: config.rateLimit.apiPer15Min,
  standardHeaders: true,                 // send RateLimit-* headers so clients can see their quota
  legacyHeaders: false,                  // skip the old X-RateLimit-* headers
  handler: tooMany('Too many requests. Please slow down and try again shortly.'),
});

/** Tighter limit for endpoints that call Claude (they cost money). */
const aiLimiter = rateLimit({
  windowMs: 60 * 1000,                   // 1 minute
  limit: config.rateLimit.aiPerMinute,
  standardHeaders: true,
  legacyHeaders: false,
  handler: tooMany('You are sending messages too quickly. Please wait a minute and try again.'),
});

/** Limit for endpoints that call Edamam (it has its own quota). */
const recipeLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: config.rateLimit.recipesPerMinute,
  standardHeaders: true,
  legacyHeaders: false,
  handler: tooMany('Too many recipe searches. Please wait a minute and try again.'),
});

module.exports = { apiLimiter, aiLimiter, recipeLimiter };
