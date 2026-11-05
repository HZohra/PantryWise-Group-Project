package com.cp470.groupproject.ui.mealplanner;

import com.cp470.groupproject.data.entity.MealPlan;

import java.util.Map;

/** The meals planned for one day of the week, as shown in one card of the planner. */
public final class DayPlan {

    /** 00:00 of the day, in milliseconds */
    public final long dayStartMs;

    /** planned meals by {@link com.cp470.groupproject.data.model.MealType} (only taken slots) */
    public final Map<String, MealPlan> meals;

    /**
     * @param dayStartMs start of the day
     * @param meals meals of the day, by meal type
     */
    public DayPlan(long dayStartMs, Map<String, MealPlan> meals) {
        this.dayStartMs = dayStartMs;
        this.meals = meals;
    }

    /** @return the meal in that slot, or null if it is empty */
    public MealPlan get(String mealType) {
        return meals.get(mealType);
    }
}
