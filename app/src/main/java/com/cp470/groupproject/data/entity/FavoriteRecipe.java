package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
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
 * The ingredients are rows of the {@code favorite_recipe_ingredients} table.
 */
@Entity(tableName = "favorite_recipes", indices = {@Index(value = "recipeId", unique = true)})
public class FavoriteRecipe {
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

    /** when the recipe was saved, in milliseconds */
    public long savedAt;

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
}