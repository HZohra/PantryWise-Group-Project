package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.cp470.groupproject.data.model.MealType;

import java.util.ArrayList;
import java.util.List;

/**
 * a recipe planned for a given day and meal.
 *
 * {@link #recipeId} is the same id used by {@link FavoriteRecipe#recipeId}, and
 * the ingredient names are stored as a snapshot, so the plan keeps working even
 * if the recipe source changes. the shopping list can compare these ingredients
 * with the stock to know what is missing for the planned meals.
 */
@Entity(tableName = "meal_plans", indices = {@Index("planDate")})
public class MealPlan {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** day of the plan, in milliseconds at the start of that day */
    public long planDate;

    /** one of the {@link MealType} constants */
    @NonNull
    public String mealType = MealType.DINNER;

    /** id of the recipe in the recipe source */
    @NonNull
    public String recipeId = "";

    @NonNull
    public String recipeTitle = "";

    /** names of the required ingredients (snapshot) */
    @NonNull
    public List<String> ingredients = new ArrayList<>();

    /** true once the meal was cooked, its ingredients are no longer needed */
    public boolean completed;

    /** creation time in milliseconds */
    public long createdAt;

    /** empty constructor required by room */
    public MealPlan() { }

    @Ignore
    public MealPlan(long planDate, @NonNull String mealType, @NonNull String recipeId,
                    @NonNull String recipeTitle, @NonNull List<String> ingredients) {
        this.planDate = planDate;
        this.mealType = mealType;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.ingredients = ingredients;
        this.createdAt = System.currentTimeMillis();
    }
}