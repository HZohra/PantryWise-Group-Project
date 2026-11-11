package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One ingredient of a planned meal. the row is deleted together with its {@link MealPlan}
 * (cascade), so no ingredient is left without a meal.
 */
@Entity(tableName = "meal_plan_ingredients",
        foreignKeys = @ForeignKey(entity = MealPlan.class, parentColumns = "id",
                childColumns = "mealPlanId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("mealPlanId")})
public class MealPlanIngredient extends IngredientLine {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** id of the planned meal this ingredient belongs to */
    public long mealPlanId;

    /** empty constructor required by room */
    public MealPlanIngredient() { }

    /**
     * @param text the ingredient as typed, such as "2 eggs"
     * @param position order inside the meal
     */
    @Ignore
    public MealPlanIngredient(@NonNull String text, int position) {
        fill(text, position);
    }
}
