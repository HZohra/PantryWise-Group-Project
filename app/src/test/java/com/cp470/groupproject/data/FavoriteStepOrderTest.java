package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Review follow-up (Nora): saved cooking steps always come back in their position order. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class FavoriteStepOrderTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PantryDatabase.class).allowMainThreadQueries().build();
        repository = new PantryRepository(db, Runnable::run);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void stepsSavedOutOfOrder_comeBackInPositionOrder() {
        FavoriteRecipe recipe = new FavoriteRecipe("ai:order", "Soup", Arrays.asList("water", "salt"));
        recipe.steps.add(new FavoriteRecipeStep("Third", 2));
        recipe.steps.add(new FavoriteRecipeStep("First", 0));
        recipe.steps.add(new FavoriteRecipeStep("Second", 1));

        repository.saveFavoriteRecipe(recipe);

        FavoriteRecipe saved = repository.getFavoriteRecipeSync("ai:order");
        assertNotNull(saved);
        assertEquals(Arrays.asList("First", "Second", "Third"), saved.stepTexts());
    }

    @Test
    public void twelveSteps_keepNumericOrder_notTextOrder() {
        FavoriteRecipe recipe = new FavoriteRecipe("ai:long", "Stew", Arrays.asList("beef"));
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            expected.add("Step " + (i + 1));
        }
        for (int i = 11; i >= 0; i--) { // saved backwards, so position 10 and 11 come before 2..9
            recipe.steps.add(new FavoriteRecipeStep("Step " + (i + 1), i));
        }

        repository.saveFavoriteRecipe(recipe);

        assertEquals(expected, repository.getFavoriteRecipeSync("ai:long").stepTexts());
    }

    @Test
    public void stepsOfTwoRecipes_stayWithTheirOwnRecipe() {
        FavoriteRecipe soup = new FavoriteRecipe("ai:soup", "Soup", Arrays.asList("water"));
        soup.steps.add(new FavoriteRecipeStep("Boil water", 0));
        FavoriteRecipe salad = new FavoriteRecipe("ai:salad", "Salad", Arrays.asList("lettuce"));
        salad.steps.add(new FavoriteRecipeStep("Wash lettuce", 0));
        salad.steps.add(new FavoriteRecipeStep("Add dressing", 1));

        repository.saveFavoriteRecipe(soup);
        repository.saveFavoriteRecipe(salad);

        assertEquals(Arrays.asList("Boil water"), repository.getFavoriteRecipeSync("ai:soup").stepTexts());
        assertEquals(Arrays.asList("Wash lettuce", "Add dressing"),
                repository.getFavoriteRecipeSync("ai:salad").stepTexts());
    }
}