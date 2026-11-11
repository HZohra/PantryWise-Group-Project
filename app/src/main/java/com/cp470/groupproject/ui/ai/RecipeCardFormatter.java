package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.ExpiringItemDto;
import com.cp470.groupproject.data.remote.dto.MatchDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns recipe data into the values a recipe card shows.
 * Plain Java (no Android), so every rule here is unit-tested.
 */
final class RecipeCardFormatter {

    private RecipeCardFormatter() { }

    /** True when the card should show "You have X of Y" (only recommend results with ingredients). */
    static boolean hasMatch(@Nullable MatchDto match) {
        return match != null && match.totalCount > 0;
    }

    /** "spinach, milk", or null when the recipe uses nothing that is expiring. */
    @Nullable
    static String expiringNames(@Nullable MatchDto match) {
        if (match == null || match.expiringUsed == null || match.expiringUsed.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (ExpiringItemDto item : match.expiringUsed) {
            if (item != null && item.name != null && !item.name.trim().isEmpty()) {
                names.add(item.name.trim());
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    /** Edamam sends minutes as a decimal and 0 when unknown: rounded, or null if unknown. */
    @Nullable
    static Integer minutes(@Nullable Double totalTimeMinutes) {
        if (totalTimeMinutes == null || totalTimeMinutes <= 0) {
            return null;
        }
        return (int) Math.round(totalTimeMinutes);
    }

    /** Joins the non-empty parts with " · ", e.g. "25 min · BBC Good Food"; null if there are none. */
    @Nullable
    static String joinMeta(@NonNull String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                kept.add(part.trim());
            }
        }
        return kept.isEmpty() ? null : String.join(" · ", kept);
    }
}