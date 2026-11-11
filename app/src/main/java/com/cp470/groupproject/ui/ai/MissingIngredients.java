package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeIngredientDto;
import com.cp470.groupproject.util.IngredientParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns the missing ingredients of a recipe into shopping list items (UC "Add missing to list").
 * The name is the ingredient ("Garlic"); the amount comes from the line when it has one
 * ("2 cloves garlic"), otherwise 1 piece. Plain Java, so every rule here is unit-tested.
 */
final class MissingIngredients {

    private MissingIngredients() { }

    /** Edamam recipe: the rows marked Missing on Recipe details. */
    @NonNull
    static List<ShoppingItem> fromDetailRows(@Nullable List<RecipeDetailViewModel.IngredientRow> rows) {
        List<ShoppingItem> items = new ArrayList<>();
        if (rows == null) return items;
        for (RecipeDetailViewModel.IngredientRow row : rows) {
            if (row.status == IngredientMatcher.Status.MISSING) {
                items.add(toItem(row.line, row.name));
            }
        }
        return items;
    }

    /** AI recipe: what is not in the pantry, not expiring (those are in the pantry) and not a staple. */
    @NonNull
    static List<ShoppingItem> fromAi(@Nullable AiRecipeDto recipe) {
        List<ShoppingItem> items = new ArrayList<>();
        if (recipe == null || recipe.ingredients == null) return items;
        for (AiRecipeIngredientDto ingredient : recipe.ingredients) {
            if (ingredient == null || ingredient.inPantry || ingredient.expiring || ingredient.staple) continue;
            if (ingredient.name == null || ingredient.name.trim().isEmpty()) continue;
            String amount = ingredient.amount == null ? "" : ingredient.amount.trim();
            items.add(toItem((amount + " " + ingredient.name.trim()).trim(), ingredient.name));
        }
        return items;
    }

    /**
     * @param line the ingredient line, e.g. "2 cloves garlic"
     * @param name the plain ingredient name if known (e.g. "garlic"), used as the item name
     */
    @NonNull
    static ShoppingItem toItem(@NonNull String line, @Nullable String name) {
        IngredientParser.Parsed parsed = IngredientParser.parse(line);
        String itemName = name != null && !name.trim().isEmpty() ? name.trim()
                : parsed != null && !parsed.normalizedName.isEmpty() ? parsed.normalizedName : line.trim();
        boolean hasAmount = parsed != null && parsed.explicitQuantity && parsed.quantity > 0;
        double quantity = hasAmount ? parsed.quantity : 1;
        String unit = hasAmount && !parsed.unit.isEmpty() ? parsed.unit : "pcs";
        return new ShoppingItem(capitalize(itemName), quantity, unit, null);
    }

    @NonNull
    private static String capitalize(@NonNull String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}