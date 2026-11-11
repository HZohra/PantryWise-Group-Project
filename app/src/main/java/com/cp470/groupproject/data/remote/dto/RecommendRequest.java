package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/** Body of POST /api/v1/recipes/recommend */
public class RecommendRequest {

    public static final String MODE_STANDARD = "standard";
    public static final String MODE_ZERO_WASTE = "zeroWaste";

    /** Backend limit for "limit". */
    public static final int MAX_LIMIT = 10;

    public List<InventoryItemDto> inventory;
    /** Optional keyword; null = recommend from the pantry. */
    @Nullable public String query;
    @Nullable public PreferencesDto preferences;
    public int limit;
    public String mode;

    /** true = recipes with the user's allergens are removed; false = kept, flagged and listed last. */
    public boolean hideAllergens;

    public RecommendRequest(List<InventoryItemDto> inventory, @Nullable String query,
                            @Nullable PreferencesDto preferences, int limit, String mode) {
        this.inventory = inventory;
        this.query = query;
        this.preferences = preferences;
        this.limit = limit;
        this.mode = mode;
    }
}