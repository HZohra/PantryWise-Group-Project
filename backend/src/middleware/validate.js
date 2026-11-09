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

// Values Edamam accepts. Checking them here gives a clear 400 instead of a confusing 502.
const DIETS = ['balanced', 'high-fiber', 'high-protein', 'low-carb', 'low-fat', 'low-sodium'];
const HEALTH_LABELS = [
  'vegan', 'vegetarian', 'pescatarian', 'gluten-free', 'dairy-free', 'egg-free',
  'peanut-free', 'tree-nut-free', 'soy-free', 'fish-free', 'shellfish-free',
  'pork-free', 'red-meat-free', 'alcohol-free', 'kosher',
];
const CUISINES = [
  'american', 'asian', 'british', 'caribbean', 'central europe', 'chinese', 'eastern europe',
  'french', 'indian', 'italian', 'japanese', 'kosher', 'mediterranean', 'mexican',
  'middle eastern', 'nordic', 'south american', 'south east asian',
];
const MEAL_TYPES = ['breakfast', 'lunch', 'dinner', 'snack', 'teatime'];

/**
 * Checks GET /recipes/search query parameters.
 * Query values arrive as strings (or arrays if a param is repeated), so numbers must be parsed.
 * @returns {{ errors: string[], clean: object }}  clean uses the option names edamamService expects
 */
function checkRecipeSearchQuery(query) {
  const errors = [];
  const clean = { query: '', health: [], limit: 10 };

  // Reads one param as a trimmed string; rejects repeated params like ?diet=a&diet=b
  const get = (name) => {
    const value = query[name];
    if (value === undefined) return undefined;
    if (typeof value !== 'string') {
      errors.push(`${name} must be given only once`);
      return undefined;
    }
    return value.trim();
  };

  // Checks a value against an allow-list, case-insensitively
  const pickFrom = (name, value, allowed) => {
    const v = value.toLowerCase();
    if (allowed.includes(v)) return v;
    errors.push(`${name} must be one of: ${allowed.join(', ')}`);
    return undefined;
  };

  // Parses a whole number within [min, max]
  const intIn = (name, value, min, max) => {
    if (!/^\d+$/.test(value) || Number(value) < min || Number(value) > max) {
      errors.push(`${name} must be a whole number from ${min} to ${max}`);
      return undefined;
    }
    return Number(value);
  };

  const q = get('q');
  if (!q) errors.push('q is required');
  else if (q.length > 100) errors.push('q must be at most 100 characters');
  else clean.query = q;

  const diet = get('diet');
  if (diet) clean.diet = pickFrom('diet', diet, DIETS);

  const health = get('health');
  if (health) {
    const labels = health.toLowerCase().split(',').map((s) => s.trim()).filter(Boolean);
    const unknown = labels.filter((l) => !HEALTH_LABELS.includes(l));
    if (unknown.length > 0) errors.push(`health labels must be from: ${HEALTH_LABELS.join(', ')}`);
    else clean.health = [...new Set(labels)];
  }

  const cuisine = get('cuisine');
  if (cuisine) clean.cuisineType = pickFrom('cuisine', cuisine, CUISINES);

  const mealType = get('mealType');
  if (mealType) clean.mealType = pickFrom('mealType', mealType, MEAL_TYPES);

  const maxTime = get('maxTime');
  if (maxTime) clean.maxTimeMinutes = intIn('maxTime', maxTime, 1, 600);

  const limit = get('limit');
  if (limit) clean.limit = intIn('limit', limit, 1, 20) ?? 10;

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

/**
 * Like validateBody, but for query parameters.
 * Express 5 makes req.query read-only, so the clean copy goes in req.validatedQuery.
 */
function validateQuery(checker) {
  return (req, res, next) => {
    const { errors, clean } = checker(req.query);
    if (errors.length > 0) {
      return next(new AppError(400, 'VALIDATION_ERROR', 'Query parameters are invalid', errors));
    }
    req.validatedQuery = clean;
    return next();
  };
}

module.exports = {
  validateBody,
  validateQuery,
  checkChatRequest,
  checkRecipeSearchQuery,
  LIMITS,
  DIETS,
  HEALTH_LABELS,
  CUISINES,
  MEAL_TYPES,
};