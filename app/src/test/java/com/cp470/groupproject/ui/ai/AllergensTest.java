package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.remote.dto.MatchDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Allergen warnings: text, which line contains the allergen, and never buying one. */
public class AllergensTest {

    private static MatchDto match(String... allergens) {
        MatchDto match = new MatchDto();
        match.have = Collections.emptyList();
        match.missing = Arrays.asList("peanut butter", "bread");
        match.allergens = Arrays.asList(allergens);
        return match;
    }

    @Test
    public void names_joinsWithoutDuplicates_orNullWhenSafe() {
        assertEquals("peanut butter, shrimp", Allergens.names(match("peanut butter", "peanut butter", "shrimp")));
        assertNull(Allergens.names(match()));
        assertNull(Allergens.names(null));
        assertFalse(Allergens.has(new MatchDto()));
    }

    @Test
    public void inLine_needsEveryWord_andHandlesPlurals() {
        List<String> allergens = Arrays.asList("peanut butter", "chicken breast");
        assertEquals("peanut butter", Allergens.inLine("2 tbsp Peanut Butter", allergens));
        assertEquals("chicken breast", Allergens.inLine("2 chicken breasts", allergens));
        assertNull(Allergens.inLine("1 cup butter", allergens));
        assertNull(Allergens.inLine("2 tbsp peanut butter", null));
    }

    @Test
    public void buildRows_marksTheAllergenLine() {
        RecipeDto recipe = new RecipeDto();
        recipe.title = "PB toast";
        recipe.ingredientLines = Arrays.asList("2 tbsp peanut butter", "2 slices bread");
        recipe.match = match("peanut butter");

        List<RecipeDetailViewModel.IngredientRow> rows = RecipeDetailViewModel.buildRows(recipe);

        assertEquals("peanut butter", rows.get(0).allergen);
        assertNull(rows.get(1).allergen);
    }

    @Test
    public void missingAllergens_areNeverAddedToTheShoppingList() {
        RecipeDto recipe = new RecipeDto();
        recipe.title = "PB toast";
        recipe.ingredientLines = Arrays.asList("2 tbsp peanut butter", "2 slices bread");
        recipe.match = match("peanut butter");

        List<ShoppingItem> items = MissingIngredients.fromDetailRows(RecipeDetailViewModel.buildRows(recipe));

        assertEquals(1, items.size());
        assertEquals("Bread", items.get(0).name);
        assertTrue(Allergens.has(recipe.match));
    }
}