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
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.cp470.groupproject.data.repository.AiRepository;
import com.cp470.groupproject.data.repository.PantryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;

/**
 * The heart on the AI recipe screen: saves the recipe on screen with its steps, removes it, and
 * follows "Try another". No network: recipes are shown with showRecipeForTest().
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AiRecipeFavoriteTest {

    /** makes livedata and room run synchronously in tests */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository pantry;
    private AiRecipeViewModel viewModel;
    private final Observer<AiRecipeViewModel.FavoriteState> ignore = value -> { };

    @Before
    public void setUp() {
        Application app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, PantryDatabase.class).allowMainThreadQueries().build();
        pantry = new PantryRepository(db, Runnable::run);
        viewModel = new AiRecipeViewModel(app, new AiRepository(app), pantry);
        viewModel.getFavorite().observeForever(ignore); // the screen always observes it
    }

    @After
    public void tearDown() {
        viewModel.getFavorite().removeObserver(ignore);
        db.close();
    }

    private static AiRecipeResponse response(String recipeId, String title) {
        AiRecipeDto recipe = new AiRecipeDto();
        recipe.recipeId = recipeId;
        recipe.title = title;
        recipe.servings = 2;
        recipe.ingredientLines = Arrays.asList("2 eggs", "1 cup spinach");
        recipe.steps = Arrays.asList("Whisk the eggs", "Cook the spinach", "Add the eggs");
        recipe.tip = "Use low heat";
        AiRecipeResponse response = new AiRecipeResponse();
        response.recipe = recipe;
        return response;
    }

    @Test
    public void beforeAnyRecipe_thereIsNothingToSave() {
        assertFalse(viewModel.canFavorite());
        assertNull(viewModel.getFavorite().getValue());
        assertNull(viewModel.toggleFavorite());
    }

    @Test
    public void recipeOnScreen_startsNotSaved() {
        viewModel.showRecipeForTest(response("ai:1", "Spinach Scramble"));

        AiRecipeViewModel.FavoriteState state = viewModel.getFavorite().getValue();
        assertNotNull(state);
        assertEquals("ai:1", state.recipeId);
        assertFalse(state.saved);
    }

    @Test
    public void toggle_savesTheRecipeWithItsStepsAndTip() {
        viewModel.showRecipeForTest(response("ai:1", "Spinach Scramble"));

        assertEquals(Boolean.TRUE, viewModel.toggleFavorite());

        FavoriteRecipe saved = pantry.getFavoriteRecipeSync("ai:1");
        assertNotNull(saved);
        assertEquals("Spinach Scramble", saved.title);
        assertEquals(Arrays.asList("Whisk the eggs", "Cook the spinach", "Add the eggs"), saved.stepTexts());
        assertEquals("Use low heat", saved.tip);
        assertTrue(viewModel.getFavorite().getValue().saved);
    }

    @Test
    public void toggle_twice_removesTheRecipe() {
        viewModel.showRecipeForTest(response("ai:1", "Spinach Scramble"));

        viewModel.toggleFavorite();
        assertEquals(Boolean.FALSE, viewModel.toggleFavorite());

        assertNull(pantry.getFavoriteRecipeSync("ai:1"));
        assertFalse(viewModel.getFavorite().getValue().saved);
    }

    @Test
    public void tryAnother_newRecipeStartsNotSaved_andTheOldOneStaysSaved() {
        viewModel.showRecipeForTest(response("ai:1", "Spinach Scramble"));
        viewModel.toggleFavorite();

        viewModel.showRecipeForTest(response("ai:2", "Tomato Soup"));

        AiRecipeViewModel.FavoriteState state = viewModel.getFavorite().getValue();
        assertEquals("ai:2", state.recipeId);
        assertFalse(state.saved);
        assertNotNull(pantry.getFavoriteRecipeSync("ai:1"));
    }

    @Test
    public void tryAnother_thenSave_savesTheNewRecipeNotTheOldOne() {
        viewModel.showRecipeForTest(response("ai:1", "Spinach Scramble"));
        viewModel.showRecipeForTest(response("ai:2", "Tomato Soup"));

        viewModel.toggleFavorite();

        assertNull(pantry.getFavoriteRecipeSync("ai:1"));
        assertNotNull(pantry.getFavoriteRecipeSync("ai:2"));
    }

    @Test
    public void recipeWithoutId_cannotBeSaved() {
        viewModel.showRecipeForTest(response(null, "Mystery Dish"));

        assertFalse(viewModel.canFavorite());
        assertNull(viewModel.toggleFavorite());
        assertEquals(0, pantry.getFavoriteRecipesSync().size());
    }
}