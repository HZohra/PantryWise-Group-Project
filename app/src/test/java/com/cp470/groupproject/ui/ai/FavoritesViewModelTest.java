package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
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

/** Favourites list: order, remove, and Undo bringing back the whole recipe. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class FavoritesViewModelTest {

    /** makes livedata and room run synchronously in tests */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository pantry;
    private FavoritesViewModel viewModel;
    private List<FavoriteRecipe> shown;
    private final Observer<List<FavoriteRecipe>> observer = list -> shown = list;

    @Before
    public void setUp() {
        Application app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, PantryDatabase.class).allowMainThreadQueries().build();
        pantry = new PantryRepository(db, Runnable::run);
        viewModel = new FavoritesViewModel(app, pantry);
        viewModel.getFavorites().observeForever(observer); // the screen always observes it
    }

    @After
    public void tearDown() {
        viewModel.getFavorites().removeObserver(observer);
        db.close();
    }

    private static FavoriteRecipe recipe(String id, String title, long savedAt) {
        FavoriteRecipe recipe = new FavoriteRecipe(id, title, Arrays.asList("2 eggs", "salt"));
        recipe.savedAt = savedAt;
        return recipe;
    }

    @Test
    public void nothingSaved_listIsEmpty() {
        assertTrue(shown.isEmpty());
    }

    @Test
    public void savedRecipes_areShownNewestFirst() {
        pantry.saveFavoriteRecipe(recipe("edamam:old", "Old", 100));
        pantry.saveFavoriteRecipe(recipe("ai:new", "New", 200));

        assertEquals(2, shown.size());
        assertEquals("New", shown.get(0).title);
        assertEquals("Old", shown.get(1).title);
    }

    @Test
    public void remove_takesTheRecipeOffTheList() {
        pantry.saveFavoriteRecipe(recipe("edamam:a", "Omelette", 100));

        viewModel.remove(shown.get(0));

        assertTrue(shown.isEmpty());
    }

    @Test
    public void undo_bringsBackIngredientsStepsAndPosition() {
        FavoriteRecipe soup = recipe("ai:soup", "Soup", 100);
        soup.steps.add(new FavoriteRecipeStep("Boil water", 0));
        soup.steps.add(new FavoriteRecipeStep("Add salt", 1));
        pantry.saveFavoriteRecipe(soup);
        pantry.saveFavoriteRecipe(recipe("edamam:new", "Newer", 200));

        FavoriteRecipe removed = shown.get(1); // the soup, as the list loaded it
        viewModel.remove(removed);
        assertEquals(1, shown.size());

        viewModel.restore(removed);

        assertEquals(2, shown.size());
        assertEquals("Soup", shown.get(1).title); // back in its old place (same save time)
        assertEquals(Arrays.asList("2 eggs", "salt"), shown.get(1).ingredientTexts());
        assertEquals(Arrays.asList("Boil water", "Add salt"), shown.get(1).stepTexts());
    }
}