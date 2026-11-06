package com.cp470.groupproject.data.model;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Relation;

import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.MealPlanIngredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A planned meal together with its ingredient rows, as room returns it from one query. the
 * repository turns it back into a plain {@link MealPlan}.
 */
public class MealPlanWithIngredients {
    @Embedded
    public MealPlan plan;

    @Relation(parentColumn = "id", entityColumn = "mealPlanId")
    public List<MealPlanIngredient> rows = new ArrayList<>();

    /** @return the meal with its ingredients in the order they were typed */
    public MealPlan toMealPlan() {
        List<MealPlanIngredient> sorted = new ArrayList<>(rows);
        // room does not promise any order for the children of a relation
        sorted.sort(Comparator.comparingInt(row -> row.position));
        plan.ingredients = sorted;
        return plan;
    }

    /** @return the plain meals of a list of rows with their ingredients */
    public static List<MealPlan> toMealPlans(@NonNull List<MealPlanWithIngredients> list) {
        List<MealPlan> result = new ArrayList<>(list.size());
        for (MealPlanWithIngredients item : list) {
            result.add(item.toMealPlan());
        }
        return result;
    }
}
