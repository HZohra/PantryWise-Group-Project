package com.cp470.groupproject.util;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Converts free-text product names to a canonical ingredient name, so that "2% Milk 1L" and "Milk"
 * can be matched reliably by the recipe engine and the shopping list.
 * plain java, so it can be unit tested without android.
 */

public final class IngredientNormalizer {

    /** quantities and sizes such as "2%", "1L", "500g" or "12" */
    private static final String SIZE_REGEX =
            "\\b\\d+([.,]\\d+)?\\s*(%|ml|kg|lbs|lb|oz|g|l)?(?![a-z0-9])";

    /** words that describe the product but do not change which ingredient it is */
    private static final Set<String> NOISE_WORDS = new HashSet<>(Arrays.asList(
            "organic", "fresh", "large", "small", "medium", "skim", "lowfat",
            "pack", "liter", "litre", "gallon"));

    private IngredientNormalizer() { }

    /**
     * @param rawName product name as typed or returned by a scan or api (may be null)
     * @return lower-case canonical name without sizes, digits, punctuation/noise words, never null
     */
    @NonNull
    public static String normalize(String rawName) {
        if (rawName == null) return "";
        String s = rawName.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll(SIZE_REGEX, " ");
        s = s.replaceAll("[^\\p{L}\\s]", " ");
        StringBuilder out = new StringBuilder();
        for (String word : s.split("\\s+")) {
            if (word.isEmpty() || NOISE_WORDS.contains(word)) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word);
        }
        return out.toString();
    }
}