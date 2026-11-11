package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/** Body of POST /api/v1/ai/recipe */
public class AiRecipeRequest {

    /** Backend LIMITS.servingsMax. */
    public static final int MAX_SERVINGS = 12;

    public List<InventoryItemDto> inventory;
    /** App language tag, e.g. "es-MX": the recipe text comes back in that language. */
    @Nullable public String locale;
    @Nullable public PreferencesDto preferences;
    public int servings;

    public AiRecipeRequest(List<InventoryItemDto> inventory, @Nullable String locale,
                           @Nullable PreferencesDto preferences, int servings) {
        this.inventory = inventory;
        this.locale = locale;
        this.preferences = preferences;
        this.servings = servings;
    }
}