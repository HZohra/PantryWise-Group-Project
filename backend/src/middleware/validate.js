const AppError = require('../utils/AppError');

const LIMITS = Object.freeze({
  messageMax: 1000,
  historyMax: 20,
  historyContentMax: 2000,
  inventoryMax: 200,
  nameMax: 100,
  unitMax: 20,
  queryMax: 100,
  maxTimeMax: 600,
  searchLimitMax: 20,
  recommendLimitMax: 10,
  titleMax: 150,
  ingredientLinesMax: 50,
  lineMax: 200,
  allergiesMax: 20,
  allergyMax: 50,
  localeMax: 20,
});

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

const RECOMMEND_MODES = ['standard', 'zeroWaste'];

// Fitness goals the app offers (AI only). A fixed list, so the prompt only ever gets known values.
const FITNESS_GOALS = ['lose-weight', 'build-muscle', 'maintain-weight', 'eat-healthier'];

// Languages the app ships strings for. Any other language falls back to English instead of failing.
const SUPPORTED_LOCALES = ['en', 'es'];
const DEFAULT_LOCALE = 'en';

// Allergy names: letters in any alphabet, spaces, hyphens, apostrophes (e.g. "tree nuts", "maní")
const ALLERGY_PATTERN = /^\p{L}[\p{L}\s'-]*$/u;
// Language tags like "en", "es", "es-MX", "es_US"
const LOCALE_PATTERN = /^[a-z]{2,3}([-_][a-z0-9]{2,8})*$/i;

const isPlainObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v);
const isNonEmptyString = (v) => typeof v === 'string' && v.trim().length > 0;

// =====================================================================
// Shared helpers: each one pushes problems into `errors` and returns a clean value
// =====================================================================

/** Checks a value against an allow-list, case-insensitively. */
function pickFrom(name, value, allowed, errors) {
  const v = String(value).trim().toLowerCase();
  if (allowed.includes(v)) return v;
  errors.push(`${name} must be one of: ${allowed.join(', ')}`);
  return undefined;
}

/** Checks a list of health labels; returns them lowercased and de-duplicated. */
function checkHealthLabels(name, labels, errors) {
  const cleaned = labels.map((l) => String(l).trim().toLowerCase()).filter(Boolean);
  if (cleaned.some((l) => !HEALTH_LABELS.includes(l))) {
    errors.push(`${name} labels must be from: ${HEALTH_LABELS.join(', ')}`);
    return [];
  }
  return [...new Set(cleaned)];
}

/** Checks a list of free-text allergies; returns them lowercased, single-spaced and de-duplicated. */
function checkAllergies(name, list, errors) {
  if (!Array.isArray(list)) {
    errors.push(`${name} must be an array`);
    return [];
  }
  if (list.length > LIMITS.allergiesMax) {
    errors.push(`${name} must have at most ${LIMITS.allergiesMax} items`);
    return [];
  }
  const cleaned = [];
  for (const entry of list) {
    const v = typeof entry === 'string' ? entry.trim().toLowerCase().replace(/\s+/g, ' ') : '';
    if (!v || v.length > LIMITS.allergyMax || !ALLERGY_PATTERN.test(v)) {
      errors.push(`${name} entries must be words of at most ${LIMITS.allergyMax} characters (letters, spaces, - and ' only)`);
      return [];
    }
    cleaned.push(v);
  }
  return [...new Set(cleaned)];
}

/**
 * Checks the app language. Missing → "en". A valid tag in an unsupported language
 * (e.g. "fr") → "en", so a phone in another language still gets answers.
 */
function checkLocale(value, errors) {
  if (value === undefined || value === null) return DEFAULT_LOCALE;
  if (typeof value !== 'string' || value.length > LIMITS.localeMax || !LOCALE_PATTERN.test(value.trim())) {
    errors.push('locale must be a language code such as "en" or "es-MX"');
    return DEFAULT_LOCALE;
  }
  const language = value.trim().split(/[-_]/)[0].toLowerCase();
  return SUPPORTED_LOCALES.includes(language) ? language : DEFAULT_LOCALE;
}

/** Checks an inventory array (shared by /ai/chat, /recipes/recommend and /recipes/substitute). */
function checkInventoryItems(items, errors) {
  const clean = [];
  if (items === undefined) return clean;
  if (!Array.isArray(items)) {
    errors.push('inventory must be an array');
    return clean;
  }
  if (items.length > LIMITS.inventoryMax) {
    errors.push(`inventory must have at most ${LIMITS.inventoryMax} items`);
    return clean;
  }
  items.forEach((item, i) => {
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
    clean.push({
      name: item.name.trim().toLowerCase(),
      quantity: quantity ?? null,
      unit: (unit ?? '').trim(),
      daysUntilExpiry: daysUntilExpiry ?? null, // null = no expiry date known
    });
  });
  return clean;
}

/**
 * Checks the preferences object (shared by /ai/chat, /recipes/recommend and /recipes/substitute).
 * Returns the option names edamamService expects (cuisineType, maxTimeMinutes)
 * plus AI-only fields (allergies, fitnessGoal), which Edamam calls simply ignore.
 */
function checkPreferences(prefs, errors) {
  const clean = { health: [], allergies: [] };
  if (prefs === undefined) return clean;
  if (!isPlainObject(prefs)) {
    errors.push('preferences must be an object');
    return clean;
  }
  if (prefs.diet !== undefined) clean.diet = pickFrom('preferences.diet', prefs.diet, DIETS, errors);
  if (prefs.health !== undefined) {
    if (!Array.isArray(prefs.health)) errors.push('preferences.health must be an array');
    else clean.health = checkHealthLabels('preferences.health', prefs.health, errors);
  }
  if (prefs.cuisine !== undefined) clean.cuisineType = pickFrom('preferences.cuisine', prefs.cuisine, CUISINES, errors);
  if (prefs.mealType !== undefined) clean.mealType = pickFrom('preferences.mealType', prefs.mealType, MEAL_TYPES, errors);
  if (prefs.maxTime !== undefined) {
    if (!Number.isInteger(prefs.maxTime) || prefs.maxTime < 1 || prefs.maxTime > LIMITS.maxTimeMax) {
      errors.push(`preferences.maxTime must be a whole number from 1 to ${LIMITS.maxTimeMax}`);
    } else {
      clean.maxTimeMinutes = prefs.maxTime;
    }
  }
  if (prefs.allergies !== undefined) {
    clean.allergies = checkAllergies('preferences.allergies', prefs.allergies, errors);
  }
  if (prefs.fitnessGoal !== undefined) {
    clean.fitnessGoal = pickFrom('preferences.fitnessGoal', prefs.fitnessGoal, FITNESS_GOALS, errors);
  }
  return clean;
}

// =====================================================================
// Request checkers: one per endpoint. Each returns { errors, clean }
// =====================================================================

/** POST /api/v1/ai/chat */
function checkChatRequest(body) {
  const errors = [];
  const clean = {
    message: '',
    history: [],
    inventory: [],
    preferences: { health: [], allergies: [] },
    locale: DEFAULT_LOCALE,
  };

  if (!isPlainObject(body)) {
    return { errors: ['Body must be a JSON object'], clean };
  }

  if (!isNonEmptyString(body.message)) {
    errors.push('message is required and must be a non-empty string');
  } else if (body.message.trim().length > LIMITS.messageMax) {
    errors.push(`message must be at most ${LIMITS.messageMax} characters`);
  } else {
    clean.message = body.message.trim();
  }

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

  clean.inventory = checkInventoryItems(body.inventory, errors);
  clean.preferences = checkPreferences(body.preferences, errors);
  clean.locale = checkLocale(body.locale, errors);
  return { errors, clean };
}

/** GET /api/v1/recipes/search (query values are strings, or arrays if a param is repeated) */
function checkRecipeSearchQuery(query) {
  const errors = [];
  const clean = { query: '', health: [], limit: 10 };

  const get = (name) => {
    const value = query[name];
    if (value === undefined) return undefined;
    if (typeof value !== 'string') {
      errors.push(`${name} must be given only once`);
      return undefined;
    }
    return value.trim();
  };

  const intIn = (name, value, min, max) => {
    if (!/^\d+$/.test(value) || Number(value) < min || Number(value) > max) {
      errors.push(`${name} must be a whole number from ${min} to ${max}`);
      return undefined;
    }
    return Number(value);
  };

  const q = get('q');
  if (!q) errors.push('q is required');
  else if (q.length > LIMITS.queryMax) errors.push(`q must be at most ${LIMITS.queryMax} characters`);
  else clean.query = q;

  const diet = get('diet');
  if (diet) clean.diet = pickFrom('diet', diet, DIETS, errors);

  const health = get('health');
  if (health) clean.health = checkHealthLabels('health', health.split(','), errors);

  const cuisine = get('cuisine');
  if (cuisine) clean.cuisineType = pickFrom('cuisine', cuisine, CUISINES, errors);

  const mealType = get('mealType');
  if (mealType) clean.mealType = pickFrom('mealType', mealType, MEAL_TYPES, errors);

  const maxTime = get('maxTime');
  if (maxTime) clean.maxTimeMinutes = intIn('maxTime', maxTime, 1, LIMITS.maxTimeMax);

  const limit = get('limit');
  if (limit) clean.limit = intIn('limit', limit, 1, LIMITS.searchLimitMax) ?? 10;

  return { errors, clean };
}

/** POST /api/v1/recipes/recommend */
function checkRecommendRequest(body) {
  const errors = [];
  const clean = {
    query: undefined,
    inventory: [],
    preferences: { health: [], allergies: [] },
    limit: 5,
    mode: 'standard',
  };

  if (!isPlainObject(body)) {
    return { errors: ['Body must be a JSON object'], clean };
  }

  clean.inventory = checkInventoryItems(body.inventory, errors);

  if (body.query !== undefined) {
    if (!isNonEmptyString(body.query) || body.query.trim().length > LIMITS.queryMax) {
      errors.push(`query must be a non-empty string of at most ${LIMITS.queryMax} characters`);
    } else {
      clean.query = body.query.trim();
    }
  }

  clean.preferences = checkPreferences(body.preferences, errors);

  if (body.limit !== undefined) {
    if (!Number.isInteger(body.limit) || body.limit < 1 || body.limit > LIMITS.recommendLimitMax) {
      errors.push(`limit must be a whole number from 1 to ${LIMITS.recommendLimitMax}`);
    } else {
      clean.limit = body.limit;
    }
  }

  if (body.mode !== undefined) {
    if (!RECOMMEND_MODES.includes(body.mode)) {
      errors.push(`mode must be one of: ${RECOMMEND_MODES.join(', ')}`);
    } else {
      clean.mode = body.mode;
    }
  }

  // Needs something to search for: a query, or at least one item that isn't expired
  const hasUsableItem = clean.inventory.some((i) => i.daysUntilExpiry === null || i.daysUntilExpiry >= 0);
  if (!clean.query && !hasUsableItem) {
    errors.push('Provide a query or at least one inventory item that is not expired');
  }

  return { errors, clean };
}

/** POST /api/v1/recipes/substitute */
function checkSubstituteRequest(body) {
  const errors = [];
  const clean = {
    recipeTitle: '',
    ingredient: '',
    ingredientLines: [],
    inventory: [],
    preferences: { health: [], allergies: [] },
    locale: DEFAULT_LOCALE,
  };

  if (!isPlainObject(body)) {
    return { errors: ['Body must be a JSON object'], clean };
  }

  if (!isNonEmptyString(body.recipeTitle) || body.recipeTitle.trim().length > LIMITS.titleMax) {
    errors.push(`recipeTitle is required (max ${LIMITS.titleMax} characters)`);
  } else {
    clean.recipeTitle = body.recipeTitle.trim();
  }

  if (!isNonEmptyString(body.ingredient) || body.ingredient.trim().length > LIMITS.nameMax) {
    errors.push(`ingredient is required (max ${LIMITS.nameMax} characters)`);
  } else {
    clean.ingredient = body.ingredient.trim();
  }

  if (body.ingredientLines !== undefined) {
    if (!Array.isArray(body.ingredientLines) || body.ingredientLines.length > LIMITS.ingredientLinesMax) {
      errors.push(`ingredientLines must be an array of at most ${LIMITS.ingredientLinesMax} strings`);
    } else if (body.ingredientLines.some((l) => typeof l !== 'string' || l.length > LIMITS.lineMax)) {
      errors.push(`each ingredientLines entry must be a string (max ${LIMITS.lineMax} characters)`);
    } else {
      clean.ingredientLines = body.ingredientLines.map((l) => l.trim()).filter(Boolean);
    }
  }

  clean.inventory = checkInventoryItems(body.inventory, errors);   // reused from chat/recommend
  clean.preferences = checkPreferences(body.preferences, errors);  // reused from chat/recommend
  clean.locale = checkLocale(body.locale, errors);                 // reused from chat
  return { errors, clean };
}
// =====================================================================
// Middleware factories
// =====================================================================

/** Bad body → 400 VALIDATION_ERROR. Good body → req.body is REPLACED by the clean version. */
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

/** Like validateBody, but Express 5 makes req.query read-only, so the clean copy goes in req.validatedQuery. */
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
  checkRecommendRequest,
  checkSubstituteRequest,
  LIMITS,
  DIETS,
  HEALTH_LABELS,
  CUISINES,
  MEAL_TYPES,
  RECOMMEND_MODES,
  FITNESS_GOALS,
  SUPPORTED_LOCALES,
};