const AppError = require('../utils/AppError');

function sendError(res, statusCode, code, message) {
  res.status(statusCode).json({ error: { code, message } });
}

// Express recognizes error middleware by its FOUR parameters, so keep `next` even though it's unused
function errorHandler(err, req, res, next) {
  if (res.headersSent) {
    return next(err); // a response already started; let Express close the connection
  }

  // 1. Errors we created on purpose: safe to show as-is
  if (err instanceof AppError) {
    return sendError(res, err.statusCode, err.code, err.message);
  }

  // 2. Errors thrown by express.json() when the body is bad
  if (err.type === 'entity.parse.failed') {
    return sendError(res, 400, 'INVALID_JSON', 'Request body is not valid JSON');
  }
  if (err.type === 'entity.too.large') {
    return sendError(res, 413, 'PAYLOAD_TOO_LARGE', 'Request body is too large');
  }

  // 3. Anything else is a bug: log the details on the server, hide them from the client
  console.error('[unhandled error]', err);
  return sendError(res, 500, 'INTERNAL_ERROR', 'Something went wrong');
}

module.exports = errorHandler;