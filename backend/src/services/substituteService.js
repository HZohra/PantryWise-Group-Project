const { generateReply } = require('./claudeService');
const { buildSubstitutePrompt } = require('../utils/prompts');
const { preparePantry, normalizeName, namesMatch } = require('./recommendationService');
const AppError = require('../utils/AppError');

// Claude's hidden thinking shares this budget; 1024 sometimes cut the JSON off before its last "}".
const MAX_REPLY_TOKENS = 2048;
const MAX_SUBSTITUTES = 3;

// =====================================================================
// Local substitution map (US "Quick substitutions without AI")
// =====================================================================

/**
 * Common swaps answered instantly, without Claude.
 * - aliases: exact names (after normalizeName) that find this entry; exact on purpose, so
 *   "peanut butter" never finds "butter".
 * - name: English, so the "in your pantry" check works; amount/notes follow the app language.
 * - tags: what the substitute contains, used to respect diets and allergies.
 */
const LOCAL_SUBSTITUTES = [
  {
    aliases: ['butter', 'unsalted butter', 'salted butter'],
    substitutes: [
      { name: 'vegetable oil', tags: [],
        amount: { en: '3/4 cup per 1 cup butter', es: '3/4 de taza por cada taza de mantequilla' },
        notes: { en: 'Best in muffins and quick breads; cookies will spread more.', es: 'Ideal para muffins y panes rápidos; las galletas quedarán más planas.' } },
      { name: 'olive oil', tags: [],
        amount: { en: '3/4 cup per 1 cup butter', es: '3/4 de taza por cada taza de mantequilla' },
        notes: { en: 'Good for savory cooking; adds a fruity flavor.', es: 'Buena para platos salados; aporta un sabor afrutado.' } },
      { name: 'applesauce', tags: [],
        amount: { en: '1/2 cup per 1 cup butter', es: '1/2 taza por cada taza de mantequilla' },
        notes: { en: 'For baking only; the result is moister and less rich.', es: 'Solo para hornear; queda más húmedo y menos graso.' } },
      { name: 'plain yogurt', tags: ['dairy'],
        amount: { en: '1/2 cup per 1 cup butter', es: '1/2 taza por cada taza de mantequilla' },
        notes: { en: 'For baking; adds moisture and a little tang.', es: 'Para hornear; aporta humedad y un toque ácido.' } },
    ],
  },
  {
    aliases: ['egg', 'large egg', 'whole egg'],
    substitutes: [
      { name: 'flaxseed', tags: [],
        amount: { en: '1 tbsp ground flaxseed + 3 tbsp water per egg', es: '1 cda de linaza molida + 3 cdas de agua por huevo' },
        notes: { en: 'Let it sit 5 minutes to thicken. Works in muffins, pancakes and cookies.', es: 'Déjalo reposar 5 minutos para que espese. Funciona en muffins, panqueques y galletas.' } },
      { name: 'banana', tags: [],
        amount: { en: '1/4 cup mashed banana per egg', es: '1/4 de taza de plátano machacado por huevo' },
        notes: { en: 'Adds sweetness; best in sweet baking.', es: 'Aporta dulzor; mejor en repostería dulce.' } },
      { name: 'applesauce', tags: [],
        amount: { en: '1/4 cup per egg', es: '1/4 de taza por huevo' },
        notes: { en: 'Keeps cakes moist; not for omelettes or binding meat.', es: 'Mantiene los bizcochos húmedos; no sirve para tortillas ni para ligar carne.' } },
      { name: 'plain yogurt', tags: ['dairy'],
        amount: { en: '1/4 cup per egg', es: '1/4 de taza por huevo' },
        notes: { en: 'Good in cakes and muffins; adds a little tang.', es: 'Bueno en bizcochos y muffins; aporta un toque ácido.' } },
    ],
  },
  {
    aliases: ['milk', 'whole milk', 'skim milk', 'cow milk'],
    substitutes: [
      { name: 'plain yogurt', tags: ['dairy'],
        amount: { en: 'Equal amount, thinned with a little water', es: 'La misma cantidad, diluida con un poco de agua' },
        notes: { en: 'Slightly tangy; works in batters, sauces and eggs.', es: 'Un poco ácido; sirve en masas, salsas y huevos.' } },
      { name: 'oat milk', tags: [],
        amount: { en: 'Equal amount', es: 'La misma cantidad' },
        notes: { en: 'Neutral taste; works almost everywhere.', es: 'Sabor neutro; funciona casi en todo.' } },
      { name: 'soy milk', tags: ['soy'],
        amount: { en: 'Equal amount', es: 'La misma cantidad' },
        notes: { en: 'Closest to dairy milk in protein; good for baking.', es: 'La más parecida a la leche de vaca en proteína; buena para hornear.' } },
      { name: 'water', tags: [],
        amount: { en: 'Equal amount', es: 'La misma cantidad' },
        notes: { en: 'Fine in small amounts (eggs, pancakes); the result is less rich.', es: 'Sirve en poca cantidad (huevos, panqueques); el resultado es menos cremoso.' } },
    ],
  },
  {
    aliases: ['buttermilk'],
    substitutes: [
      { name: 'milk', tags: ['dairy'],
        amount: { en: '1 cup milk + 1 tbsp lemon juice or vinegar', es: '1 taza de leche + 1 cda de jugo de limón o vinagre' },
        notes: { en: 'Let it sit 5 minutes until it thickens slightly.', es: 'Déjala reposar 5 minutos hasta que espese un poco.' } },
      { name: 'plain yogurt', tags: ['dairy'],
        amount: { en: '3/4 cup yogurt + 1/4 cup water per cup', es: '3/4 de taza de yogur + 1/4 de taza de agua por taza' },
        notes: { en: 'Same tang; great for pancakes and marinades.', es: 'La misma acidez; ideal para panqueques y adobos.' } },
      { name: 'oat milk', tags: [],
        amount: { en: '1 cup oat milk + 1 tbsp lemon juice', es: '1 taza de bebida de avena + 1 cda de jugo de limón' },
        notes: { en: 'Dairy-free option; slightly thinner.', es: 'Opción sin lácteos; un poco más líquida.' } },
    ],
  },
  {
    aliases: ['heavy cream', 'whipping cream', 'heavy whipping cream', 'double cream', 'cream'],
    substitutes: [
      { name: 'milk', tags: ['dairy'],
        amount: { en: '3/4 cup milk + 1/4 cup melted butter per cup', es: '3/4 de taza de leche + 1/4 de taza de mantequilla derretida por taza' },
        notes: { en: 'Works in sauces and soups; it will not whip.', es: 'Sirve para salsas y sopas; no se puede montar.' } },
      { name: 'coconut cream', tags: [],
        amount: { en: 'Equal amount', es: 'La misma cantidad' },
        notes: { en: 'Rich and dairy-free; adds a light coconut flavor.', es: 'Cremosa y sin lácteos; aporta un ligero sabor a coco.' } },
      { name: 'cashews', tags: ['tree-nut'],
        amount: { en: '1 cup soaked cashews blended with 1/2 cup water per cup', es: '1 taza de anacardos remojados, batidos con 1/2 taza de agua, por taza' },
        notes: { en: 'Soak 30 minutes in hot water first; very creamy and dairy-free.', es: 'Remójalos 30 minutos en agua caliente antes; muy cremoso y sin lácteos.' } },
      { name: 'oat milk', tags: [],
        amount: { en: '1 cup oat milk + 1 tbsp cornstarch per cup', es: '1 taza de bebida de avena + 1 cda de maicena por taza' },
        notes: { en: 'Simmer to thicken; good for creamy pasta sauces.', es: 'Cocínala a fuego lento para que espese; buena para salsas de pasta cremosas.' } },
    ],
  },
  {
    aliases: ['sour cream'],
    substitutes: [
      { name: 'plain yogurt', tags: ['dairy'],
        amount: { en: 'Equal amount (Greek yogurt is closest)', es: 'La misma cantidad (el yogur griego es el más parecido)' },
        notes: { en: 'Stir it in at the end so it does not split.', es: 'Añádelo al final para que no se corte.' } },
      { name: 'cashews', tags: ['tree-nut'],
        amount: { en: '1 cup soaked cashews + 1/2 cup water + 1 tbsp lemon juice', es: '1 taza de anacardos remojados + 1/2 taza de agua + 1 cda de jugo de limón' },
        notes: { en: 'Blend until smooth; dairy-free and tangy.', es: 'Bátelo hasta que quede liso; sin lácteos y ácido.' } },
    ],
  },
  {
    aliases: ['sugar', 'white sugar', 'granulated sugar', 'caster sugar'],
    substitutes: [
      { name: 'honey', tags: ['animal'],
        amount: { en: '3/4 cup per 1 cup sugar, with 1/4 cup less liquid', es: '3/4 de taza por cada taza de azúcar, con 1/4 de taza menos de líquido' },
        notes: { en: 'Lower the oven by 25 °F (15 °C); it browns faster.', es: 'Baja el horno 15 °C; se dora más rápido.' } },
      { name: 'maple syrup', tags: [],
        amount: { en: '3/4 cup per 1 cup sugar, with 3 tbsp less liquid', es: '3/4 de taza por cada taza de azúcar, con 3 cdas menos de líquido' },
        notes: { en: 'Adds a caramel flavor.', es: 'Aporta sabor a caramelo.' } },
      { name: 'brown sugar', tags: [],
        amount: { en: 'Equal amount', es: 'La misma cantidad' },
        notes: { en: 'Baked goods come out softer and a bit darker.', es: 'Los horneados quedan más blandos y un poco más oscuros.' } },
    ],
  },
  {
    aliases: ['brown sugar', 'light brown sugar', 'dark brown sugar'],
    substitutes: [
      { name: 'sugar', tags: [],
        amount: { en: '1 cup white sugar + 1 tbsp molasses per cup', es: '1 taza de azúcar blanca + 1 cda de melaza por taza' },
        notes: { en: 'Without molasses it still works, just less moist.', es: 'Sin melaza también funciona, solo queda menos húmedo.' } },
      { name: 'maple syrup', tags: [],
        amount: { en: '3/4 cup per 1 cup brown sugar, with 3 tbsp less liquid', es: '3/4 de taza por cada taza de azúcar morena, con 3 cdas menos de líquido' },
        notes: { en: 'Similar caramel flavor.', es: 'Sabor a caramelo parecido.' } },
    ],
  },
  {
    aliases: ['flour', 'all purpose flour', 'plain flour', 'white flour', 'wheat flour'],
    substitutes: [
      { name: 'whole wheat flour', tags: ['gluten'],
        amount: { en: 'Equal amount, plus 1–2 tbsp extra liquid per cup', es: 'La misma cantidad, más 1–2 cdas de líquido extra por taza' },
        notes: { en: 'Denser and nuttier; use half and half for a lighter result.', es: 'Más denso y con sabor a nuez; usa mitad y mitad para un resultado más ligero.' } },
      { name: 'oats', tags: ['gluten'], // oats are often cross-contaminated: not offered as gluten-free
        amount: { en: '1 1/3 cups blended oats per cup', es: '1 1/3 tazas de avena triturada por taza' },
        notes: { en: 'Blend rolled oats until fine; best for muffins and pancakes.', es: 'Tritura copos de avena hasta que queden finos; mejor para muffins y panqueques.' } },
      { name: 'cornstarch', tags: [],
        amount: { en: '1/2 tbsp per 1 tbsp flour', es: '1/2 cda por cada cda de harina' },
        notes: { en: 'Only for thickening sauces and gravies, not for baking.', es: 'Solo para espesar salsas, no para hornear.' } },
    ],
  },
  {
    aliases: ['baking powder'],
    substitutes: [
      { name: 'baking soda', tags: [],
        amount: { en: '1/4 tsp baking soda + 1/2 tsp cream of tartar per 1 tsp', es: '1/4 cdta de bicarbonato + 1/2 cdta de crémor tártaro por cada cdta' },
        notes: { en: 'Mix right before using and bake soon after.', es: 'Mézclalo justo antes de usar y hornea enseguida.' } },
    ],
  },
];

/** normalized alias → entry, built once. */
const LOCAL_INDEX = new Map();
for (const entry of LOCAL_SUBSTITUTES) {
  for (const alias of entry.aliases) LOCAL_INDEX.set(normalizeName(alias), entry);
}

/** Health labels → tags they forbid. */
const HEALTH_FORBIDS = {
  'dairy-free': ['dairy'],
  vegan: ['dairy', 'egg', 'animal'],
  'egg-free': ['egg'],
  'gluten-free': ['gluten'],
  'peanut-free': ['peanut'],
  'tree-nut-free': ['tree-nut'],
  'soy-free': ['soy'],
};

/** Common allergy words → tags they forbid (on top of the name check in removeAllergens). */
const ALLERGY_FORBIDS = {
  milk: ['dairy'], dairy: ['dairy'], lactose: ['dairy'],
  egg: ['egg'],
  gluten: ['gluten'], wheat: ['gluten'],
  soy: ['soy'],
  peanut: ['peanut'],
  nut: ['tree-nut', 'peanut'], 'tree nut': ['tree-nut'], cashew: ['tree-nut'],
};

/** Every tag the user's preferences forbid. */
function forbiddenTags(preferences = {}) {
  const tags = new Set();
  for (const label of preferences.health || []) (HEALTH_FORBIDS[label] || []).forEach((t) => tags.add(t));
  for (const allergy of preferences.allergies || []) {
    (ALLERGY_FORBIDS[normalizeName(allergy)] || []).forEach((t) => tags.add(t));
  }
  return tags;
}

/**
 * Local substitutes for this ingredient that fit the preferences, in the app language.
 * @returns {object[] | null}  null when the ingredient isn't in the local list
 */
function findLocalSubstitutes(ingredient, preferences, locale = 'en') {
  const entry = LOCAL_INDEX.get(normalizeName(ingredient));
  if (!entry) return null;
  const forbidden = forbiddenTags(preferences);
  return entry.substitutes
    .filter((s) => !s.tags.some((t) => forbidden.has(t)))
    .map((s) => ({
      name: s.name,
      amount: s.amount[locale] || s.amount.en,
      notes: s.notes[locale] || s.notes.en,
    }));
}

// =====================================================================
// Claude (only for ingredients the local list doesn't cover)
// =====================================================================

/**
 * Finds the first complete JSON object in a reply (ignores code fences and text around it).
 * @returns {object|null} null if there is none, e.g. because the reply was cut off
 */
function extractJson(text) {
  const cleaned = String(text).replace(/```(?:json)?/gi, '').trim();
  try {
    return JSON.parse(cleaned);
  } catch {
    // not pure JSON: look for the first complete object below
  }
  const start = cleaned.indexOf('{');
  if (start === -1) return null;
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let i = start; i < cleaned.length; i += 1) {
    const ch = cleaned[i];
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === '\\') escaped = true;
      else if (ch === '"') inString = false;
    } else if (ch === '"') {
      inString = true;
    } else if (ch === '{') {
      depth += 1;
    } else if (ch === '}') {
      depth -= 1;
      if (depth === 0) {
        try {
          return JSON.parse(cleaned.slice(start, i + 1));
        } catch {
          return null;
        }
      }
    }
  }
  return null; // the object never closed: the reply was cut off
}

/** Trimmed string capped at `max` characters; anything that isn't a string becomes "". */
const shortText = (value, max) => (typeof value === 'string' ? value.trim().slice(0, max) : '');

/** Claude's text → { substitutes, tip }, or null if it isn't usable. */
function readSubstitutes(text) {
  const data = extractJson(text);
  if (!data || !Array.isArray(data.substitutes)) return null;
  return {
    substitutes: data.substitutes
      .filter((s) => s && typeof s.name === 'string' && s.name.trim())
      .slice(0, MAX_SUBSTITUTES)
      .map((s) => ({
        name: shortText(s.name, 60).toLowerCase(),
        amount: shortText(s.amount, 160),
        notes: shortText(s.notes, 300),
      })),
    tip: shortText(data.tip, 200),
  };
}

/**
 * Turns Claude's text into { substitutes, tip }, keeping only the fields we expect.
 * @throws {AppError} 502 AI_BAD_FORMAT if the reply isn't usable JSON
 */
function parseSubstitutes(text) {
  const result = readSubstitutes(text);
  if (!result) {
    const reply = String(text);
    console.error(`[substitute] reply was not the expected JSON (${reply.length} chars). Ends with:`,
      reply.slice(-120));
    throw new AppError(502, 'AI_BAD_FORMAT', 'The assistant gave an unexpected answer. Please try again.');
  }
  return result;
}

/** Asks Claude; if the reply is unusable, asks once more (UC "retry once"). */
async function askClaude(input) {
  const { system, userMessage } = buildSubstitutePrompt(input);
  const ask = () => generateReply({
    system,
    messages: [{ role: 'user', content: userMessage }],
    maxTokens: MAX_REPLY_TOKENS,
  });

  const first = readSubstitutes(await ask());
  if (first) return first;
  console.warn('[substitute] unusable reply, retrying once');
  return parseSubstitutes(await ask()); // throws AI_BAD_FORMAT if it fails again
}

// =====================================================================
// Shared steps
// =====================================================================

/**
 * US-4 safety net: the prompt already forbids allergens, but if one still appears
 * it is dropped here. Uses the same name-matching rules as the pantry (e.g. "peanuts" ~ "peanut").
 */
function removeAllergens(substitutes, allergies = []) {
  const blocked = allergies.map(normalizeName).filter(Boolean);
  if (blocked.length === 0) return substitutes;
  return substitutes.filter((s) => {
    const key = normalizeName(s.name);
    const hit = blocked.find((allergy) => namesMatch(key, allergy) || namesMatch(allergy, key));
    if (hit) console.warn(`[substitute] dropped "${s.name}": matches an allergy`);
    return !hit;
  });
}

/** Our code decides inPantry, not Claude: reuses the 4.5 matching rules. */
function markPantryItems(substitutes, inventory) {
  const pantry = preparePantry(inventory); // also drops expired items
  return substitutes.map((s) => {
    const key = normalizeName(s.name);
    return { ...s, inPantry: key !== '' && pantry.some((p) => namesMatch(key, p.key)) };
  });
}

/** Allergens out, pantry items first (otherwise keeps the original order), at most 3. */
function finish(substitutes, input) {
  const safe = removeAllergens(substitutes, input.preferences.allergies);
  const marked = markPantryItems(safe, input.inventory);
  marked.sort((a, b) => Number(b.inPantry) - Number(a.inPantry));
  return marked.slice(0, MAX_SUBSTITUTES);
}

/**
 * Local list first (instant, no AI); Claude only when the ingredient isn't covered
 * or the user's diet/allergies rule out every local option.
 * @param {{recipeTitle, ingredient, ingredientLines, inventory, preferences, locale}} input  validated
 * @returns {Promise<{ingredient: string, source: 'local'|'ai', substitutes: object[], tip: string}>}
 */
async function suggestSubstitutes(input) {
  const local = findLocalSubstitutes(input.ingredient, input.preferences, input.locale);
  if (local) {
    const substitutes = finish(local, input);
    if (substitutes.length > 0) {
      return { ingredient: input.ingredient, source: 'local', substitutes, tip: '' };
    }
  }

  const { substitutes, tip } = await askClaude(input);
  return { ingredient: input.ingredient, source: 'ai', substitutes: finish(substitutes, input), tip };
}

module.exports = {
  suggestSubstitutes,
  parseSubstitutes,
  extractJson,
  removeAllergens,
  findLocalSubstitutes,
  LOCAL_SUBSTITUTES,
};