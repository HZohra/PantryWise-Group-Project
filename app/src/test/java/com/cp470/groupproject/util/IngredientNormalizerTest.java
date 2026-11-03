package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** plain jvm test */
public class IngredientNormalizerTest {

    @Test
    public void removesPercentAndVolume() {
        assertEquals("milk", IngredientNormalizer.normalize("2% Milk 1L"));
    }

    @Test
    public void removesWeightAndLowercases() {
        assertEquals("chicken breast", IngredientNormalizer.normalize("Chicken Breast 500g"));
    }

    @Test
    public void removesNoiseWordsAndExtraSpaces() {
        assertEquals("eggs", IngredientNormalizer.normalize("  Organic   EGGS  "));
    }

    @Test
    public void keepsAccentedLettersForSpanish() {
        assertEquals("jamón", IngredientNormalizer.normalize("Jamón 200g"));
    }

    @Test
    public void nullAndBlankGiveEmptyString() {
        assertEquals("", IngredientNormalizer.normalize(null));
        assertEquals("", IngredientNormalizer.normalize("   "));
    }

    @Test
    public void plainNameIsUnchanged() {
        assertEquals("milk", IngredientNormalizer.normalize("Milk"));
    }
}