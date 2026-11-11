package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.MatchDto;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Allergen warnings for Edamam recipes. The backend lists the recipe's ingredients that match the
 * user's allergies in match.allergens; this class turns them into text and finds the line that
 * contains each one (same whole-word rules as IngredientMatcher). Plain Java, unit-tested.
 */
final class Allergens {

    private Allergens() { }

    static boolean has(@Nullable MatchDto match) {
        return match != null && match.allergens != null && !match.allergens.isEmpty();
    }

    /** "peanut butter, shrimp", or null when the recipe has no allergen. */
    @Nullable
    static String names(@Nullable MatchDto match) {
        if (!has(match)) return null;
        return String.join(", ", new LinkedHashSet<>(match.allergens));
    }

    /**
     * The allergen this ingredient line contains ("2 tbsp peanut butter" → "peanut butter"),
     * or null. "1 cup butter" does NOT contain "peanut butter": every word must appear.
     */
    @Nullable
    static String inLine(@Nullable String line, @Nullable List<String> allergens) {
        if (allergens == null || allergens.isEmpty()) return null;
        IngredientMatcher.Match match = IngredientMatcher.matchLine(line, allergens, null);
        return match.status == IngredientMatcher.Status.UNKNOWN ? null : match.name;
    }

    @NonNull
    static String join(@NonNull List<String> names) {
        return String.join(", ", new LinkedHashSet<>(names));
    }
}