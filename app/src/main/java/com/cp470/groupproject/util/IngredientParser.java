package com.cp470.groupproject.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads one line of a recipe ingredient list, such as "200 g flour" or "soy sauce", into a name, a
 * quantity and a unit. plain java, so it is tested on the jvm.
 */
public final class IngredientParser {

    /** a number or fraction, an optional word after it, and the rest of the text */
    static final Pattern QUANTITY = Pattern.compile(
            "^\\s*(\\d+\\s*/\\s*\\d+|\\d+(?:[.,]\\d+)?)\\s*(\\p{L}+)?\\s*(.*)$");

    /** "of" or "de" between a unit and the name: "100 grams of chicken" */
    private static final Pattern LINK_WORD = Pattern.compile(
            "^(?:of|de)\\s+", Pattern.CASE_INSENSITIVE);

    /** result of parsing one ingredient line */
    public static final class Parsed {
        /** canonical name, e.g. "flour" */
        public final String normalizedName;
        /** name to show, e.g. "Flour" */
        public final String displayName;
        /** amount in the base unit (1 when the text had no amount) */
        public final double quantity;
        /** base unit: "g", "pcs", "tbsp"... */
        public final String unit;
        /** false for ingredients without an amount ("soy sauce"): only "have it or not" matters */
        public final boolean explicitQuantity;

        Parsed(String normalizedName, String displayName, double quantity, String unit,
               boolean explicitQuantity) {
            this.normalizedName = normalizedName;
            this.displayName = displayName;
            this.quantity = quantity;
            this.unit = unit;
            this.explicitQuantity = explicitQuantity;
        }
    }

    private IngredientParser() { }

    /**
     * @param raw one ingredient line, may be null
     * @return the parsed ingredient, or null if the text has no usable name
     */
    public static Parsed parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = QUANTITY.matcher(raw);
        if (!matcher.matches()) {
            return withoutQuantity(raw);
        }
        double amount = toNumber(matcher.group(1));
        if (Double.isNaN(amount)) {
            return withoutQuantity(raw);
        }

        String word = matcher.group(2);
        String rest = matcher.group(3);
        String unit;
        String name;
        if (word != null && UnitUtils.isKnownUnit(word)) {
            unit = word;
            name = stripLinkWord(rest);
        } else {
            // "2 eggs": the word after the number is part of the name, the unit is "pieces"
            unit = "";
            name = (word == null ? "" : word + " ") + rest;
        }

        String normalized = IngredientNormalizer.normalize(name);
        if (normalized.isEmpty()) {
            return null;
        }
        UnitUtils.Quantity base = UnitUtils.toBase(amount, unit);
        return new Parsed(normalized, capitalize(normalized), base.value, base.unit, true);
    }

    private static Parsed withoutQuantity(String raw) {
        String normalized = IngredientNormalizer.normalize(raw);
        if (normalized.isEmpty()) {
            return null;
        }
        return new Parsed(normalized, capitalize(normalized), 1, "pcs", false);
    }

    /** @return the text without a leading "of" or "de" ("of chicken" becomes "chicken") */
    static String stripLinkWord(String text) {
        return LINK_WORD.matcher(text.trim()).replaceFirst("");
    }

    /** @return the value of "1,5", "2" or "1/2", or NaN for a fraction with a zero denominator */
    static double toNumber(String text) {
        if (text.contains("/")) {
            String[] parts = text.split("/");
            double top = Double.parseDouble(parts[0].trim());
            double bottom = Double.parseDouble(parts[1].trim());
            return bottom == 0 ? Double.NaN : top / bottom;
        }
        return Double.parseDouble(text.replace(',', '.'));
    }

    private static String capitalize(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
