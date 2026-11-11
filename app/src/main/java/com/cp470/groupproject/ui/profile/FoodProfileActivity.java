package com.cp470.groupproject.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.preferences.AiPreferences;
import com.cp470.groupproject.data.preferences.FoodProfileRules;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.function.Consumer;

/**
 * "Your food profile": name, allergies (required answer), "must be" labels, diet and fitness goal.
 * Two modes: first-time setup (no back arrow; Back closes the app, so the profile can't be skipped)
 * and editing from Settings / Recipes / Chat (back arrow, normal Back).
 */
public class FoodProfileActivity extends AppCompatActivity {

    private static final String EXTRA_SETUP = "com.cp470.groupproject.ui.profile.EXTRA_SETUP";

    /** @param firstTimeSetup true for the one-time screen after the first launch (or first sign-in) */
    @NonNull
    public static Intent newIntent(@NonNull Context context, boolean firstTimeSetup) {
        return new Intent(context, FoodProfileActivity.class).putExtra(EXTRA_SETUP, firstTimeSetup);
    }

    private FoodProfileViewModel viewModel;
    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private TextInputLayout allergyLayout;
    private TextInputEditText allergyInput;
    private View allergyAdd;
    private ChipGroup allergyChips;
    private MaterialCheckBox noAllergies;
    private TextView allergyError;
    private View scroll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_food_profile);
        boolean setup = getIntent().getBooleanExtra(EXTRA_SETUP, false);

        MaterialToolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(!setup);
        }
        findViewById(R.id.profile_intro).setVisibility(setup ? View.VISIBLE : View.GONE);
        applyWindowInsets();

        if (setup) {
            // The profile is required the first time: Back closes the app instead of skipping it
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finishAffinity();
                }
            });
        }

        viewModel = new ViewModelProvider(this).get(FoodProfileViewModel.class);
        viewModel.loadOnce();

        nameLayout = findViewById(R.id.profile_name_layout);
        nameInput = findViewById(R.id.profile_name);
        allergyLayout = findViewById(R.id.profile_allergy_layout);
        allergyInput = findViewById(R.id.profile_allergy_input);
        allergyAdd = findViewById(R.id.profile_allergy_add);
        allergyChips = findViewById(R.id.profile_allergy_chips);
        noAllergies = findViewById(R.id.profile_no_allergies);
        allergyError = findViewById(R.id.profile_allergy_error);
        scroll = findViewById(R.id.profile_scroll);

        if (savedInstanceState == null) {
            nameInput.setText(viewModel.getInitialName()); // after rotation the field restores itself
        }
        nameInput.setOnFocusChangeListener((v, hasFocus) -> nameLayout.setError(null));

        allergyAdd.setOnClickListener(v -> onAddAllergy());
        allergyInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onAddAllergy();
                return true;
            }
            return false;
        });
        noAllergies.setChecked(viewModel.isNoAllergies());
        noAllergies.setOnCheckedChangeListener((button, checked) -> {
            viewModel.setNoAllergies(checked);
            refreshAllergies();
        });
        refreshAllergies();

        buildHealthChips();
        buildSingleChoice(findViewById(R.id.profile_diet_chips), AiPreferences.DIET_OPTIONS,
                getResources().getStringArray(R.array.profile_diet_labels), getString(R.string.profile_diet_none),
                viewModel.getDiet(), viewModel::setDiet);
        buildSingleChoice(findViewById(R.id.profile_goal_chips), AiPreferences.GOAL_OPTIONS,
                getResources().getStringArray(R.array.profile_goal_labels), getString(R.string.profile_goal_none),
                viewModel.getFitnessGoal(), viewModel::setFitnessGoal);

        findViewById(R.id.profile_save).setOnClickListener(v -> onSave());
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.profile_root);
        View appBar = findViewById(R.id.profile_app_bar);
        View scrollView = findViewById(R.id.profile_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            scrollView.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ---------------------------------------------------------------- allergies

    private void onAddAllergy() {
        CharSequence text = allergyInput.getText();
        if (text == null || text.toString().trim().isEmpty()) return;
        switch (viewModel.addAllergy(text.toString())) {
            case ADDED:
                allergyInput.setText(null);
                allergyLayout.setError(null);
                refreshAllergies();
                break;
            case INVALID:
                allergyLayout.setError(getString(R.string.profile_allergy_invalid));
                break;
            case LIMIT:
                allergyLayout.setError(getString(R.string.profile_allergy_limit, AiPreferences.MAX_ALLERGIES));
                break;
        }
    }

    /** Redraws the allergy chips; "no allergies" can only be ticked while the list is empty. */
    private void refreshAllergies() {
        allergyChips.removeAllViews();
        List<String> allergies = viewModel.getAllergies();
        for (String allergy : allergies) {
            Chip chip = new Chip(this);
            chip.setText(allergy);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.profile_allergy_remove, allergy));
            chip.setOnCloseIconClickListener(v -> {
                viewModel.removeAllergy(allergy);
                refreshAllergies();
            });
            allergyChips.addView(chip);
        }
        boolean hasAllergies = !allergies.isEmpty();
        if (hasAllergies && noAllergies.isChecked()) {
            noAllergies.setChecked(false);
        }
        noAllergies.setEnabled(!hasAllergies);
        boolean canAdd = !noAllergies.isChecked();
        allergyLayout.setEnabled(canAdd);
        allergyAdd.setEnabled(canAdd);
        if (hasAllergies || noAllergies.isChecked()) {
            allergyError.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------- chips

    private void buildHealthChips() {
        ChipGroup group = findViewById(R.id.profile_health_chips);
        String[] labels = getResources().getStringArray(R.array.profile_health_labels);
        for (int i = 0; i < AiPreferences.HEALTH_OPTIONS.size(); i++) {
            String option = AiPreferences.HEALTH_OPTIONS.get(i);
            Chip chip = new Chip(this);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChecked(viewModel.hasHealth(option));
            chip.setOnCheckedChangeListener((button, checked) -> viewModel.setHealth(option, checked));
            group.addView(chip);
        }
    }

    /** One "none" chip plus one chip per option; exactly one is always selected. */
    private void buildSingleChoice(@NonNull ChipGroup group, @NonNull List<String> options,
                                   @NonNull String[] labels, @NonNull String noneLabel,
                                   @Nullable String current, @NonNull Consumer<String> onPick) {
        addChoiceChip(group, noneLabel, null, current == null, onPick);
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            addChoiceChip(group, labels[i], option, option.equals(current), onPick);
        }
    }

    private void addChoiceChip(@NonNull ChipGroup group, @NonNull String label, @Nullable String value,
                               boolean checked, @NonNull Consumer<String> onPick) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId()); // single selection needs ids
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        group.addView(chip);
        chip.setChecked(checked);
        chip.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) onPick.accept(value);
        });
    }

    // ---------------------------------------------------------------- save

    private void onSave() {
        CharSequence name = nameInput.getText();
        FoodProfileRules.Problem problem = viewModel.save(name == null ? null : name.toString());
        switch (problem) {
            case NONE:
                Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                finish();
                break;
            case NAME_MISSING:
                nameLayout.setError(getString(R.string.profile_name_missing));
                scroll.scrollTo(0, 0);
                break;
            case NAME_INVALID:
                nameLayout.setError(getString(R.string.profile_name_invalid));
                scroll.scrollTo(0, 0);
                break;
            case ALLERGIES_UNANSWERED:
                allergyError.setVisibility(View.VISIBLE);
                scroll.scrollTo(0, allergyLayout.getTop());
                break;
        }
    }

    /** Edit mode: back to the screen that opened it. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}