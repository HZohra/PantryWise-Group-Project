const AppError = require('../utils/AppError');

const LIMITS = Object.freeze({
  messageMax: 1000,
  historyMax: 20,
  historyContentMax: 2000,
  inventoryMax: 200,
  nameMax: 100,
  unitMax: 20,
});

const isPlainObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v);
const isNonEmptyString = (v) => typeof v === 'string' && v.trim().length > 0;

/**
 * Checks a chat request body.
 * @returns {{ errors: string[], clean: object }}  clean holds ONLY allowed, trimmed fields
 */
function checkChatRequest(body) {
  const errors = [];
  const clean = { message: '', history: [], inventory: [] };

  if (!isPlainObject(body)) {
    return { errors: ['Body must be a JSON object'], clean };
  }

  // message
  if (!isNonEmptyString(body.message)) {
    errors.push('message is required and must be a non-empty string');
  } else if (body.message.trim().length > LIMITS.messageMax) {
    errors.push(`message must be at most ${LIMITS.messageMax} characters`);
  } else {
    clean.message = body.message.trim();
  }

  // history (optional)
  if (body.history !== undefined) {
    if (!Array.isArray(body.history)) {
      errors.push('history must be an array');
    } else if (body.history.length > LIMITS.historyMax) {
      errors.push(`history must have at most ${LIMITS.historyMax} messages`);
    } else {
      body.history.forEach((m, i) => {
        if (!isPlainObject(m) || !['user', 'assistant'].includes(m.role) || !isNonEmptyString(m.content)) {
          errors.push(`history[${i}] must have role "user" or "assistant" and non-empty content`);
        } else if (m.content.length > LIMITS.historyContentMax) {
          errors.push(`history[${i}].content must be at most ${LIMITS.historyContentMax} characters`);
        } else {
          clean.history.push({ role: m.role, content: m.content.trim() });
        }
      });
    }
  }

  // inventory (optional)
  if (body.inventory !== undefined) {
    if (!Array.isArray(body.inventory)) {
      errors.push('inventory must be an array');
    } else if (body.inventory.length > LIMITS.inventoryMax) {
      errors.push(`inventory must have at most ${LIMITS.inventoryMax} items`);
    } else {
      body.inventory.forEach((item, i) => {
        if (!isPlainObject(item) || !isNonEmptyString(item.name) || item.name.length > LIMITS.nameMax) {
          errors.push(`inventory[${i}].name is required (max ${LIMITS.nameMax} characters)`);
          return;
        }
        const { quantity, unit, daysUntilExpiry } = item;
        if (quantity !== undefined && (typeof quantity !== 'number' || !Number.isFinite(quantity) || quantity < 0)) {
          errors.push(`inventory[${i}].quantity must be a number >= 0`);
          return;
        }
        if (unit !== undefined && (typeof unit !== 'string' || unit.length > LIMITS.unitMax)) {
          errors.push(`inventory[${i}].unit must be a string (max ${LIMITS.unitMax} characters)`);
          return;
        }
        if (daysUntilExpiry !== undefined && daysUntilExpiry !== null && !Number.isInteger(daysUntilExpiry)) {
          errors.push(`inventory[${i}].daysUntilExpiry must be a whole number or null`);
          return;
        }
        clean.inventory.push({
          name: item.name.trim().toLowerCase(),
          quantity: quantity ?? null,
          unit: (unit ?? '').trim(),
          daysUntilExpiry: daysUntilExpiry ?? null, // null = no expiry date known
        });
      });
    }
  }

  return { errors, clean };
}

/**
 * Turns a checker function into Express middleware.
 * Bad input → 400 VALIDATION_ERROR with every problem listed in `details`.
 * Good input → req.body is REPLACED by the clean version.
 */
function validateBody(checker) {
  return (req, res, next) => {
    const { errors, clean } = checker(req.body);
    if (errors.length > 0) {
      return next(new AppError(400, 'VALIDATION_ERROR', 'Request body is invalid', errors));
    }
    req.body = clean;
    return next();
  };
}

module.exports = { validateBody, checkChatRequest, LIMITS };