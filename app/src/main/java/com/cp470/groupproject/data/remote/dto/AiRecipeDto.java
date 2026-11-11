package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/** A recipe written by the AI and checked by the backend (structure, allergens). */
public class AiRecipeDto {
    /** "ai:<uuid>" (shared format with the favourites and meal-plan tables). */
    public String recipeId;
    public String title;
    @Nullable public Integer servings;
    @Nullable public Integer totalTimeMinutes;
    public List<AiRecipeIngredientDto> ingredients;
    /** "1 cup spinach": ready for the favourites / meal planner / shopping list formats. */
    public List<String> ingredientLines;
    /** Without numbers: the app numbers them. */
    public List<String> steps;
    /** One short tip, or "". */
    public String tip;
}