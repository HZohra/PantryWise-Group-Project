package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/** Body of POST /api/v1/recipes/substitute */
public class SubstituteRequest {
    public String recipeTitle;
    /** The missing food, e.g. "egg" (not the whole line). */
    public String ingredient;
    public List<String> ingredientLines;
    public List<InventoryItemDto> inventory;
    /** App language tag, e.g. "es-MX": amounts and notes come back in that language. */
    @Nullable public String locale;
    @Nullable public PreferencesDto preferences;

    public SubstituteRequest(String recipeTitle, String ingredient, List<String> ingredientLines,
                             List<InventoryItemDto> inventory, @Nullable String locale,
                             @Nullable PreferencesDto preferences) {
        this.recipeTitle = recipeTitle;
        this.ingredient = ingredient;
        this.ingredientLines = ingredientLines;
        this.inventory = inventory;
        this.locale = locale;
        this.preferences = preferences;
    }
}