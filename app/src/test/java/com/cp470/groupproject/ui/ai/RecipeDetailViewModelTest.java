package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.MatchDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecipeDetailViewModelTest {

    private static RecipeDto omelette(boolean withMatch) {
        RecipeDto recipe = new RecipeDto();
        recipe.title = "Spinach Omelette";
        recipe.ingredientLines = Arrays.asList("3 large eggs", "1 cup chopped spinach", "salt to taste", "  ");
        if (withMatch) {
            recipe.match = new MatchDto();
            recipe.match.have = Collections.singletonList("spinach");
            recipe.match.missing = Collections.singletonList("egg");
        }
        return recipe;
    }

    @Test
    public void buildRows_marksEachLine_andSkipsBlankOnes() {
        List<RecipeDetailViewModel.IngredientRow> rows = RecipeDetailViewModel.buildRows(omelette(true));

        assertEquals(3, rows.size());
        assertEquals(IngredientMatcher.Status.MISSING, rows.get(0).status);
        assertEquals("egg", rows.get(0).name);
        assertEquals(IngredientMatcher.Status.IN_PANTRY, rows.get(1).status);
        assertEquals(IngredientMatcher.Status.UNKNOWN, rows.get(2).status);
    }

    @Test
    public void buildRows_withoutMatch_leavesEverythingUnmarked() {
        for (RecipeDetailViewModel.IngredientRow row : RecipeDetailViewModel.buildRows(omelette(false))) {
            assertEquals(IngredientMatcher.Status.UNKNOWN, row.status);
            assertNull(row.name);
        }
    }

    @Test
    public void buildIngredientList_staysWithinLimit() {
        List<String> lines = Arrays.asList("2 eggs", "1 cup milk", "a very long line that will not fit");

        assertEquals("- 2 eggs\n- 1 cup milk", RecipeDetailViewModel.buildIngredientList(lines, 25));
        assertTrue(RecipeDetailViewModel.buildIngredientList(lines, 1000).endsWith("will not fit"));
        assertEquals("", RecipeDetailViewModel.buildIngredientList(null, 100));
    }
}