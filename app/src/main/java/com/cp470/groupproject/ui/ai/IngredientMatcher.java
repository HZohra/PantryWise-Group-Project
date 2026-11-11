package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a recipe's ingredient line ("2 cups chopped onions") is in the pantry or missing,
 * using the food names the backend already matched (match.have / match.missing).
 * Plain Java, no Android: the same normalizing rules as the backend's recommendationService
 * (lowercase, letters only, simple singular, whole words), so "onions" matches "onion"
 * but "peanuts" never matches "pea".
 */
public final class IngredientMatcher {

    public enum Status { IN_PANTRY, MISSING, UNKNOWN }

    /** The result for one line. {@link #name} is the matched food (e.g. "egg"), null when UNKNOWN. */
    public static final class Match {
        @NonNull public final Status status;
        @Nullable public final String name;

        Match(@NonNull Status status, @Nullable String name) {
            this.status = status;
            this.name = name;
        }
    }

    private static final Match NO_MATCH = new Match(Status.UNKNOWN, null);

    private IngredientMatcher() { }

    /**
     * Matches one line against the backend's names. The longest matching name wins,
     * so "peanut butter" beats "butter". Staples like salt usually give UNKNOWN.
     */
    @NonNull
    public static Match matchLine(@Nullable String line,
                                  @Nullable List<String> have,
                                  @Nullable List<String> missing) {
        if (line == null) return NO_MATCH;
        Set<String> lineWords = words(line);
        if (lineWords.isEmpty()) return NO_MATCH;

        String bestName = null;
        Status bestStatus = Status.UNKNOWN;
        int bestLength = 0;

        if (have != null) {
            for (String name : have) {
                int length = matchLength(lineWords, name);
                if (length > bestLength) {
                    bestLength = length;
                    bestName = name;
                    bestStatus = Status.IN_PANTRY;
                }
            }
        }
        if (missing != null) {
            for (String name : missing) {
                int length = matchLength(lineWords, name);
                if (length > bestLength) {
                    bestLength = length;
                    bestName = name;
                    bestStatus = Status.MISSING;
                }
            }
        }
        return bestName == null ? NO_MATCH : new Match(bestStatus, bestName);
    }

    /** Number of words in name if ALL of them appear in the line, else 0. */
    private static int matchLength(@NonNull Set<String> lineWords, @Nullable String name) {
        if (name == null) return 0;
        String normalized = normalize(name);
        if (normalized.isEmpty()) return 0;
        String[] nameWords = normalized.split(" ");
        for (String word : nameWords) {
            if (!lineWords.contains(word)) return 0;
        }
        return nameWords.length;
    }

    @NonNull
    private static Set<String> words(@NonNull String text) {
        String normalized = normalize(text);
        return normalized.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(normalized.split(" ")));
    }

    /** "2% Whole Milk!" → "whole milk", "Tomatoes" → "tomato" (same rules as the backend). */
    @NonNull
    static String normalize(@NonNull String text) {
        String lettersOnly = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z\\s]", " ");
        StringBuilder result = new StringBuilder();
        for (String word : lettersOnly.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(singularize(word));
        }
        return result.toString();
    }

    /** "eggs" → "egg", "berries" → "berry", "tomatoes" → "tomato"; "glass", "hummus" stay. */
    @NonNull
    static String singularize(@NonNull String word) {
        if (word.length() <= 3 || word.endsWith("ss") || word.endsWith("us") || word.endsWith("is")) {
            return word;
        }
        if (word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y";
        if (word.endsWith("sses") || word.endsWith("ches") || word.endsWith("shes")
                || word.endsWith("xes") || word.endsWith("oes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s")) return word.substring(0, word.length() - 1);
        return word;
    }
}