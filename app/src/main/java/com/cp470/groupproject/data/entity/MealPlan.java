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
 * the ingredients are stored as a snapshot (rows of {@code meal_plan_ingredients}), so the plan keeps working even
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

    /**
     * ingredients of the meal (snapshot). they live in the {@code meal_plan_ingredients} table, so
     * room does not read this field: the repository fills it after loading the meal.
     */
    @Ignore
    @NonNull
    public List<MealPlanIngredient> ingredients = new ArrayList<>();

    /** true once the meal was cooked, its ingredients are no longer needed */
    public boolean completed;

    /** creation time in milliseconds */
    public long createdAt;

    /** empty constructor required by room */
    public MealPlan() { }

    /** @param ingredientTexts the ingredients as typed, such as "200 g flour"; blank ones are dropped */
    @Ignore
    public MealPlan(long planDate, @NonNull String mealType, @NonNull String recipeId,
                    @NonNull String recipeTitle, @NonNull List<String> ingredientTexts) {
        this.planDate = planDate;
        this.mealType = mealType;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.createdAt = System.currentTimeMillis();
        int position = 0;
        for (String text : ingredientTexts) {
            if (text != null && !text.trim().isEmpty()) {
                this.ingredients.add(new MealPlanIngredient(text, position++));
            }
        }
    }

    /** @return the typed text of each ingredient, in order (what the edit form shows) */
    public List<String> ingredientTexts() {
        return IngredientLine.texts(ingredients);
    }
}