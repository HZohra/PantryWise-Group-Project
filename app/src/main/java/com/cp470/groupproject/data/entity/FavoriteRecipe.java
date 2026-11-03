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
 * The ingredient list is stored through a List<String> type converter.
 */
@Entity(tableName = "favorite_recipes", indices = {@Index(value = "recipeId", unique = true)})
public class FavoriteRecipe {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String recipeId = "";

    @NonNull
    public String title = "";

    /** names of the required ingredients */
    @NonNull
    public List<String> ingredients = new ArrayList<>();

    /** when the recipe was saved, in milliseconds */
    public long savedAt;

    /** empty constructor required by room */
    public FavoriteRecipe() { }

    @Ignore
    public FavoriteRecipe(@NonNull String recipeId, @NonNull String title,
                          @NonNull List<String> ingredients) {
        this.recipeId = recipeId;
        this.title = title;
        this.ingredients = ingredients;
        this.savedAt = System.currentTimeMillis();
    }
}