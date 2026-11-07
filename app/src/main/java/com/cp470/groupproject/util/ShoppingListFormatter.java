package com.cp470.groupproject.util;

import com.cp470.groupproject.data.entity.ShoppingItem;

import java.util.List;

/**
 * Builds the plain text version of the shopping list that is sent to other apps.
 * plain java, so it is tested on the jvm.
 */
public final class ShoppingListFormatter {

    private ShoppingListFormatter() { }

    /**
     * one header line followed by one line per item, in the given order, such as
     * {@code [ ] 500 g Flour (Walmart)}.
     *
     * @param header first line of the text
     * @param items items to write
     * @return the text, lines separated by new lines
     */
    public static String format(String header, List<ShoppingItem> items) {
        StringBuilder text = new StringBuilder(header);
        for (ShoppingItem item : items) {
            text.append('\n').append(item.checked ? "[x] " : "[ ] ");
            text.append(quantityPrefix(item)).append(item.name);
            if (item.store != null && !item.store.trim().isEmpty()) {
                text.append(" (").append(item.store.trim()).append(')');
            }
        }
        return text.toString();
    }

    /**
     * quantity and unit written before the name: "500 g ", "2 x " or nothing for a single unit.
     *
     * @param item the item to describe
     * @return the prefix, possibly empty
     */
    private static String quantityPrefix(ShoppingItem item) {
        String quantity = formatQuantity(item.quantity);
        if (item.unit.trim().isEmpty()) {
            return item.quantity == 1 ? "" : quantity + " x ";
        }
        return quantity + " " + item.unit.trim() + " ";
    }

    /**
     * writes a quantity without a useless decimal part.
     *
     * @param quantity the number to write
     * @return "2" for 2.0 and "1.5" for 1.5
     */
    public static String formatQuantity(double quantity) {
        if (quantity == Math.rint(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }
}
