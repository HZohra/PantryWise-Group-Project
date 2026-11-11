const AppError = require('../utils/AppError');

// Runs only if no route matched the request
function notFound(req, res, next) {
  next(new AppError(404, 'NOT_FOUND', `Route ${req.method} ${req.path} not found`));
}

module.exports = notFound;