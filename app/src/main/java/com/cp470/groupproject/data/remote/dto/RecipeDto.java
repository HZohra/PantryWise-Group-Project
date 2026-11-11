package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * One recipe as the backend returns it (normalized from Edamam).
 * The image URL is signed and expires after about an hour. Favourites keep it anyway so it shows
 * right after saving; once it has expired the favourites screen shows a placeholder.
 */
public class RecipeDto {
    /** "edamam:<id>" (shared format with the favourites and meal-plan tables). */
    public String recipeId;
    public String title;
    @Nullable public String image;
    @Nullable public String sourceName;
    @Nullable public String sourceUrl;
    @Nullable public Double servings;
    /** null when Edamam doesn't know the time. */
    @Nullable public Double totalTimeMinutes;
    /** Calories of the WHOLE recipe; per-serving values are in {@link #nutrition}. */
    @Nullable public Double calories;
    /** Calories, protein, carbs and fat per serving. */
    @Nullable public NutritionDto nutrition;
    public List<String> ingredientLines;
    public List<String> ingredientNames;
    public List<String> dietLabels;
    public List<String> healthLabels;
    public List<String> cuisineType;
    /** Pantry match: present on /recipes/recommend, null on /recipes/search. */
    @Nullable public MatchDto match;
}