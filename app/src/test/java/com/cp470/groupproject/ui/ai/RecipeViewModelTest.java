package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.dto.ExpiringItemDto;
import com.cp470.groupproject.data.remote.dto.MatchDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.RecommendResponse;

import org.junit.Test;

import java.util.Collections;

public class RecipeViewModelTest {

    private static RecipeDto recipe(boolean usesExpiring) {
        RecipeDto recipe = new RecipeDto();
        recipe.match = new MatchDto();
        if (usesExpiring) {
            ExpiringItemDto item = new ExpiringItemDto();
            item.name = "spinach";
            recipe.match.expiringUsed = Collections.singletonList(item);
        } else {
            recipe.match.expiringUsed = Collections.emptyList();
        }
        return recipe;
    }

    private static RecommendResponse response(boolean personalized, boolean broadened, RecipeDto... recipes) {
        RecommendResponse data = new RecommendResponse();
        data.personalized = personalized;
        data.broadened = broadened;
        data.recipes = java.util.Arrays.asList(recipes);
        return data;
    }

    @Test
    public void emptyPantry_showsAddFoodNotice() {
        assertEquals(R.string.ai_recipes_empty_pantry,
                RecipeViewModel.chooseNotice(response(false, false, recipe(false)), false));
    }

    @Test
    public void broadenedSearch_showsSimilarRecipesNotice() {
        assertEquals(R.string.ai_recipes_broadened,
                RecipeViewModel.chooseNotice(response(true, true, recipe(false)), false));
    }

    @Test
    public void zeroWaste_withNothingExpiring_showsNotice() {
        assertEquals(R.string.ai_recipes_nothing_expiring,
                RecipeViewModel.chooseNotice(response(true, false, recipe(false)), true));
    }

    @Test
    public void normalResults_showNoNotice() {
        assertEquals(0, RecipeViewModel.chooseNotice(response(true, false, recipe(true)), true));
        assertEquals(0, RecipeViewModel.chooseNotice(response(true, false, recipe(false)), false));
    }
}