package com.cp470.groupproject.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Collections;

/** Name rules and the "allergies must be answered" rule. */
public class FoodProfileRulesTest {

    @Test
    public void cleanName_acceptsRealNames() {
        assertEquals("Alex", FoodProfileRules.cleanName("  Alex "));
        assertEquals("Mary-Jane O'Neil", FoodProfileRules.cleanName("Mary-Jane   O'Neil"));
        assertEquals("José", FoodProfileRules.cleanName("José"));
        assertEquals("A. Smith", FoodProfileRules.cleanName("A. Smith"));
    }

    @Test
    public void cleanName_rejectsDigitsSymbolsAndLongText() {
        assertNull(FoodProfileRules.cleanName("Alex1"));
        assertNull(FoodProfileRules.cleanName("<b>"));
        assertNull(FoodProfileRules.cleanName("   "));
        assertNull(FoodProfileRules.cleanName(null));
        assertNull(FoodProfileRules.cleanName(new String(new char[FoodProfileRules.MAX_NAME_LENGTH + 1]).replace('\0', 'a')));
    }

    @Test
    public void check_nameFirst_thenAllergies() {
        assertEquals(FoodProfileRules.Problem.NAME_MISSING,
                FoodProfileRules.check(" ", Collections.emptyList(), false));
        assertEquals(FoodProfileRules.Problem.NAME_INVALID,
                FoodProfileRules.check("R2D2", Collections.emptyList(), true));
        assertEquals(FoodProfileRules.Problem.ALLERGIES_UNANSWERED,
                FoodProfileRules.check("Alex", Collections.emptyList(), false));
    }

    @Test
    public void check_passesWithAllergiesOrWithNoAllergiesTicked() {
        assertEquals(FoodProfileRules.Problem.NONE,
                FoodProfileRules.check("Alex", Collections.singletonList("peanut"), false));
        assertEquals(FoodProfileRules.Problem.NONE,
                FoodProfileRules.check("Alex", Collections.emptyList(), true));
    }
}