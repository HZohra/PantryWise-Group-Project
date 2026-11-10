package com.cp470.groupproject.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipeRepositoryLimitsTest {

    private static String repeat(char c, int times) {
        return new String(new char[times]).replace('\0', c);
    }

    @Test
    public void clampText_trimsAndCuts() {
        assertEquals("", RecipeRepository.clampText(null, 10));
        assertEquals("egg", RecipeRepository.clampText("  egg  ", 10));
        assertEquals(RecipeRepository.MAX_TITLE_LENGTH,
                RecipeRepository.clampText(repeat('a', 300), RecipeRepository.MAX_TITLE_LENGTH).length());
    }

    @Test
    public void cleanLines_dropsBlanks_andRespectsLimits() {
        List<String> lines = new ArrayList<>(Arrays.asList("  2 eggs ", "", "   ", repeat('b', 500)));
        for (int i = 0; i < 100; i++) lines.add("line " + i);

        List<String> clean = RecipeRepository.cleanLines(lines);

        assertEquals("2 eggs", clean.get(0));
        assertEquals(RecipeRepository.MAX_LINE_LENGTH, clean.get(1).length());
        assertEquals(RecipeRepository.MAX_LINES, clean.size());
        assertTrue(RecipeRepository.cleanLines(null).isEmpty());
    }
}