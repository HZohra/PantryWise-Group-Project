package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests how recipe ingredient lines are read into name, quantity and unit. */
public class IngredientParserTest {

    private static void check(String raw, String name, double quantity, String unit,
                              boolean explicit) {
        IngredientParser.Parsed parsed = IngredientParser.parse(raw);
        assertNotNull(raw, parsed);
        assertEquals(raw, name, parsed.normalizedName);
        assertEquals(raw, quantity, parsed.quantity, 1e-9);
        assertEquals(raw, unit, parsed.unit);
        assertEquals(raw, explicit, parsed.explicitQuantity);
    }

    @Test
    public void parse_quantityUnitAndName() {
        check("200 g flour", "flour", 200, "g", true);
    }

    @Test
    public void parse_unitGluedToNumber() {
        check("200g flour", "flour", 200, "g", true);
    }

    @Test
    public void parse_kilogramsBecomeGrams() {
        check("1.5 kg rice", "rice", 1500, "g", true);
    }

    @Test
    public void parse_countWithoutUnit() {
        check("2 eggs", "eggs", 2, "pcs", true);
    }

    @Test
    public void parse_sizeWordIsNotAUnit() {
        check("2 large eggs", "eggs", 2, "pcs", true);
    }

    @Test
    public void parse_fraction() {
        check("1/2 cup sugar", "sugar", 0.5, "cup", true);
        check("1 / 2 cup sugar", "sugar", 0.5, "cup", true);
    }

    @Test
    public void parse_commaDecimalAndLiters() {
        check("1,5 L milk", "milk", 1500, "ml", true);
    }

    @Test
    public void parse_withoutQuantity_isPresenceOnly() {
        check("soy sauce", "soy sauce", 1, "pcs", false);
    }

    @Test
    public void parse_zeroDenominator_isTreatedAsNoQuantity() {
        IngredientParser.Parsed parsed = IngredientParser.parse("0/0 cup x");

        assertNotNull(parsed);
        assertFalse(parsed.explicitQuantity);
    }

    @Test
    public void parse_upperCaseUnit() {
        check("200 G Flour", "flour", 200, "g", true);
    }

    @Test
    public void parse_displayNameIsCapitalized() {
        assertEquals("Soy sauce", IngredientParser.parse("soy sauce").displayName);
    }

    @Test
    public void parse_noUsableName_returnsNull() {
        assertNull(IngredientParser.parse("12"));
        assertNull(IngredientParser.parse(""));
        assertNull(IngredientParser.parse("   "));
        assertNull(IngredientParser.parse(null));
    }

    @Test
    public void parse_oddText_doesNotFail() {
        assertNull(IngredientParser.parse("2%"));
        assertTrue(IngredientParser.parse("2 %milk").explicitQuantity);
    }

    @Test
    public void parse_gramAbbreviation_isAUnit() {
        check("100 gr chicken", "chicken", 100, "g", true);
    }

    @Test
    public void parse_spanishUnitWithDe_dropsTheLinkWord() {
        check("100 gramos de pollo", "pollo", 100, "g", true);
    }

    @Test
    public void parse_ofAfterTheUnit_isDropped() {
        check("100 grams of chicken", "chicken", 100, "g", true);
    }

    @Test
    public void parse_ofAfterCups_isDropped() {
        check("2 cups of flour", "flour", 2, "cup", true);
    }

    @Test
    public void parse_ofInsideTheName_isKept() {
        check("2 cream of mushroom", "cream of mushroom", 2, "pcs", true);
    }
}
