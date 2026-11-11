package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeIngredientDto;
import com.cp470.groupproject.data.remote.dto.MatchDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Which ingredients go to the shopping list, and with what name and amount. Plain JUnit. */
public class MissingIngredientsTest {

    private static AiRecipeIngredientDto ai(String amount, String name, boolean inPantry,
                                            boolean expiring, boolean staple) {
        AiRecipeIngredientDto item = new AiRecipeIngredientDto();
        item.amount = amount;
        item.name = name;
        item.inPantry = inPantry;
        item.expiring = expiring;
        item.staple = staple;
        return item;
    }

    @Test
    public void fromAi_keepsOnlyMissingNonStapleIngredients() {
        AiRecipeDto recipe = new AiRecipeDto();
        recipe.ingredients = Arrays.asList(
                ai("500 g", "chicken", true, true, false),   // expiring → in pantry
                ai("1 cup", "rice", true, false, false),     // in pantry
                ai("2 cloves", "garlic", false, false, false),
                ai("1 tsp", "salt", false, false, true),     // staple
                ai("1 tbsp", "olive oil", false, false, false));

        List<ShoppingItem> items = MissingIngredients.fromAi(recipe);

        assertEquals(2, items.size());
        assertEquals("Garlic", items.get(0).name);
        assertEquals("Olive oil", items.get(1).name);
    }

    @Test
    public void fromAi_nothingMissing_isEmpty() {
        AiRecipeDto recipe = new AiRecipeDto();
        recipe.ingredients = Collections.singletonList(ai("1 cup", "rice", true, false, false));

        assertTrue(MissingIngredients.fromAi(recipe).isEmpty());
        assertTrue(MissingIngredients.fromAi(null).isEmpty());
    }

    @Test
    public void fromDetailRows_keepsOnlyMissingRows() {
        RecipeDto recipe = new RecipeDto();
        recipe.title = "Spinach Omelette";
        recipe.ingredientLines = Arrays.asList("3 large eggs", "1 cup chopped spinach", "salt to taste");
        recipe.match = new MatchDto();
        recipe.match.have = Collections.singletonList("spinach");
        recipe.match.missing = Collections.singletonList("egg");

        List<ShoppingItem> items = MissingIngredients.fromDetailRows(RecipeDetailViewModel.buildRows(recipe));

        assertEquals(1, items.size());
        assertEquals("Egg", items.get(0).name);
    }

    @Test
    public void toItem_withoutAmount_isOnePiece() {
        ShoppingItem item = MissingIngredients.toItem("soy sauce", "soy sauce");

        assertEquals("Soy sauce", item.name);
        assertEquals(1.0, item.quantity, 0.0001);
        assertEquals("pcs", item.unit);
    }

    @Test
    public void toItem_withAmount_keepsAPositiveQuantityAndAUnit() {
        ShoppingItem item = MissingIngredients.toItem("200 g flour", "flour");

        assertEquals("Flour", item.name);
        assertTrue(item.quantity > 0);
        assertTrue(!item.unit.isEmpty());
    }
}