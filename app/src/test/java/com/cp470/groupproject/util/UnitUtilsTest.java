package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests the unit conversions used to compare ingredient quantities. */
public class UnitUtilsTest {

    @Test
    public void toBase_kgToGrams() {
        UnitUtils.Quantity q = UnitUtils.toBase(1.5, "kg");

        assertEquals(1500, q.value, 1e-9);
        assertEquals("g", q.unit);
    }

    @Test
    public void toBase_litersToMilliliters() {
        UnitUtils.Quantity q = UnitUtils.toBase(2, "L");

        assertEquals(2000, q.value, 1e-9);
        assertEquals("ml", q.unit);
    }

    @Test
    public void toBase_pieceAliases() {
        for (String alias : new String[] {"", "pcs", "pc", "piece", "x"}) {
            assertEquals(alias, "pcs", UnitUtils.toBase(1, alias).unit);
        }
    }

    @Test
    public void toBase_unknownUnitIsKept() {
        assertEquals("bunch", UnitUtils.toBase(1, "Bunch").unit);
    }

    @Test
    public void sameDimension_comparesWhatIsMeasured() {
        assertTrue(UnitUtils.sameDimension("g", "kg"));
        assertTrue(UnitUtils.sameDimension("ml", "L"));
        assertFalse(UnitUtils.sameDimension("g", "ml"));
        assertFalse(UnitUtils.sameDimension("pcs", "g"));
    }

    @Test
    public void isKnownUnit_acceptsOnlyListedUnits() {
        assertTrue(UnitUtils.isKnownUnit("kg"));
        assertTrue(UnitUtils.isKnownUnit("TBSP"));
        assertFalse(UnitUtils.isKnownUnit("Eggs"));
        assertFalse(UnitUtils.isKnownUnit(""));
        assertFalse(UnitUtils.isKnownUnit(null));
    }

    @Test
    public void pretty_promotesToKgAndL() {
        assertEquals(1.5, UnitUtils.pretty(1500, "g").value, 1e-9);
        assertEquals("kg", UnitUtils.pretty(1500, "g").unit);
        assertEquals(500, UnitUtils.pretty(500, "g").value, 1e-9);
        assertEquals("g", UnitUtils.pretty(500, "g").unit);
        assertEquals(2, UnitUtils.pretty(2000, "ml").value, 1e-9);
        assertEquals("L", UnitUtils.pretty(2000, "ml").unit);
    }

    @Test
    public void pretty_roundsToTwoDecimals() {
        assertEquals(1.33, UnitUtils.pretty(4.0 / 3, "pcs").value, 1e-9);
    }

    @Test
    public void displayUnit_pcsIsEmpty() {
        assertEquals("", UnitUtils.displayUnit("pcs"));
        assertEquals("tbsp", UnitUtils.displayUnit("tbsp"));
    }

    @Test
    public void toBase_gramAbbreviationAndSpanishNames() {
        for (String alias : new String[] {"gr", "grs", "gramo", "gramos", "Gramos"}) {
            assertEquals(alias, "g", UnitUtils.toBase(1, alias).unit);
        }
    }

    @Test
    public void toBase_spanishKilosAndLitersAreConverted() {
        assertEquals(2000, UnitUtils.toBase(2, "kilos").value, 1e-9);
        assertEquals(1500, UnitUtils.toBase(1.5, "litros").value, 1e-9);
    }

    @Test
    public void isKnownUnit_spanishKitchenUnits() {
        for (String unit : new String[] {"cucharada", "cucharadita", "taza", "onza", "libra",
                "unidades", "piezas"}) {
            assertTrue(unit, UnitUtils.isKnownUnit(unit));
        }
    }

    @Test
    public void canonicalUnit_returnsTheMainNameOfTheUnit() {
        assertEquals("kg", UnitUtils.canonicalUnit("Kilograms"));
        assertEquals("g", UnitUtils.canonicalUnit("gramos"));
        assertEquals("l", UnitUtils.canonicalUnit("litres"));
        assertEquals("pcs", UnitUtils.canonicalUnit("pieces"));
    }

    @Test
    public void canonicalUnit_unknownUnit_isNull() {
        assertEquals(null, UnitUtils.canonicalUnit("pinch"));
    }
}
