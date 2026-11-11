package com.cp470.groupproject.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Units of measure for ingredients: converts to a base unit (g, ml, pcs...) so quantities can be
 * added and compared. plain java, so it is tested on the jvm.
 */
public final class UnitUtils {

    /** a quantity expressed in a (base) unit */
    public static final class Quantity {
        /** the amount */
        public final double value;
        /** the unit the amount is expressed in */
        public final String unit;

        Quantity(double value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    /**
     * alias of a unit: the base unit it belongs to, how many base units it is worth and the name
     * the unit has in the meal dialog
     */
    private static final class Alias {
        final String base;
        final double factor;
        final String canonical;

        Alias(String base, double factor, String canonical) {
            this.base = base;
            this.factor = factor;
            this.canonical = canonical;
        }
    }

    private static final Map<String, Alias> ALIASES = new HashMap<>();

    static {
        add("pcs", 1, "", "pcs", "pc", "piece", "pieces", "unit", "units", "x", "each",
                "ud", "uds", "unidad", "unidades", "pieza", "piezas");
        add("g", 1, "g", "gr", "grs", "gram", "grams", "gramo", "gramos");
        add("g", 1000, "kg", "kgs", "kilo", "kilos", "kilogram", "kilograms", "kilogramo",
                "kilogramos");
        add("ml", 1, "ml", "mls", "milliliter", "milliliters", "mililitro", "mililitros");
        add("ml", 1000, "l", "lt", "lts", "liter", "liters", "litre", "litres", "litro",
                "litros");
        add("tsp", 1, "tsp", "teaspoon", "teaspoons", "cucharadita", "cucharaditas");
        add("tbsp", 1, "tbsp", "tablespoon", "tablespoons", "cucharada", "cucharadas");
        add("cup", 1, "cup", "cups", "taza", "tazas");
        add("oz", 1, "oz", "ounce", "ounces", "onza", "onzas");
        add("lb", 1, "lb", "lbs", "pound", "pounds", "libra", "libras");
    }

    /** the first name of the list is the canonical one; the empty text stands for "pcs" */
    private static void add(String base, double factor, String... names) {
        String canonical = names[0].isEmpty() ? "pcs" : names[0];
        for (String name : names) {
            ALIASES.put(name, new Alias(base, factor, canonical));
        }
    }

    private UnitUtils() { }

    private static String key(String rawUnit) {
        return rawUnit == null ? "" : rawUnit.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param raw text that may be a unit, in any letter case
     * @return true for a unit in the table. the empty text is not a unit for the parser
     */
    public static boolean isKnownUnit(String raw) {
        String key = key(raw);
        return !key.isEmpty() && ALIASES.containsKey(key);
    }

    /**
     * @param raw text that may be a unit, in any letter case
     * @return the main name of that unit ("kg" for "kilograms", "pcs" for "pieces"), or null if the
     *         text is not a known unit
     */
    public static String canonicalUnit(String raw) {
        Alias alias = ALIASES.get(key(raw));
        return alias == null ? null : alias.canonical;
    }

    /**
     * converts a quantity to its base unit.
     *
     * @param value the amount
     * @param rawUnit unit as written ("kg", "L", "tbsp", "" for pieces...)
     * @return the amount in the base unit; an unknown unit is kept as it is, in lower case
     */
    public static Quantity toBase(double value, String rawUnit) {
        String key = key(rawUnit);
        Alias alias = ALIASES.get(key);
        if (alias == null) {
            return new Quantity(value, key);
        }
        return new Quantity(value * alias.factor, alias.base);
    }

    /** @return true when both units measure the same thing (g and kg yes, g and ml no) */
    public static boolean sameDimension(String unitA, String unitB) {
        return toBase(1, unitA).unit.equals(toBase(1, unitB).unit);
    }

    /** @return the unit to show to the user: pieces have no unit text */
    public static String displayUnit(String baseUnit) {
        return "pcs".equals(baseUnit) ? "" : baseUnit;
    }

    /**
     * makes a base quantity easier to read: 1500 g becomes 1.5 kg and 2000 ml becomes 2 L.
     * the value is rounded to 2 decimals.
     *
     * @param value amount in the base unit
     * @param baseUnit the base unit
     * @return the amount to show
     */
    public static Quantity pretty(double value, String baseUnit) {
        if ("g".equals(baseUnit) && value >= 1000) {
            return new Quantity(round(value / 1000), "kg");
        }
        if ("ml".equals(baseUnit) && value >= 1000) {
            return new Quantity(round(value / 1000), "L");
        }
        return new Quantity(round(value), baseUnit);
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
