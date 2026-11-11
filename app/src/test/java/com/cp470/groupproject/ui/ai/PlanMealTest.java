package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.model.MealType;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.repository.AiRepository;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.data.repository.RecipeRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/** "Plan this meal" saves a meal plan with the recipe's ingredients, from both screens. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class PlanMealTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final long DAY = 1_700_000_000_000L;

    private Application app;
    private PantryDatabase db;
    private PantryRepository pantry;

    @Before
    public void setUp() {
        app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, PantryDatabase.class).allowMainThreadQueries().build();
        pantry = new PantryRepository(db, Runnable::run);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void detail_planMeal_savesThePlanWithIngredients() {
        RecipeDetailViewModel viewModel = new RecipeDetailViewModel(app, new RecipeRepository(app), pantry);
        RecipeDto recipe = new RecipeDto();
        recipe.recipeId = "edamam:abc";
        recipe.title = "Spinach Omelette";
        recipe.ingredientLines = Arrays.asList("2 eggs", "1 cup spinach");
        viewModel.setRecipeIfAbsent(recipe);
        final long[] id = new long[1];

        assertTrue(viewModel.planMeal(DAY, MealType.LUNCH, r -> id[0] = r));

        List<MealPlan> plans = pantry.getMealPlansSync(DAY, DAY);
        assertEquals(1, plans.size());
        assertEquals("edamam:abc", plans.get(0).recipeId);
        assertEquals("Spinach Omelette", plans.get(0).recipeTitle);
        assertEquals(MealType.LUNCH, plans.get(0).mealType);
        assertEquals(Arrays.asList("2 eggs", "1 cup spinach"), plans.get(0).ingredientTexts());
        assertTrue(id[0] > 0);
    }

    @Test
    public void detail_recipeWithoutId_isNotPlanned() {
        RecipeDetailViewModel viewModel = new RecipeDetailViewModel(app, new RecipeRepository(app), pantry);
        RecipeDto recipe = new RecipeDto();
        recipe.title = "No id";
        viewModel.setRecipeIfAbsent(recipe);

        assertFalse(viewModel.planMeal(DAY, MealType.DINNER, r -> { }));
        assertTrue(pantry.getMealPlansSync(DAY, DAY).isEmpty());
    }

    @Test
    public void ai_planMeal_savesTheRecipeOnScreen() {
        AiRecipeViewModel viewModel = new AiRecipeViewModel(app, new AiRepository(app), pantry);
        AiRecipeDto recipe = new AiRecipeDto();
        recipe.recipeId = "ai:1";
        recipe.title = "Spinach Scramble";
        recipe.ingredientLines = Arrays.asList("2 eggs", "1 cup spinach");
        AiRecipeResponse response = new AiRecipeResponse();
        response.recipe = recipe;
        viewModel.showRecipeForTest(response);

        assertTrue(viewModel.planMeal(DAY, MealType.DINNER, r -> { }));

        MealPlan plan = pantry.getMealPlansSync(DAY, DAY).get(0);
        assertEquals("ai:1", plan.recipeId);
        assertEquals("Spinach Scramble", plan.recipeTitle);
        assertEquals(Arrays.asList("2 eggs", "1 cup spinach"), plan.ingredientTexts());
    }

    @Test
    public void ai_noRecipeYet_isNotPlanned() {
        AiRecipeViewModel viewModel = new AiRecipeViewModel(app, new AiRepository(app), pantry);

        assertFalse(viewModel.planMeal(DAY, MealType.DINNER, r -> { }));
    }
}