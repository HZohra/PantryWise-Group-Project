package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FavoriteRecipeStep;
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
 * Favorite recipes with their cooking steps and optional details (database version 4). same setup
 * as {@link PantryRepositoryTest}: an in-memory database and a direct executor.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class FavoriteRecipeRepositoryTest {

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

    /** an ai recipe with ingredients and steps; each step gets its index as position */
    private static FavoriteRecipe aiRecipe(String recipeId, List<String> ingredients,
                                           List<String> steps) {
        FavoriteRecipe recipe = new FavoriteRecipe(recipeId, "Tomato soup", ingredients);
        for (int i = 0; i < steps.size(); i++) {
            recipe.steps.add(new FavoriteRecipeStep(steps.get(i), i));
        }
        return recipe;
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
    public void saveFavorite_withSteps_loadsThemInPositionOrder() {
        FavoriteRecipe recipe = new FavoriteRecipe("ai:1", "Tomato soup", Arrays.asList("tomatoes"));
        // added out of order on purpose: the position decides the order, not the insert order
        recipe.steps.add(new FavoriteRecipeStep("Serve hot", 2));
        recipe.steps.add(new FavoriteRecipeStep("Chop the tomatoes", 0));
        recipe.steps.add(new FavoriteRecipeStep("Simmer for 20 minutes", 1));

        repository.saveFavoriteRecipe(recipe);

        assertEquals(Arrays.asList("Chop the tomatoes", "Simmer for 20 minutes", "Serve hot"),
                repository.getFavoriteRecipesSync().get(0).stepTexts());
    }

    @Test
    public void saveFavorite_withDetails_keepsThem() {
        FavoriteRecipe recipe = new FavoriteRecipe("edamam:abc", "Omelette",
                Arrays.asList("2 eggs"));
        recipe.imageUrl = "https://example.com/omelette.jpg";
        recipe.sourceName = "Example Kitchen";
        recipe.sourceUrl = "https://example.com/omelette";
        recipe.servings = 2;
        recipe.totalTimeMinutes = 10;
        recipe.calories = 250.5;
        recipe.protein = 18.0;
        recipe.carbs = 2.0;
        recipe.fat = 19.0;

        repository.saveFavoriteRecipe(recipe);
        FavoriteRecipe loaded = repository.getFavoriteRecipesSync().get(0);

        assertEquals("https://example.com/omelette.jpg", loaded.imageUrl);
        assertEquals("Example Kitchen", loaded.sourceName);
        assertEquals("https://example.com/omelette", loaded.sourceUrl);
        assertEquals(Integer.valueOf(2), loaded.servings);
        assertEquals(Integer.valueOf(10), loaded.totalTimeMinutes);
        assertEquals(250.5, loaded.calories, 0.0001);
        assertEquals(18.0, loaded.protein, 0.0001);
        assertEquals(2.0, loaded.carbs, 0.0001);
        assertEquals(19.0, loaded.fat, 0.0001);
        assertNull(loaded.tip);
    }

    @Test
    public void saveFavorite_withoutDetails_leavesThemNull() {
        repository.saveFavoriteRecipe(new FavoriteRecipe("r1", "Omelette", Arrays.asList("eggs")));

        FavoriteRecipe loaded = repository.getFavoriteRecipesSync().get(0);

        assertNull(loaded.imageUrl);
        assertNull(loaded.servings);
        assertNull(loaded.calories);
        assertTrue(loaded.steps.isEmpty());
    }

    @Test
    public void saveFavorite_sameRecipeAgain_replacesStepsWithoutDuplicates() {
        repository.saveFavoriteRecipe(aiRecipe("ai:1", Arrays.asList("tomatoes"),
                Arrays.asList("Chop", "Simmer", "Serve")));
        repository.saveFavoriteRecipe(aiRecipe("ai:1", Arrays.asList("tomatoes"),
                Arrays.asList("Blend")));

        assertEquals(1, repository.getFavoriteRecipesSync().size());
        assertEquals(Arrays.asList("Blend"), repository.getFavoriteRecipesSync().get(0).stepTexts());
        assertEquals(1, countRows("favorite_recipe_steps"));
    }

    @Test
    public void saveFavorite_loadedFromDatabaseAndSavedAgain_doesNotDuplicateRows() {
        repository.saveFavoriteRecipe(aiRecipe("ai:1", Arrays.asList("tomatoes", "salt"),
                Arrays.asList("Chop", "Simmer")));
        FavoriteRecipe loaded = repository.getFavoriteRecipeSync("ai:1");
        assertNotNull(loaded);

        repository.saveFavoriteRecipe(loaded);

        assertEquals(1, countRows("favorite_recipes"));
        assertEquals(2, countRows("favorite_recipe_ingredients"));
        assertEquals(2, countRows("favorite_recipe_steps"));
    }

    @Test
    public void removeFavorite_withSteps_deletesItsStepRows() {
        repository.saveFavoriteRecipe(aiRecipe("ai:1", Arrays.asList("tomatoes"),
                Arrays.asList("Chop", "Simmer")));
        assertEquals(2, countRows("favorite_recipe_steps"));

        repository.removeFavoriteRecipe("ai:1");

        assertEquals(0, countRows("favorite_recipe_steps"));
        assertEquals(0, countRows("favorite_recipe_ingredients"));
    }

    @Test
    public void getFavoriteRecipeSync_returnsTheSavedRecipeOrNull() {
        repository.saveFavoriteRecipe(aiRecipe("ai:1", Arrays.asList("tomatoes", "salt"),
                Arrays.asList("Chop", "Simmer")));

        FavoriteRecipe found = repository.getFavoriteRecipeSync("ai:1");

        assertNotNull(found);
        assertEquals("Tomato soup", found.title);
        assertEquals(Arrays.asList("tomatoes", "salt"), found.ingredientTexts());
        assertEquals(Arrays.asList("Chop", "Simmer"), found.stepTexts());
        assertNull(repository.getFavoriteRecipeSync("ai:missing"));
    }

    @Test
    public void getFavoriteRecipes_observed_includesSteps() {
        repository.saveFavoriteRecipe(aiRecipe("ai:1", Arrays.asList("tomatoes"),
                Arrays.asList("Chop", "Simmer")));

        List<FavoriteRecipe> favorites = LiveDataTestUtil.getValue(repository.getFavoriteRecipes());

        assertEquals(Arrays.asList("Chop", "Simmer"), favorites.get(0).stepTexts());
    }

    @Test
    public void isAi_dependsOnTheRecipeIdPrefix() {
        assertTrue(new FavoriteRecipe("ai:1b2c", "Soup", Arrays.asList("water")).isAi());
        assertFalse(new FavoriteRecipe("edamam:abc", "Soup", Arrays.asList("water")).isAi());
        assertFalse(new FavoriteRecipe("r1", "Soup", Arrays.asList("water")).isAi());
    }
}