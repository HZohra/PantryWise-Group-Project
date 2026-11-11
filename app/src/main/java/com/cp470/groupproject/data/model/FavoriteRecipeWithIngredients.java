package com.cp470.groupproject.data.model;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Relation;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FavoriteRecipeIngredient;
import com.cp470.groupproject.data.entity.FavoriteRecipeStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A favorite recipe together with its ingredient rows and its cooking steps, as room returns it
 * from one query. the repository turns it back into a plain {@link FavoriteRecipe}.
 */
public class FavoriteRecipeWithIngredients {
    @Embedded
    public FavoriteRecipe recipe;

    @Relation(parentColumn = "id", entityColumn = "favoriteId")
    public List<FavoriteRecipeIngredient> rows = new ArrayList<>();

    @Relation(parentColumn = "id", entityColumn = "favoriteId")
    public List<FavoriteRecipeStep> stepRows = new ArrayList<>();

    /** @return the recipe with its ingredients and steps in the order they were saved */
    public FavoriteRecipe toRecipe() {
        List<FavoriteRecipeIngredient> sorted = new ArrayList<>(rows);
        // room does not promise any order for the children of a relation
        sorted.sort(Comparator.comparingInt(row -> row.position));
        recipe.ingredients = sorted;

        List<FavoriteRecipeStep> sortedSteps = new ArrayList<>(stepRows);
        sortedSteps.sort(Comparator.comparingInt(step -> step.position));
        recipe.steps = sortedSteps;
        return recipe;
    }

    /** @return the plain recipes of a list of rows with their ingredients */
    public static List<FavoriteRecipe> toRecipes(@NonNull List<FavoriteRecipeWithIngredients> list) {
        List<FavoriteRecipe> result = new ArrayList<>(list.size());
        for (FavoriteRecipeWithIngredients item : list) {
            result.add(item.toRecipe());
        }
        return result;
    }
}