const { searchRecipes } = require('../src/services/edamamService');

async function main() {
  const recipes = await searchRecipes({ query: 'chicken', diet: 'balanced', maxTimeMinutes: 45, limit: 3 });
  console.log(`Got ${recipes.length} recipes:`);
  recipes.forEach((r) => {
    console.log(`- ${r.title} | ${r.recipeId} | ${r.totalTimeMinutes ?? '?'} min | ${r.ingredientNames.length} ingredients`);
  });
  if (recipes[0]) {
    console.log('\nFirst recipe, full shape:');
    console.log(JSON.stringify(recipes[0], null, 2));
  }
}

main().catch((err) => {
  console.error('Mapped to:', err.statusCode, err.code, '-', err.message);
  process.exitCode = 1;
});