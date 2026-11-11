package com.cp470.groupproject.ui.mealplanner;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.cp470.groupproject.R;
import com.cp470.groupproject.util.IngredientLine;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The ingredient part of the meal dialog: one input row (name, quantity, unit) and an Add button,
 * like the shopping list form. each added ingredient becomes a small chip below; tapping a chip
 * moves it back to the input row to edit it, and its close icon removes it. the text is built by
 * {@link IngredientLine}, so this class only handles the views.
 */
final class IngredientEditor {

    private final Context context;
    private final AutoCompleteTextView name;
    private final EditText quantity;
    private final Spinner unit;
    private final ChipGroup chips;
    private final List<String> ingredients = new ArrayList<>();

    /**
     * @param context used to build the chips and the adapters
     * @param content the inflated meal dialog
     * @param suggestions names offered while typing an ingredient, may be empty
     */
    IngredientEditor(@NonNull Context context, @NonNull View content,
                     @NonNull List<String> suggestions) {
        this.context = context;
        name = content.findViewById(R.id.edtIngName);
        quantity = content.findViewById(R.id.edtIngQty);
        unit = content.findViewById(R.id.spnIngUnit);
        chips = content.findViewById(R.id.chipsIngredients);

        name.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, suggestions));
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(context,
                R.array.ingredient_unit_labels, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unit.setAdapter(unitAdapter);

        content.findViewById(R.id.btnAddIngredient).setOnClickListener(v -> addTyped());
        quantity.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTyped();
                return true;
            }
            return false;
        });
    }

    /** replaces the added ingredients (for example with the ones of a saved recipe) */
    void setIngredients(@NonNull List<String> texts) {
        ingredients.clear();
        ingredients.addAll(texts);
        clearInputs();
        showChips();
    }

    /** @return the ingredients added so far, as saved texts such as "100 g beef" */
    List<String> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    /**
     * adds what is typed in the input row, if anything, and empties the row. a row with something
     * wrong shows its error and nothing is added.
     *
     * @return false if the typed ingredient is invalid
     */
    boolean addTyped() {
        IngredientLine.Result result = IngredientLine.compose(name.getText().toString(),
                quantity.getText().toString(), IngredientLine.UNITS[unit.getSelectedItemPosition()]);
        if (result.error == IngredientLine.Error.NAME_REQUIRED) {
            name.setError(context.getString(R.string.shopping_error_name));
            return false;
        }
        if (result.error == IngredientLine.Error.QUANTITY_INVALID) {
            quantity.setError(context.getString(R.string.shopping_error_qty));
            return false;
        }
        if (!result.text.isEmpty()) {
            ingredients.add(result.text);
            showChips();
            clearInputs();
        }
        return true;
    }

    /** empties the input row and puts the cursor back on the name, ready for the next one */
    private void clearInputs() {
        name.setText("", false);
        name.setError(null);
        quantity.setText("");
        quantity.setError(null);
        unit.setSelection(0);
        name.requestFocus();
    }

    /** redraws one chip per ingredient */
    private void showChips() {
        chips.removeAllViews();
        for (int i = 0; i < ingredients.size(); i++) {
            final int index = i;
            Chip chip = new Chip(context);
            chip.setText(ingredients.get(i));
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(context.getString(R.string.planner_remove_ingredient));
            chip.setOnCloseIconClickListener(v -> {
                ingredients.remove(index);
                showChips();
            });
            chip.setOnClickListener(v -> edit(index));
            chips.addView(chip);
        }
    }

    /** moves an added ingredient back to the input row so it can be changed */
    private void edit(int index) {
        // what is already typed is kept as one more ingredient, not overwritten
        if (!addTyped()) {
            return;
        }
        IngredientLine.Parts parts = IngredientLine.split(ingredients.get(index));
        ingredients.remove(index);
        showChips();
        name.setText(parts.name, false);
        quantity.setText(parts.quantity);
        unit.setSelection(IngredientLine.unitIndex(parts.unit));
        name.setSelection(name.getText().length());
    }
}
