package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.data.model.MealType;
import com.cp470.groupproject.data.model.WasteStats;
import com.cp470.groupproject.data.repository.PantryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/**
 * Repository logic tested without the android ui: an in-memory database and a direct (synchronous)
 * executor make the asynchronous api deterministic.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class PantryRepositoryTest {

    /** makes livedata and room run synchronously in tests */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PantryDatabase.class)
                .allowMainThreadQueries().build();
        repository = new PantryRepository(db, Runnable::run);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void insertNormalizesName() {
        final long[] id = new long[1];
        repository.insertFoodItem(new FoodItem("2% Milk 1L", 1, "pcs", 1, 1, 0, 0), r -> id[0] = r);

        assertEquals("milk", db.foodItemDao().getById(id[0]).normalizedName);
    }

    @Test
    public void recordDispositionLogsAndRemovesFromInventory() {
        FoodItem item = new FoodItem("Yogurt", 2, "pcs", 1, 1, 0, 0);
        final long[] id = new long[1];
        repository.insertFoodItem(item, r -> id[0] = r);
        FoodItem saved = db.foodItemDao().getById(id[0]);

        final long[] historyId = new long[1];
        repository.recordDisposition(saved, ItemStatus.WASTED, r -> historyId[0] = r);

        assertEquals(0, db.foodItemDao().countActive());
        assertTrue(historyId[0] > 0);
        WasteStats stats = LiveDataTestUtil.getValue(repository.getWasteStats(0));
        assertNotNull(stats);
        assertEquals(1, stats.wastedCount);
        assertEquals(100.0, stats.getWastePercent(), 0.0001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void recordDispositionRejectsActiveStatus() {
        repository.recordDisposition(new FoodItem(), ItemStatus.ACTIVE, null);
    }

    @Test
    public void wasteStatsMixesOutcomes() {
        for (String outcome : new String[]{ItemStatus.CONSUMED, ItemStatus.CONSUMED,
                ItemStatus.CONSUMED, ItemStatus.EXPIRED}) {
            FoodItem item = new FoodItem("Item", 1, "pcs", 1, 1, 0, 0);
            final long[] id = new long[1];
            repository.insertFoodItem(item, r -> id[0] = r);
            repository.recordDisposition(db.foodItemDao().getById(id[0]), outcome, null);
        }
        WasteStats stats = LiveDataTestUtil.getValue(repository.getWasteStats(0));
        assertEquals(4, stats.totalCount);
        assertEquals(25.0, stats.getWastePercent(), 0.0001);
    }

    @Test
    public void clearCompletedRemovesOnlyCheckedShoppingItems() {
        final long[] ids = new long[2];
        repository.addShoppingItem(new ShoppingItem("Bread", 1, "pcs", "Costco"), r -> ids[0] = r);
        repository.addShoppingItem(new ShoppingItem("Eggs 12", 1, "pcs", null), r -> ids[1] = r);
        repository.setShoppingItemChecked(ids[0], true);

        final int[] removed = new int[1];
        repository.clearCompletedShoppingItems(r -> removed[0] = r);

        assertEquals(1, removed[0]);
        assertEquals(1, db.shoppingItemDao().getAllSync().size());
        assertEquals("eggs", db.shoppingItemDao().getAllSync().get(0).normalizedName);
    }

    @Test
    public void favoritesSaveQueryAndRemove() {
        repository.saveFavoriteRecipe(new FavoriteRecipe("r1", "Omelette", Arrays.asList("eggs", "milk")));
        assertEquals(1, db.favoriteRecipeDao().getAllSync().size());
        assertEquals(Arrays.asList("eggs", "milk"),
                repository.getFavoriteRecipesSync().get(0).ingredientTexts());

        repository.removeFavoriteRecipe("r1");
        assertEquals(0, db.favoriteRecipeDao().getAllSync().size());
    }

    @Test
    public void removeFavorite_withIngredients_deletesItsIngredientRows() {
        repository.saveFavoriteRecipe(new FavoriteRecipe("r1", "Omelette", Arrays.asList("eggs", "milk")));
        assertEquals(2, countRows("favorite_recipe_ingredients"));

        repository.removeFavoriteRecipe("r1");

        assertEquals(0, countRows("favorite_recipe_ingredients"));
    }

    @Test
    public void saveFavorite_sameRecipeAgain_replacesIngredientsWithoutDuplicates() {
        repository.saveFavoriteRecipe(new FavoriteRecipe("r1", "Omelette", Arrays.asList("eggs", "milk")));
        repository.saveFavoriteRecipe(new FavoriteRecipe("r1", "Omelette", Arrays.asList("eggs")));

        assertEquals(1, repository.getFavoriteRecipesSync().size());
        assertEquals(Arrays.asList("eggs"), repository.getFavoriteRecipesSync().get(0).ingredientTexts());
        assertEquals(1, countRows("favorite_recipe_ingredients"));
    }

    @Test
    public void addMealPlan_withIngredients_savesParsedRowsInOrder() {
        long day = 1_700_000_000_000L;
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Pancakes",
                Arrays.asList("200 g flour", "2 eggs", "soy sauce")), null);

        MealPlan saved = repository.getMealPlansSync(day, day).get(0);

        assertEquals(Arrays.asList("200 g flour", "2 eggs", "soy sauce"), saved.ingredientTexts());
        assertEquals("flour", saved.ingredients.get(0).normalizedName);
        assertEquals(200.0, saved.ingredients.get(0).quantity, 0.0001);
        assertEquals("g", saved.ingredients.get(0).unit);
        assertTrue(saved.ingredients.get(1).explicitQuantity);
        assertFalse(saved.ingredients.get(2).explicitQuantity);
    }

    @Test
    public void addMealPlan_withBlankIngredient_dropsIt() {
        long day = 1_700_000_000_000L;
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Soup",
                Arrays.asList("carrot", "  ", "")), null);

        assertEquals(1, countRows("meal_plan_ingredients"));
    }

    @Test
    public void updateMealPlan_newIngredients_replacesTheOldOnes() {
        long day = 1_700_000_000_000L;
        final long[] id = new long[1];
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Omelette",
                Arrays.asList("eggs", "milk")), r -> id[0] = r);

        MealPlan edited = new MealPlan(day, MealType.DINNER, "r1", "Omelette",
                Arrays.asList("eggs", "cheese", "ham"));
        edited.id = id[0];
        repository.updateMealPlan(edited);

        assertEquals(Arrays.asList("eggs", "cheese", "ham"),
                repository.getMealPlansSync(day, day).get(0).ingredientTexts());
        assertEquals(3, countRows("meal_plan_ingredients"));
    }

    @Test
    public void updateMealPlan_loadedFromDatabase_doesNotDuplicateRows() {
        long day = 1_700_000_000_000L;
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Omelette",
                Arrays.asList("eggs", "milk")), null);
        MealPlan loaded = repository.getMealPlansSync(day, day).get(0);

        loaded.completed = true;
        repository.updateMealPlan(loaded);

        assertEquals(2, countRows("meal_plan_ingredients"));
    }

    @Test
    public void deleteMealPlan_withIngredients_deletesItsIngredientRows() {
        long day = 1_700_000_000_000L;
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Omelette",
                Arrays.asList("eggs", "milk")), null);

        repository.deleteMealPlan(repository.getMealPlansSync(day, day).get(0));

        assertEquals(0, countRows("meal_plan_ingredients"));
    }

    @Test
    public void getMealPlans_observed_includesIngredients() {
        long day = 1_700_000_000_000L;
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Omelette",
                Arrays.asList("eggs", "milk")), null);

        List<MealPlan> plans = LiveDataTestUtil.getValue(repository.getMealPlans(day, day));

        assertEquals(Arrays.asList("eggs", "milk"), plans.get(0).ingredientTexts());
    }

    /** number of rows of a table, read straight from sqlite */
    private int countRows(String table) {
        try (android.database.Cursor cursor = db.getOpenHelper().getReadableDatabase()
                .query("SELECT COUNT(*) FROM " + table)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    @Test
    public void plannedIngredientsAreNormalizedDistinctAndSkipCompleted() {
        long day = 1_700_000_000_000L;
        final long[] ids = new long[2];
        repository.addMealPlan(new MealPlan(day, MealType.DINNER, "r1", "Omelette",
                Arrays.asList("2 Eggs", "Milk 1L")), r -> ids[0] = r);
        repository.addMealPlan(new MealPlan(day, MealType.LUNCH, "r2", "Pancakes",
                Arrays.asList("Milk", "Flour 500g")), r -> ids[1] = r);
        // a plan outside the requested range must be ignored
        repository.addMealPlan(new MealPlan(day + 10 * 86_400_000L, MealType.DINNER, "r3", "Soup",
                Arrays.asList("Carrot")), null);

        // lunch comes before dinner
        assertEquals("Pancakes", repository.getMealPlansSync(day, day).get(0).recipeTitle);

        List<String> planned = repository.getPlannedIngredientsSync(day, day);
        assertEquals(Arrays.asList("milk", "flour", "eggs"), planned);

        repository.setMealPlanCompleted(ids[1], true);
        assertEquals(Arrays.asList("eggs", "milk"), repository.getPlannedIngredientsSync(day, day));
    }
}