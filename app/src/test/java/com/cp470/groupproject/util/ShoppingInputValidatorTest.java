package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests the validation of the shopping list form. */
public class ShoppingInputValidatorTest {

    @Test
    public void blankName_isRejected() {
        assertEquals(ShoppingInputValidator.Error.NAME_REQUIRED,
                ShoppingInputValidator.validate("", "1").error);
        assertEquals(ShoppingInputValidator.Error.NAME_REQUIRED,
                ShoppingInputValidator.validate("   ", "1").error);
        assertEquals(ShoppingInputValidator.Error.NAME_REQUIRED,
                ShoppingInputValidator.validate(null, "1").error);
    }

    @Test
    public void emptyQuantity_defaultsToOne() {
        ShoppingInputValidator.Result result = ShoppingInputValidator.validate("Eggs", "");

        assertTrue(result.isValid());
        assertEquals(1.0, result.quantity, 0.0);
    }

    @Test
    public void commaDecimal_isAccepted() {
        ShoppingInputValidator.Result result = ShoppingInputValidator.validate("Milk", "1,5");

        assertTrue(result.isValid());
        assertEquals(1.5, result.quantity, 0.0);
    }

    @Test
    public void zeroOrNegativeQuantity_isRejected() {
        assertEquals(ShoppingInputValidator.Error.QUANTITY_INVALID,
                ShoppingInputValidator.validate("Eggs", "0").error);
        assertEquals(ShoppingInputValidator.Error.QUANTITY_INVALID,
                ShoppingInputValidator.validate("Eggs", "-2").error);
    }

    @Test
    public void nonNumericQuantity_isRejected() {
        assertEquals(ShoppingInputValidator.Error.QUANTITY_INVALID,
                ShoppingInputValidator.validate("Eggs", "abc").error);
    }

    @Test
    public void name_isTrimmed() {
        assertEquals("Eggs", ShoppingInputValidator.validate("  Eggs ", "2").name);
    }
}
