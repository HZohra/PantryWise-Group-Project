package com.cp470.groupproject.data.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * What the food profile screen requires before it can be saved: a name, and an answer about
 * allergies (at least one, or "I have no food allergies"). Plain Java, unit-tested.
 */
public final class FoodProfileRules {

    public static final int MAX_NAME_LENGTH = 40;

    /** Letters in any alphabet, spaces, dots, apostrophes and hyphens ("Alex", "Mary-Jane", "Ö. Yıldız"). */
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L}\\s.'-]*$");

    /** The first problem that stops saving, or NONE. */
    public enum Problem { NONE, NAME_MISSING, NAME_INVALID, ALLERGIES_UNANSWERED }

    private FoodProfileRules() { }

    /** @return the name trimmed with single spaces, or null when it is empty, too long or has digits/symbols */
    @Nullable
    public static String cleanName(@Nullable String raw) {
        if (raw == null) return null;
        String clean = raw.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty() || clean.length() > MAX_NAME_LENGTH) return null;
        return NAME_PATTERN.matcher(clean).matches() ? clean : null;
    }

    @NonNull
    public static Problem check(@Nullable String rawName, @NonNull List<String> allergies, boolean noAllergies) {
        if (rawName == null || rawName.trim().isEmpty()) return Problem.NAME_MISSING;
        if (cleanName(rawName) == null) return Problem.NAME_INVALID;
        if (allergies.isEmpty() && !noAllergies) return Problem.ALLERGIES_UNANSWERED;
        return Problem.NONE;
    }
}