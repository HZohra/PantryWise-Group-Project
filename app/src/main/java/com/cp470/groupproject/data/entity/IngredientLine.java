package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;

import com.cp470.groupproject.util.IngredientParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The columns shared by the ingredient rows of a meal plan and of a favorite recipe. it is not an
 * entity itself: room adds these fields to the table of each subclass.
 *
 * the line is parsed once, when it is created, so the shopping list does not have to read the text
 * again. {@link #text} keeps what the user typed, because {@link #quantity} and {@link #unit} are
 * in a base unit ("1 kg" becomes 1000 g).
 */
public abstract class IngredientLine {

    /** order of the ingredient inside its recipe */
    public int position;

    /** the line as it was typed, such as "200 g flour" */
    @NonNull
    public String text = "";

    /** canonical name used to compare with the inventory, empty if the text has no usable name */
    @NonNull
    public String normalizedName = "";

    /** amount in the base unit, 1 when the text had no amount */
    public double quantity = 1;

    /** base unit such as "g", "ml" or "pcs" */
    @NonNull
    public String unit = "";

    /** false for ingredients without an amount ("soy sauce"): only having it matters */
    public boolean explicitQuantity;

    /**
     * reads a typed line into the columns.
     *
     * @param raw the ingredient text, may be null
     * @param position order of the ingredient inside its recipe
     */
    protected final void fill(String raw, int position) {
        this.position = position;
        this.text = raw == null ? "" : raw.trim();
        IngredientParser.Parsed parsed = IngredientParser.parse(text);
        if (parsed == null) {
            this.normalizedName = "";
            this.quantity = 0;
            this.unit = "";
            this.explicitQuantity = false;
            return;
        }
        this.normalizedName = parsed.normalizedName;
        this.quantity = parsed.quantity;
        this.unit = parsed.unit;
        this.explicitQuantity = parsed.explicitQuantity;
    }

    /** @return the name to show in a shopping list, such as "Flour" */
    public String displayName() {
        if (normalizedName.isEmpty()) {
            return "";
        }
        return normalizedName.substring(0, 1).toUpperCase(Locale.ROOT) + normalizedName.substring(1);
    }

    /** @return the typed text of each line, in the given order */
    public static List<String> texts(@NonNull List<? extends IngredientLine> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (IngredientLine line : lines) {
            result.add(line.text);
        }
        return result;
    }
}
