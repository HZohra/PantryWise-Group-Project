package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

/** Calories and macros from Edamam, rounded. Per serving when perServing is true, else for the whole recipe. */
public class NutritionDto {
    /** false when Edamam didn't know the number of servings. */
    public boolean perServing;
    @Nullable public Integer calories;
    /** Grams. */
    @Nullable public Integer protein;
    /** Grams. */
    @Nullable public Integer carbs;
    /** Grams. */
    @Nullable public Integer fat;
}