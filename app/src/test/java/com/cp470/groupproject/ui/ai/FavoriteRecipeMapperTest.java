package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeIngredientDto;
import com.cp470.groupproject.data.remote.dto.NutritionDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Rules for turning Edamam and AI recipes into favourites. Plain JUnit, no Android. */
public class FavoriteRecipeMapperTest {

    private static RecipeDto edamam() {
        RecipeDto recipe = new RecipeDto();
        recipe.recipeId = "edamam:abc";
        recipe.title = "Spinach Omelette";
        recipe.ingredientLines = Arrays.asList("2 eggs", "1 cup spinach");
        return recipe;
    }

    private static AiRecipeDto ai() {
        AiRecipeDto recipe = new AiRecipeDto();
        recipe.recipeId = "ai:1b2c";
        recipe.title = "Spinach Scramble";
        recipe.ingredientLines = Arrays.asList("2 eggs", "1 cup spinach");
        recipe.steps = Arrays.asList("Whisk the eggs", "Cook the spinach", "Add the eggs");
        recipe.tip = "Use low heat";
        return recipe;
    }

    private static AiRecipeIngredientDto ingredient(String amount, String name) {
        AiRecipeIngredientDto item = new AiRecipeIngredientDto();
        item.amount = amount;
        item.name = name;
        return item;
    }

    // ---------------------------------------------------------------- edamam

    @Test
    public void fromEdamam_copiesIdTitleAndIngredientLines() {
        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(edamam());

        assertNotNull(favorite);
        assertEquals("edamam:abc", favorite.recipeId);
        assertEquals("Spinach Omelette", favorite.title);
        assertEquals(Arrays.asList("2 eggs", "1 cup spinach"), favorite.ingredientTexts());
        assertTrue(favorite.steps.isEmpty());
        assertFalse(favorite.isAi());
    }

    @Test
    public void fromEdamam_copiesImageAndSource_andRoundsServingsAndTime() {
        RecipeDto recipe = edamam();
        recipe.image = "https://img.example/omelette.jpg";
        recipe.sourceName = "BBC Good Food";
        recipe.sourceUrl = "https://example.com/omelette";
        recipe.servings = 4.0;
        recipe.totalTimeMinutes = 24.6;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertEquals("https://img.example/omelette.jpg", favorite.imageUrl);
        assertEquals("BBC Good Food", favorite.sourceName);
        assertEquals("https://example.com/omelette", favorite.sourceUrl);
        assertEquals(Integer.valueOf(4), favorite.servings);
        assertEquals(Integer.valueOf(25), favorite.totalTimeMinutes);
    }

    @Test
    public void fromEdamam_zeroServingsAndTime_meanUnknown() {
        RecipeDto recipe = edamam();
        recipe.servings = 0.0;
        recipe.totalTimeMinutes = 0.0;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertNull(favorite.servings);
        assertNull(favorite.totalTimeMinutes);
    }

    @Test
    public void fromEdamam_blankTexts_becomeNull() {
        RecipeDto recipe = edamam();
        recipe.image = "";
        recipe.sourceName = "   ";
        recipe.sourceUrl = null;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertNull(favorite.imageUrl);
        assertNull(favorite.sourceName);
        assertNull(favorite.sourceUrl);
    }

    @Test
    public void fromEdamam_nutritionPerServing_isKept() {
        RecipeDto recipe = edamam();
        recipe.nutrition = new NutritionDto();
        recipe.nutrition.perServing = true;
        recipe.nutrition.calories = 320;
        recipe.nutrition.protein = 21;
        recipe.nutrition.carbs = 4;
        recipe.nutrition.fat = 24;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertEquals(320.0, favorite.calories, 0.0001);
        assertEquals(21.0, favorite.protein, 0.0001);
        assertEquals(4.0, favorite.carbs, 0.0001);
        assertEquals(24.0, favorite.fat, 0.0001);
    }

    @Test
    public void fromEdamam_nutritionForTheWholeRecipe_isNotStored() {
        RecipeDto recipe = edamam();
        recipe.nutrition = new NutritionDto();
        recipe.nutrition.perServing = false;
        recipe.nutrition.calories = 1280;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertNull(favorite.calories);
        assertNull(favorite.protein);
    }

    @Test
    public void fromEdamam_partlyKnownNutrition_keepsOnlyTheKnownValues() {
        RecipeDto recipe = edamam();
        recipe.nutrition = new NutritionDto();
        recipe.nutrition.perServing = true;
        recipe.nutrition.calories = 300;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertEquals(300.0, favorite.calories, 0.0001);
        assertNull(favorite.fat);
    }

    @Test
    public void fromEdamam_withoutIdOrNull_returnsNull() {
        RecipeDto recipe = edamam();
        recipe.recipeId = "  ";

        assertNull(FavoriteRecipeMapper.fromEdamam(recipe));
        assertNull(FavoriteRecipeMapper.fromEdamam(null));
    }

    @Test
    public void fromEdamam_missingListsAndTitle_doNotCrash() {
        RecipeDto recipe = new RecipeDto();
        recipe.recipeId = "edamam:x";

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromEdamam(recipe);

        assertEquals("", favorite.title);
        assertTrue(favorite.ingredients.isEmpty());
    }

    // -------------------------------------------------------------------- ai

    @Test
    public void fromAi_copiesStepsInOrder_andDropsBlankSteps() {
        AiRecipeDto recipe = ai();
        recipe.steps = Arrays.asList("Whisk the eggs", "  ", "Cook the spinach", "", "Add the eggs");

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromAi(recipe);

        assertEquals(Arrays.asList("Whisk the eggs", "Cook the spinach", "Add the eggs"),
                favorite.stepTexts());
        assertEquals(0, favorite.steps.get(0).position);
        assertEquals(2, favorite.steps.get(2).position);
    }

    @Test
    public void fromAi_copiesIdTitleIngredientsTipAndIsAi() {
        AiRecipeDto recipe = ai();
        recipe.servings = 2;
        recipe.totalTimeMinutes = 15;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromAi(recipe);

        assertEquals("ai:1b2c", favorite.recipeId);
        assertEquals("Spinach Scramble", favorite.title);
        assertEquals(Arrays.asList("2 eggs", "1 cup spinach"), favorite.ingredientTexts());
        assertEquals("Use low heat", favorite.tip);
        assertEquals(Integer.valueOf(2), favorite.servings);
        assertEquals(Integer.valueOf(15), favorite.totalTimeMinutes);
        assertTrue(favorite.isAi());
        assertNull(favorite.imageUrl);
        assertNull(favorite.sourceUrl);
        assertNull(favorite.calories);
    }

    @Test
    public void fromAi_emptyTipAndZeroValues_becomeNull() {
        AiRecipeDto recipe = ai();
        recipe.tip = "";
        recipe.servings = 0;
        recipe.totalTimeMinutes = null;

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromAi(recipe);

        assertNull(favorite.tip);
        assertNull(favorite.servings);
        assertNull(favorite.totalTimeMinutes);
    }

    @Test
    public void fromAi_withoutIngredientLines_buildsThemFromAmountAndName() {
        AiRecipeDto recipe = ai();
        recipe.ingredientLines = Collections.emptyList();
        recipe.ingredients = Arrays.asList(ingredient("1 cup", "spinach"),
                ingredient("", "salt"), ingredient("2", " "), null);

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromAi(recipe);

        assertEquals(Arrays.asList("1 cup spinach", "salt"), favorite.ingredientTexts());
    }

    @Test
    public void fromAi_missingStepsAndLists_doNotCrash() {
        AiRecipeDto recipe = new AiRecipeDto();
        recipe.recipeId = "ai:x";

        FavoriteRecipe favorite = FavoriteRecipeMapper.fromAi(recipe);

        assertNotNull(favorite);
        assertTrue(favorite.steps.isEmpty());
        assertTrue(favorite.ingredients.isEmpty());
    }

    @Test
    public void fromAi_withoutIdOrNull_returnsNull() {
        AiRecipeDto recipe = ai();
        recipe.recipeId = null;

        assertNull(FavoriteRecipeMapper.fromAi(recipe));
        assertNull(FavoriteRecipeMapper.fromAi(null));
    }
}