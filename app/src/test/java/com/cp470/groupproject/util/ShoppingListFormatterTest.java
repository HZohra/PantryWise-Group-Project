package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import com.cp470.groupproject.data.entity.ShoppingItem;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Tests the plain text written when the shopping list is shared. */
public class ShoppingListFormatterTest {

    private static ShoppingItem item(String name, double qty, String unit, String store,
                                     boolean checked) {
        ShoppingItem item = new ShoppingItem(name, qty, unit, store);
        item.checked = checked;
        return item;
    }

    @Test
    public void format_emptyList_onlyHeader() {
        assertEquals("Shopping list",
                ShoppingListFormatter.format("Shopping list", Collections.emptyList()));
    }

    @Test
    public void format_uncheckedAndChecked() {
        String text = ShoppingListFormatter.format("H", Arrays.asList(
                item("Eggs", 1, "", null, false), item("Milk", 1, "", null, true)));

        assertEquals("H\n[ ] Eggs\n[x] Milk", text);
    }

    @Test
    public void format_quantityWithoutUnit() {
        String text = ShoppingListFormatter.format("H", Arrays.asList(
                item("Eggs", 2, "", null, false), item("Milk", 1, "", null, false)));

        assertEquals("H\n[ ] 2 x Eggs\n[ ] Milk", text);
    }

    @Test
    public void format_quantityWithUnit() {
        String text = ShoppingListFormatter.format("H",
                Collections.singletonList(item("Flour", 500, "g", null, false)));

        assertEquals("H\n[ ] 500 g Flour", text);
    }

    @Test
    public void format_decimalQuantity() {
        String text = ShoppingListFormatter.format("H", Arrays.asList(
                item("Milk", 1.5, "L", null, false), item("Rice", 2.0, "kg", null, false)));

        assertEquals("H\n[ ] 1.5 L Milk\n[ ] 2 kg Rice", text);
    }

    @Test
    public void format_withStore() {
        String text = ShoppingListFormatter.format("H", Arrays.asList(
                item("Flour", 500, "g", "Walmart", false),
                item("Milk", 1, "", "", false),
                item("Eggs", 1, "", null, false)));

        assertEquals("H\n[ ] 500 g Flour (Walmart)\n[ ] Milk\n[ ] Eggs", text);
    }

    @Test
    public void format_keepsGivenOrder() {
        String text = ShoppingListFormatter.format("H", Arrays.asList(
                item("Zucchini", 1, "", null, false), item("Apple", 1, "", null, false)));

        assertEquals("H\n[ ] Zucchini\n[ ] Apple", text);
    }
}
