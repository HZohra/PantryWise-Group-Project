package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IngredientMatcherTest {

    private static final List<String> NONE = Collections.emptyList();

    @Test
    public void exactMatch_isInPantry() {
        IngredientMatcher.Match m = IngredientMatcher.matchLine("1 cup rice", Arrays.asList("rice"), NONE);
        assertEquals(IngredientMatcher.Status.IN_PANTRY, m.status);
        assertEquals("rice", m.name);
    }

    @Test
    public void pluralAndDescriptors_stillMatch() {
        IngredientMatcher.Match m = IngredientMatcher.matchLine(
                "2 cups finely chopped Onions", Arrays.asList("onion"), NONE);
        assertEquals(IngredientMatcher.Status.IN_PANTRY, m.status);
    }

    @Test
    public void missingItem_returnsItsFoodName() {
        IngredientMatcher.Match m = IngredientMatcher.matchLine("3 large eggs", NONE, Arrays.asList("egg"));
        assertEquals(IngredientMatcher.Status.MISSING, m.status);
        assertEquals("egg", m.name); // what "Find substitute" sends
    }

    @Test
    public void longestNameWins() {
        IngredientMatcher.Match m = IngredientMatcher.matchLine(
                "2 tbsp peanut butter", Arrays.asList("butter"), Arrays.asList("peanut butter"));
        assertEquals(IngredientMatcher.Status.MISSING, m.status);
        assertEquals("peanut butter", m.name);
    }

    @Test
    public void partialWord_doesNotMatch() {
        IngredientMatcher.Match m = IngredientMatcher.matchLine("1/2 cup peanuts", NONE, Arrays.asList("pea"));
        assertEquals(IngredientMatcher.Status.UNKNOWN, m.status);
        assertNull(m.name);
    }

    @Test
    public void noMatchOrNullLists_isUnknown() {
        assertEquals(IngredientMatcher.Status.UNKNOWN,
                IngredientMatcher.matchLine("salt to taste", NONE, NONE).status);
        assertEquals(IngredientMatcher.Status.UNKNOWN,
                IngredientMatcher.matchLine("1 cup milk", null, null).status);
        assertEquals(IngredientMatcher.Status.UNKNOWN,
                IngredientMatcher.matchLine(null, Arrays.asList("milk"), NONE).status);
    }

    @Test
    public void normalize_followsBackendRules() {
        assertEquals("whole milk", IngredientMatcher.normalize("2% Whole Milk!"));
        assertEquals("tomato", IngredientMatcher.normalize("Tomatoes"));
        assertEquals("berry", IngredientMatcher.normalize("berries"));
        assertEquals("hummus", IngredientMatcher.normalize("hummus"));
    }
}