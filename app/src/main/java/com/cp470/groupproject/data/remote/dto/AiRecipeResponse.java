package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/** Response of POST /api/v1/ai/recipe */
public class AiRecipeResponse {
    public AiRecipeDto recipe;
    /** false = nothing was expiring, so the recipe was built from the whole pantry (UC-2 1a). */
    public boolean usedExpiring;
    /** The expiring pantry items the recipe was built around, most urgent first. */
    public List<String> expiringItems;
}