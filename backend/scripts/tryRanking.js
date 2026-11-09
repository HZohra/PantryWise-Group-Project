const { rankRecipes, normalizeName, namesMatch } = require('../src/services/recommendationService');

// --- quick checks of the helpers ---
console.log('normalize:', normalizeName('Eggs'), '|', normalizeName('Tomatoes'), '|', normalizeName('2% Milk'), '|', normalizeName('Berries'));
console.log('chicken vs chicken breast:', namesMatch('chicken', 'chicken breast'));
console.log('pea vs peanut butter:     ', namesMatch('pea', 'peanut butter'));
console.log('');

// --- fake inventory (same shape the validator produces) ---
const inventory = [
  { name: 'milk', quantity: 1, unit: 'L', daysUntilExpiry: 2 },
  { name: 'eggs', quantity: 6, unit: 'pcs', daysUntilExpiry: 10 },
  { name: 'spinach', quantity: 1, unit: 'bag', daysUntilExpiry: 1 },
  { name: 'chicken breast', quantity: 2, unit: 'pcs', daysUntilExpiry: 0 },
  { name: 'rice', quantity: 1, unit: 'kg', daysUntilExpiry: null },
  { name: 'yogurt', quantity: 1, unit: 'cup', daysUntilExpiry: -1 }, // expired → must be ignored
  { name: 'peanut butter', quantity: 1, unit: 'jar', daysUntilExpiry: 90 },
];

// --- fake recipes (only the fields ranking uses) ---
const recipes = [
  { recipeId: 'test:tacos', title: 'Beef Tacos', totalTimeMinutes: 25, ingredientNames: ['beef', 'taco shells', 'cheese', 'lettuce'] },
  { recipeId: 'test:parfait', title: 'Yogurt Parfait', totalTimeMinutes: 5, ingredientNames: ['yogurt', 'granola', 'honey'] },
  { recipeId: 'test:friedrice', title: 'Chicken Fried Rice', totalTimeMinutes: 20, ingredientNames: ['chicken', 'rice', 'eggs', 'soy sauce', 'peas', 'green onion'] },
  { recipeId: 'test:soup', title: 'Pea Soup', totalTimeMinutes: 40, ingredientNames: ['peas', 'onion', 'water'] },
  { recipeId: 'test:omelette', title: 'Spinach Omelette', totalTimeMinutes: 10, ingredientNames: ['eggs', 'spinach', 'milk', 'salt', 'black pepper'] },
];

for (const r of rankRecipes(recipes, inventory)) {
  const m = r.match;
  const exp = m.expiringUsed.map((e) => `${e.name}(${e.daysUntilExpiry}d)`).join(', ') || '-';
  console.log(`${String(m.score).padStart(3)} | ${r.title.padEnd(18)} | have ${m.haveCount}/${m.totalCount} | missing: ${m.missing.join(', ') || '-'} | expiring: ${exp}`);
}