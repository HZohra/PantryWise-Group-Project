/**
 * An error we create on purpose, so its message is safe to show the client.
 * statusCode = HTTP status (400, 404, 502...), code = stable machine-readable name.
 */
class AppError extends Error {
  constructor(statusCode, code, message) {
    super(message);
    this.name = 'AppError';
    this.statusCode = statusCode;
    this.code = code;
  }
}

module.exports = AppError;