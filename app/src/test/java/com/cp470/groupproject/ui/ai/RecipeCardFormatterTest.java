package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.ExpiringItemDto;
import com.cp470.groupproject.data.remote.dto.MatchDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class RecipeCardFormatterTest {

    private static ExpiringItemDto expiring(String name, int days) {
        ExpiringItemDto item = new ExpiringItemDto();
        item.name = name;
        item.daysUntilExpiry = days;
        return item;
    }

    @Test
    public void hasMatch_onlyWhenThereAreIngredients() {
        assertFalse(RecipeCardFormatter.hasMatch(null));
        MatchDto empty = new MatchDto();
        assertFalse(RecipeCardFormatter.hasMatch(empty)); // totalCount 0: search result, hide the line
        MatchDto match = new MatchDto();
        match.haveCount = 3;
        match.totalCount = 5;
        assertTrue(RecipeCardFormatter.hasMatch(match));
    }

    @Test
    public void expiringNames_joinsInOrder_orNullWhenNone() {
        assertNull(RecipeCardFormatter.expiringNames(null));
        MatchDto match = new MatchDto();
        match.expiringUsed = Collections.emptyList();
        assertNull(RecipeCardFormatter.expiringNames(match));

        match.expiringUsed = Arrays.asList(expiring("spinach", 0), expiring(" milk ", 2));
        assertEquals("spinach, milk", RecipeCardFormatter.expiringNames(match));
    }

    @Test
    public void minutes_roundsAndHidesUnknown() {
        assertNull(RecipeCardFormatter.minutes(null));
        assertNull(RecipeCardFormatter.minutes(0.0)); // Edamam uses 0 for "unknown"
        assertEquals(Integer.valueOf(25), RecipeCardFormatter.minutes(24.6));
    }

    @Test
    public void joinMeta_skipsEmptyParts() {
        assertEquals("25 min · BBC Good Food", RecipeCardFormatter.joinMeta("25 min", "BBC Good Food"));
        assertEquals("BBC Good Food", RecipeCardFormatter.joinMeta(null, "BBC Good Food"));
        assertNull(RecipeCardFormatter.joinMeta(null, "  "));
    }
}