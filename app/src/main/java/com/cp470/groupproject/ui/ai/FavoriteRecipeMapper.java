package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FavoriteRecipeStep;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeIngredientDto;
import com.cp470.groupproject.data.remote.dto.NutritionDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the {@link FavoriteRecipe} that is saved when the user taps the heart, from an Edamam
 * recipe or from an AI recipe, and turns a saved Edamam favourite back into a {@link RecipeDto}
 * for Recipe details. Plain Java (no Android), so every rule here is unit-tested.
 *
 * Rules: blank text becomes null, 0 or negative servings and times mean "unknown" (null), and
 * nutrition is kept only when it is per serving (the favourites columns are per serving).
 */
final class FavoriteRecipeMapper {

    private FavoriteRecipeMapper() { }

    /**
     * @param recipe an Edamam recipe from /recipes/search or /recipes/recommend
     * @return the favourite to save, or null when the recipe has no id (it could not be found again)
     */
    @Nullable
    static FavoriteRecipe fromEdamam(@Nullable RecipeDto recipe) {
        if (recipe == null || isBlank(recipe.recipeId)) {
            return null;
        }
        FavoriteRecipe favorite = new FavoriteRecipe(recipe.recipeId.trim(), clean(recipe.title),
                orEmpty(recipe.ingredientLines));
        // the signed link expires after about an hour; the favourites screen shows a placeholder then
        favorite.imageUrl = blankToNull(recipe.image);
        favorite.sourceName = blankToNull(recipe.sourceName);
        favorite.sourceUrl = blankToNull(recipe.sourceUrl);
        favorite.servings = roundedPositive(recipe.servings);
        favorite.totalTimeMinutes = RecipeCardFormatter.minutes(recipe.totalTimeMinutes);

        NutritionDto nutrition = recipe.nutrition;
        if (nutrition != null && nutrition.perServing) {
            favorite.calories = toDouble(nutrition.calories);
            favorite.protein = toDouble(nutrition.protein);
            favorite.carbs = toDouble(nutrition.carbs);
            favorite.fat = toDouble(nutrition.fat);
        }
        return favorite;
    }

    /**
     * @param recipe a recipe written by the AI (/ai/recipe)
     * @return the favourite to save with its steps, or null when the recipe has no id
     */
    @Nullable
    static FavoriteRecipe fromAi(@Nullable AiRecipeDto recipe) {
        if (recipe == null || isBlank(recipe.recipeId)) {
            return null;
        }
        FavoriteRecipe favorite = new FavoriteRecipe(recipe.recipeId.trim(), clean(recipe.title),
                aiIngredientLines(recipe));
        favorite.servings = positive(recipe.servings);
        favorite.totalTimeMinutes = positive(recipe.totalTimeMinutes);
        favorite.tip = blankToNull(recipe.tip);

        int position = 0;
        for (String step : orEmpty(recipe.steps)) {
            if (!isBlank(step)) {
                favorite.steps.add(new FavoriteRecipeStep(step, position++));
            }
        }
        return favorite;
    }

    /**
     * A saved Edamam favourite as Recipe details expects it (offline, no backend call). There is
     * no pantry match, because it was not saved.
     */
    @NonNull
    static RecipeDto toRecipeDto(@NonNull FavoriteRecipe favorite) {
        RecipeDto recipe = new RecipeDto();
        recipe.recipeId = favorite.recipeId;
        recipe.title = favorite.title;
        recipe.image = favorite.imageUrl;
        recipe.sourceName = favorite.sourceName;
        recipe.sourceUrl = favorite.sourceUrl;
        recipe.servings = favorite.servings == null ? null : favorite.servings.doubleValue();
        recipe.totalTimeMinutes = favorite.totalTimeMinutes == null ? null
                : favorite.totalTimeMinutes.doubleValue();
        recipe.ingredientLines = new ArrayList<>(favorite.ingredientTexts());

        if (favorite.calories != null || favorite.protein != null
                || favorite.carbs != null || favorite.fat != null) {
            NutritionDto nutrition = new NutritionDto();
            nutrition.perServing = true; // only per-serving values are ever saved
            nutrition.calories = toInteger(favorite.calories);
            nutrition.protein = toInteger(favorite.protein);
            nutrition.carbs = toInteger(favorite.carbs);
            nutrition.fat = toInteger(favorite.fat);
            recipe.nutrition = nutrition;
        }
        return recipe;
    }

    /**
     * the backend's ready-made lines ("1 cup spinach"); if they are missing, the lines are built
     * from the amount and the name of each ingredient.
     */
    @NonNull
    static List<String> aiIngredientLines(@NonNull AiRecipeDto recipe) {
        if (recipe.ingredientLines != null && !recipe.ingredientLines.isEmpty()) {
            return recipe.ingredientLines;
        }
        List<String> lines = new ArrayList<>();
        if (recipe.ingredients != null) {
            for (AiRecipeIngredientDto ingredient : recipe.ingredients) {
                if (ingredient == null || isBlank(ingredient.name)) {
                    continue;
                }
                String name = ingredient.name.trim();
                lines.add(isBlank(ingredient.amount) ? name : ingredient.amount.trim() + " " + name);
            }
        }
        return lines;
    }

    private static boolean isBlank(@Nullable String text) {
        return text == null || text.trim().isEmpty();
    }

    @Nullable
    private static String blankToNull(@Nullable String text) {
        return isBlank(text) ? null : text.trim();
    }

    @NonNull
    private static String clean(@Nullable String text) {
        return text == null ? "" : text.trim();
    }

    @NonNull
    private static List<String> orEmpty(@Nullable List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }

    /** Edamam sends servings as a decimal: rounded, or null when unknown (0 or less). */
    @Nullable
    private static Integer roundedPositive(@Nullable Double value) {
        if (value == null || value <= 0) {
            return null;
        }
        return (int) Math.round(value);
    }

    @Nullable
    private static Integer positive(@Nullable Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    @Nullable
    private static Double toDouble(@Nullable Integer value) {
        return value == null ? null : value.doubleValue();
    }

    @Nullable
    private static Integer toInteger(@Nullable Double value) {
        return value == null ? null : (int) Math.round(value);
    }
}