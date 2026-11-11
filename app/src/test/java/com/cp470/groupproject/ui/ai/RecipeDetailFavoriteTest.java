package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.data.repository.RecipeRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;

/**
 * The heart on Recipe details: save, remove, and what happens without a recipe id.
 * In-memory database and a direct executor, like the repository tests.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class RecipeDetailFavoriteTest {

    /** makes livedata and room run synchronously in tests */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository pantry;
    private RecipeDetailViewModel viewModel;
    private final Observer<Boolean> ignore = value -> { };

    @Before
    public void setUp() {
        Application app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, PantryDatabase.class).allowMainThreadQueries().build();
        pantry = new PantryRepository(db, Runnable::run);
        viewModel = new RecipeDetailViewModel(app, new RecipeRepository(app), pantry);
    }

    @After
    public void tearDown() {
        if (viewModel.canFavorite()) {
            viewModel.getIsFavorite().removeObserver(ignore);
        }
        db.close();
    }

    private static RecipeDto omelette(String recipeId) {
        RecipeDto recipe = new RecipeDto();
        recipe.recipeId = recipeId;
        recipe.title = "Spinach Omelette";
        recipe.ingredientLines = Arrays.asList("2 eggs", "1 cup spinach");
        recipe.sourceUrl = "https://example.com/omelette";
        return recipe;
    }

    /** sets the recipe and starts observing the heart, as the screen does */
    private void open(RecipeDto recipe) {
        assertTrue(viewModel.setRecipeIfAbsent(recipe));
        if (viewModel.canFavorite()) {
            viewModel.getIsFavorite().observeForever(ignore);
        }
    }

    @Test
    public void notSavedYet_heartIsEmpty() {
        open(omelette("edamam:abc"));

        assertTrue(viewModel.canFavorite());
        assertEquals(Boolean.FALSE, viewModel.getIsFavorite().getValue());
    }

    @Test
    public void toggle_notSaved_savesTheRecipeAndFillsTheHeart() {
        open(omelette("edamam:abc"));

        assertEquals(Boolean.TRUE, viewModel.toggleFavorite());

        FavoriteRecipe saved = pantry.getFavoriteRecipeSync("edamam:abc");
        assertNotNull(saved);
        assertEquals("Spinach Omelette", saved.title);
        assertEquals(Arrays.asList("2 eggs", "1 cup spinach"), saved.ingredientTexts());
        assertEquals("https://example.com/omelette", saved.sourceUrl);
        assertEquals(Boolean.TRUE, viewModel.getIsFavorite().getValue());
    }

    @Test
    public void toggle_twice_removesTheRecipeAgain() {
        open(omelette("edamam:abc"));

        viewModel.toggleFavorite();
        assertEquals(Boolean.FALSE, viewModel.toggleFavorite());

        assertNull(pantry.getFavoriteRecipeSync("edamam:abc"));
        assertEquals(Boolean.FALSE, viewModel.getIsFavorite().getValue());
    }

    @Test
    public void alreadySavedBefore_heartStartsFilled() {
        pantry.saveFavoriteRecipe(FavoriteRecipeMapper.fromEdamam(omelette("edamam:abc")));

        open(omelette("edamam:abc"));

        assertEquals(Boolean.TRUE, viewModel.getIsFavorite().getValue());
    }

    @Test
    public void recipeWithoutId_cannotBeSaved() {
        open(omelette(null));

        assertFalse(viewModel.canFavorite());
        assertNull(viewModel.toggleFavorite());
        assertEquals(0, pantry.getFavoriteRecipesSync().size());
    }
}