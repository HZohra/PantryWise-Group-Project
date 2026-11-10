package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe the user saved. {@link #recipeId} comes from the recipe source(ai engine or api)
 * and is unique, so a recipe can be saved only once.
 *
 * The ingredients are rows of the {@code favorite_recipe_ingredients} table and the cooking steps
 * are rows of the {@code favorite_recipe_steps} table.
 *
 * The optional details (image, source, servings, time, tip and nutrition) were added in database
 * version 4 so a saved recipe can be shown offline. they are null when the source did not give them.
 */
@Entity(tableName = "favorite_recipes", indices = {@Index(value = "recipeId", unique = true)})
public class FavoriteRecipe {
    /** prefix of {@link #recipeId} for recipes created by the ai engine, such as "ai:1b2c..." */
    public static final String AI_ID_PREFIX = "ai:";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String recipeId = "";

    @NonNull
    public String title = "";

    /**
     * ingredients of the recipe. they live in the {@code favorite_recipe_ingredients} table, so
     * room does not read this field: the repository fills it after loading the recipe.
     */
    @Ignore
    @NonNull
    public List<FavoriteRecipeIngredient> ingredients = new ArrayList<>();

    /**
     * cooking steps of the recipe, in order. they live in the {@code favorite_recipe_steps}
     * table, so room does not read this field: the repository fills it after loading the recipe.
     */
    @Ignore
    @NonNull
    public List<FavoriteRecipeStep> steps = new ArrayList<>();

    /** when the recipe was saved, in milliseconds */
    public long savedAt;

    /** picture of the recipe (edamam links expire after about an hour), null for ai recipes */
    @Nullable
    public String imageUrl;

    /** name of the website the recipe comes from, null for ai recipes */
    @Nullable
    public String sourceName;

    /** link to the original recipe, null for ai recipes */
    @Nullable
    public String sourceUrl;

    /** number of servings, null if unknown */
    @Nullable
    public Integer servings;

    /** total cooking time in minutes, null if unknown */
    @Nullable
    public Integer totalTimeMinutes;

    /** short cooking tip from the ai engine, null for other recipes */
    @Nullable
    public String tip;

    /** calories per serving, null if unknown */
    @Nullable
    public Double calories;

    /** protein in grams per serving, null if unknown */
    @Nullable
    public Double protein;

    /** carbohydrates in grams per serving, null if unknown */
    @Nullable
    public Double carbs;

    /** fat in grams per serving, null if unknown */
    @Nullable
    public Double fat;

    /** empty constructor required by room */
    public FavoriteRecipe() { }

    /** @param ingredientTexts the ingredients as typed, such as "eggs"; blank ones are dropped */
    @Ignore
    public FavoriteRecipe(@NonNull String recipeId, @NonNull String title,
                          @NonNull List<String> ingredientTexts) {
        this.recipeId = recipeId;
        this.title = title;
        this.savedAt = System.currentTimeMillis();
        int position = 0;
        for (String text : ingredientTexts) {
            if (text != null && !text.trim().isEmpty()) {
                this.ingredients.add(new FavoriteRecipeIngredient(text, position++));
            }
        }
    }

    /** @return the typed text of each ingredient, in order (what the edit form shows) */
    public List<String> ingredientTexts() {
        return IngredientLine.texts(ingredients);
    }

    /** @return true when the recipe was created by the ai engine */
    public boolean isAi() {
        return recipeId.startsWith(AI_ID_PREFIX);
    }

    /** @return the text of each cooking step, in order */
    public List<String> stepTexts() {
        List<String> result = new ArrayList<>(steps.size());
        for (FavoriteRecipeStep step : steps) {
            result.add(step.text);
        }
        return result;
    }
}