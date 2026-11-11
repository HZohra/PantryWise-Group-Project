package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests how the rows of the ingredient form become saved text and back. */
public class IngredientLineTest {

    private static String text(String name, String qty, String unit) {
        IngredientLine.Result result = IngredientLine.compose(name, qty, unit);
        assertTrue(name + " " + qty + " " + unit, result.isValid());
        return result.text;
    }

    private static void checkParts(String saved, String name, String qty, String unit) {
        IngredientLine.Parts parts = IngredientLine.split(saved);
        assertEquals(saved, name, parts.name);
        assertEquals(saved, qty, parts.quantity);
        assertEquals(saved, unit, parts.unit);
    }

    // ---- compose

    @Test
    public void compose_blankRow_givesEmptyText() {
        assertEquals("", text("", "", "g"));
        assertEquals("", text("   ", " ", null));
    }

    @Test
    public void compose_nameWithoutQuantity_isPresenceOnly() {
        assertEquals("soy sauce", text("soy sauce", "", "g"));
    }

    @Test
    public void compose_quantityAndUnit() {
        assertEquals("100 g beef", text("beef", "100", "g"));
        assertEquals("1 L milk", text("milk", "1", "L"));
    }

    @Test
    public void compose_pieces_haveNoUnitText() {
        assertEquals("2 red peppers", text("red peppers", "2", "pcs"));
        assertEquals("2 red peppers", text("red peppers", "2", null));
    }

    @Test
    public void compose_commaDecimal_isAccepted() {
        assertEquals("1.5 kg rice", text("rice", "1,5", "kg"));
    }

    @Test
    public void compose_trimsTheName() {
        assertEquals("3 eggs", text("  eggs ", "3", "pcs"));
    }

    @Test
    public void compose_piecesOfSomethingStartingWithAUnitWord_keepPcs() {
        assertEquals("2 pcs cup noodles", text("cup noodles", "2", "pcs"));
    }

    @Test
    public void compose_quantityWithoutName_isAnError() {
        assertEquals(IngredientLine.Error.NAME_REQUIRED,
                IngredientLine.compose("", "2", "g").error);
    }

    @Test
    public void compose_invalidQuantity_isAnError() {
        for (String qty : new String[] {"abc", "0", "-1", "1/2"}) {
            assertEquals(qty, IngredientLine.Error.QUANTITY_INVALID,
                    IngredientLine.compose("flour", qty, "g").error);
        }
    }

    // ---- split

    @Test
    public void split_quantityUnitAndName() {
        checkParts("200 g flour", "flour", "200", "g");
    }

    @Test
    public void split_countWithoutUnit() {
        checkParts("2 eggs", "eggs", "2", "pcs");
        checkParts("2 Red Peppers", "Red Peppers", "2", "pcs");
    }

    @Test
    public void split_withoutNumber_isNameOnly() {
        checkParts("soy sauce", "soy sauce", "", "pcs");
    }

    @Test
    public void split_doesNotConvertUnits() {
        checkParts("1.5 kg rice", "rice", "1.5", "kg");
    }

    @Test
    public void split_fractionBecomesDecimal() {
        checkParts("1/2 cup sugar", "sugar", "0.5", "cup");
    }

    @Test
    public void split_unitAliasesBecomeFormUnits() {
        checkParts("1 liter milk", "milk", "1", "L");
        checkParts("100 gr Chicken", "Chicken", "100", "g");
        checkParts("100 gramos de pollo", "pollo", "100", "g");
        checkParts("2 tablespoons honey", "honey", "2", "tbsp");
    }

    @Test
    public void split_emptyOrNull_isEmpty() {
        checkParts("", "", "", "pcs");
        checkParts(null, "", "", "pcs");
    }

    // ---- round trip and units

    @Test
    public void composeAfterSplit_keepsTheSavedText() {
        for (String saved : new String[] {"200 g flour", "2 eggs", "soy sauce", "1.5 kg rice",
                "1 L milk", "2 pcs cup noodles"}) {
            IngredientLine.Parts parts = IngredientLine.split(saved);
            assertEquals(saved, saved, text(parts.name, parts.quantity, parts.unit));
        }
    }

    @Test
    public void unitIndex_ignoresLetterCaseAndDefaultsToPieces() {
        assertEquals(0, IngredientLine.unitIndex("pcs"));
        assertEquals(4, IngredientLine.unitIndex("L"));
        assertEquals(4, IngredientLine.unitIndex("l"));
        assertEquals(0, IngredientLine.unitIndex("pinch"));
    }
}
