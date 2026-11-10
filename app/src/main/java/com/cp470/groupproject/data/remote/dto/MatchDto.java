package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/**
 * How well a recipe fits the user's pantry (only on /recipes/recommend results).
 * Staples like salt, pepper and water are never counted.
 */
public class MatchDto {
    /** Higher = better; already used by the backend to sort the list. */
    public int score;
    /** Ingredients the user has … */
    public int haveCount;
    /** … out of this many needed ("You have haveCount of totalCount"). */
    public int totalCount;
    public List<String> have;
    public List<String> missing;
    /** Expiring pantry items this recipe uses, most urgent first ("Uses expiring" badge). */
    public List<ExpiringItemDto> expiringUsed;
}