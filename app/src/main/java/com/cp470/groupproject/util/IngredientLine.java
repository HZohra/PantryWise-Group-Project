package com.cp470.groupproject.util;

import java.util.Locale;
import java.util.regex.Matcher;

/**
 * One row of the ingredient form in the meal dialog (name, quantity and unit) and its saved text,
 * such as "100 g beef" or "2 red peppers". the form builds the text and reads it back when a meal
 * is edited, so the stored format stays the one {@link IngredientParser} understands.
 * plain java, so it is tested on the jvm.
 */
public final class IngredientLine {

    /** units offered by the form, in the same order as the labels shown to the user */
    public static final String[] UNITS = {
            "pcs", "g", "kg", "ml", "L", "tsp", "tbsp", "cup", "oz", "lb"
    };

    /** what is wrong with a row, if anything */
    public enum Error { NONE, NAME_REQUIRED, QUANTITY_INVALID }

    /** outcome of building the text of a row */
    public static final class Result {
        /** what is wrong with the row, or NONE */
        public final Error error;
        /** text to save; empty for a row left blank (only meaningful when there is no error) */
        public final String text;

        Result(Error error, String text) {
            this.error = error;
            this.text = text;
        }

        /** @return true when the row can be saved */
        public boolean isValid() {
            return error == Error.NONE;
        }
    }

    /** the three fields of a row, as they are shown in the form */
    public static final class Parts {
        /** ingredient name as written */
        public final String name;
        /** quantity text, empty when the ingredient has no amount */
        public final String quantity;
        /** one of {@link #UNITS} */
        public final String unit;

        Parts(String name, String quantity, String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    private IngredientLine() { }

    /**
     * builds the text of a row. a blank row gives an empty text, a row without quantity gives just
     * the name (the shopping list then only checks that it is in stock), and the quantity accepts
     * both "1,5" and "1.5".
     *
     * @param name text of the name field
     * @param quantityText text of the quantity field
     * @param unit one of {@link #UNITS}; null or "pcs" means pieces
     * @return the result, never null
     */
    public static Result compose(String name, String quantityText, String unit) {
        String cleanName = name == null ? "" : name.trim();
        String qty = quantityText == null ? "" : quantityText.trim();
        if (cleanName.isEmpty() && qty.isEmpty()) {
            return new Result(Error.NONE, "");
        }
        if (cleanName.isEmpty()) {
            return new Result(Error.NAME_REQUIRED, "");
        }
        if (qty.isEmpty()) {
            return new Result(Error.NONE, cleanName);
        }
        double value;
        try {
            value = Double.parseDouble(qty.replace(',', '.'));
        } catch (NumberFormatException e) {
            return new Result(Error.QUANTITY_INVALID, "");
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            return new Result(Error.QUANTITY_INVALID, "");
        }
        String text = ShoppingListFormatter.formatQuantity(value) + " "
                + unitToken(unit, cleanName) + cleanName;
        return new Result(Error.NONE, text);
    }

    /**
     * @return the unit followed by a space, or nothing for pieces. pieces are written with "pcs"
     *         only when the name starts with a unit word ("2 pcs cup noodles"), so it is not read
     *         as "2 cups of noodles".
     */
    private static String unitToken(String unit, String name) {
        boolean pieces = unit == null || unit.isEmpty() || "pcs".equalsIgnoreCase(unit);
        if (!pieces) {
            return unit + " ";
        }
        String firstWord = name.split("\\s+")[0];
        return UnitUtils.isKnownUnit(firstWord) ? "pcs " : "";
    }

    /**
     * splits a saved ingredient text into the fields of a row, without converting units
     * ("1.5 kg rice" stays 1.5 kg). text without a number is a name only.
     *
     * @param text saved ingredient, such as "200 g flour", "2 eggs" or "soy sauce"
     * @return the fields of the row, never null
     */
    public static Parts split(String text) {
        String clean = text == null ? "" : text.trim();
        Matcher matcher = IngredientParser.QUANTITY.matcher(clean);
        if (!matcher.matches()) {
            return new Parts(clean, "", UNITS[0]);
        }
        double amount = IngredientParser.toNumber(matcher.group(1));
        if (Double.isNaN(amount)) {
            return new Parts(clean, "", UNITS[0]);
        }
        String word = matcher.group(2);
        String rest = matcher.group(3);
        String unit = UNITS[0];
        String name;
        if (word != null && UnitUtils.isKnownUnit(word)) {
            unit = formUnit(UnitUtils.canonicalUnit(word));
            name = IngredientParser.stripLinkWord(rest);
        } else {
            name = (word == null ? "" : word + " ") + rest;
        }
        return new Parts(name.trim(), ShoppingListFormatter.formatQuantity(amount), unit);
    }

    /** @return the position of a unit in {@link #UNITS} (letter case ignored), 0 if it is not there */
    public static int unitIndex(String unit) {
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equalsIgnoreCase(unit)) {
                return i;
            }
        }
        return 0;
    }

    private static String formUnit(String canonical) {
        return UNITS[unitIndex(canonical == null ? "" : canonical.toLowerCase(Locale.ROOT))];
    }
}
