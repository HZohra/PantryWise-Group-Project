/**
 * An error we create on purpose, so its message is safe to show the client.
 * statusCode = HTTP status, code = stable machine-readable name,
 * details = optional list of specific problems (e.g. validation errors).
 */
 class AppError extends Error {
    constructor(statusCode, code, message, details) {
        super(message);
        this.name = 'AppError';
        this.statusCode = statusCode;
        this.code = code;
        if (details) this.details = details;
    }
}

module.exports = AppError;