const crypto = require('node:crypto');
const { generateReply } = require('./claudeService');
const { buildAiRecipePrompt, EXPIRING_SOON_DAYS, sortByUrgency } = require('../utils/prompts');
const { preparePantry, normalizeName, namesMatch, STAPLES } = require('./recommendationService');
const { extractJson } = require('./substituteService');
const AppError = require('../utils/AppError');

// A full recipe plus Claude's hidden thinking needs more room than a substitution.
const MAX_REPLY_TOKENS = 3000;

/** What we accept from Claude. Anything outside these limits counts as an unusable answer. */
const RECIPE_LIMITS = Object.freeze({
  titleMax: 120,
  ingredientsMin: 2,
  ingredientsMax: 25,
  stepsMin: 2,
  stepsMax: 15,
  nameMax: 60,
  amountMax: 60,
  stepMax: 400,
  tipMax: 200,
  servingsMax: 12,
  timeMax: 600,
});

const RETRY_NOTE = '\n\nYour previous answer could not be used. Return ONLY the JSON object described, '
  + 'with every field, and include none of the ALLERGIES.';

/** Trimmed string capped at `max` characters; anything that isn't a string becomes "". */
const shortText = (value, max) => (typeof value === 'string' ? value.trim().slice(0, max) : '');

/** Whole number in 1..max, or null. */
const wholeIn = (value, max) => (Number.isInteger(value) && value >= 1 && value <= max ? value : null);

/** The pantry split into usable items and the ones expiring within 3 days. */
function choosePantryItems(inventory) {
  const usable = inventory.filter((i) => i.daysUntilExpiry === null || i.daysUntilExpiry >= 0);
  const expiring = sortByUrgency(usable.filter(
    (i) => i.daysUntilExpiry !== null && i.daysUntilExpiry <= EXPIRING_SOON_DAYS,
  ));
  return { usable, expiring, usedExpiring: expiring.length > 0 };
}

/**
 * Claude's text → a clean recipe, or null if it isn't usable (missing fields, too few steps…).
 * Step numbers Claude adds anyway ("1. ") are removed, because the app numbers the steps.
 */
function readRecipe(text) {
  const data = extractJson(text);
  if (!data || typeof data !== 'object') return null;

  const title = shortText(data.title, RECIPE_LIMITS.titleMax);
  const ingredients = (Array.isArray(data.ingredients) ? data.ingredients : [])
    .filter((i) => i && typeof i.name === 'string' && i.name.trim())
    .slice(0, RECIPE_LIMITS.ingredientsMax)
    .map((i) => ({
      name: shortText(i.name, RECIPE_LIMITS.nameMax).toLowerCase(),
      amount: shortText(i.amount, RECIPE_LIMITS.amountMax),
    }));
  const steps = (Array.isArray(data.steps) ? data.steps : [])
    .filter((s) => typeof s === 'string' && s.trim())
    .slice(0, RECIPE_LIMITS.stepsMax)
    .map((s) => shortText(s.replace(/^\s*\d+\s*[.)-]\s*/, ''), RECIPE_LIMITS.stepMax));

  if (!title || ingredients.length < RECIPE_LIMITS.ingredientsMin || steps.length < RECIPE_LIMITS.stepsMin) {
    return null;
  }
  return {
    title,
    servings: wholeIn(data.servings, RECIPE_LIMITS.servingsMax),
    timeMinutes: wholeIn(data.timeMinutes, RECIPE_LIMITS.timeMax),
    ingredients,
    steps,
    tip: shortText(data.tip, RECIPE_LIMITS.tipMax),
  };
}

/** The first ingredient that matches an allergy (same whole-word rules as the pantry), or null. */
function findAllergen(ingredients, allergies = []) {
  const blocked = allergies.map(normalizeName).filter(Boolean);
  for (const ingredient of ingredients) {
    const key = normalizeName(ingredient.name);
    if (key && blocked.some((allergy) => namesMatch(key, allergy))) return ingredient.name;
  }
  return null;
}

/** Adds inPantry / expiring / staple to each ingredient. Our code decides, not Claude. */
function markIngredients(ingredients, inventory, expiring) {
  const pantry = preparePantry(inventory); // also drops expired items
  const expiringKeys = expiring.map((i) => normalizeName(i.name)).filter(Boolean);
  return ingredients.map((ingredient) => {
    const key = normalizeName(ingredient.name);
    return {
      ...ingredient,
      inPantry: key !== '' && pantry.some((p) => namesMatch(key, p.key)),
      expiring: key !== '' && expiringKeys.some((k) => namesMatch(key, k)),
      staple: STAPLES.has(key),
    };
  });
}

/**
 * One recipe built around the user's expiring food (UC-2).
 * Unusable or unsafe answers are asked for again once; then an AppError is thrown.
 * @param {{inventory: object[], preferences: object, locale: string, servings: number}} input  validated
 * @returns {Promise<{recipe: object, usedExpiring: boolean, expiringItems: string[]}>}
 * @throws {AppError} 502 AI_BAD_FORMAT / AI_UNSAFE_RECIPE, or any error from claudeService
 */
async function generateAiRecipe({ inventory, preferences, locale, servings }) {
  const { usable, expiring, usedExpiring } = choosePantryItems(inventory);
  const { system, userMessage } = buildAiRecipePrompt({
    focus: usedExpiring ? expiring : usable,
    usable,
    usedExpiring,
    preferences,
    locale,
    servings,
  });

  let problem = 'format';
  for (let attempt = 1; attempt <= 2; attempt += 1) {
    const text = await generateReply({
      system,
      messages: [{ role: 'user', content: attempt === 1 ? userMessage : userMessage + RETRY_NOTE }],
      maxTokens: MAX_REPLY_TOKENS,
    });

    const recipe = readRecipe(text);
    if (!recipe) {
      problem = 'format';
      console.warn(`[ai-recipe] attempt ${attempt}: unusable reply (${String(text).length} chars)`);
      continue;
    }
    if (findAllergen(recipe.ingredients, preferences.allergies)) {
      problem = 'allergen';
      console.warn(`[ai-recipe] attempt ${attempt}: discarded a recipe that used an allergy ingredient`);
      continue;
    }

    const ingredients = markIngredients(recipe.ingredients, inventory, expiring);
    return {
      recipe: {
        recipeId: `ai:${crypto.randomUUID()}`,
        title: recipe.title,
        servings: recipe.servings ?? servings,
        totalTimeMinutes: recipe.timeMinutes,
        ingredients,
        ingredientLines: ingredients.map((i) => `${i.amount} ${i.name}`.trim()),
        steps: recipe.steps,
        tip: recipe.tip,
      },
      usedExpiring,
      expiringItems: expiring.map((i) => i.name),
    };
  }

  throw problem === 'allergen'
    ? new AppError(502, 'AI_UNSAFE_RECIPE', 'Could not create a recipe without your allergens. Please try again.')
    : new AppError(502, 'AI_BAD_FORMAT', 'Could not create a recipe. Please try again.');
}

module.exports = { generateAiRecipe, readRecipe, findAllergen, choosePantryItems };