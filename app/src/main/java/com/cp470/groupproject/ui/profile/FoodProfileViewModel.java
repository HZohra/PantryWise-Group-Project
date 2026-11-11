package com.cp470.groupproject.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;

import com.cp470.groupproject.data.preferences.AiPreferences;
import com.cp470.groupproject.data.preferences.AiPreferencesStore;
import com.cp470.groupproject.data.preferences.FoodProfileRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The food profile being edited (MVVM: no Views here). Loaded once from the store, kept across
 * rotation, and saved only when the rules pass. Main thread only.
 */
public class FoodProfileViewModel extends AndroidViewModel {

    /** Result of adding one allergy. */
    public enum AddResult { ADDED, INVALID, LIMIT }

    private final AiPreferencesStore store;
    private boolean loaded;

    private final List<String> allergies = new ArrayList<>();
    private final Set<String> health = new LinkedHashSet<>();
    @Nullable private String diet;
    @Nullable private String fitnessGoal;
    private boolean noAllergies;
    @NonNull private String initialName = "";

    public FoodProfileViewModel(@NonNull Application application) {
        this(application, AiPreferencesStore.from(application));
    }

    FoodProfileViewModel(@NonNull Application application, @NonNull AiPreferencesStore store) {
        super(application);
        this.store = store;
    }

    /** Reads the saved profile the first time only (not again after rotation). */
    public void loadOnce() {
        if (loaded) return;
        AiPreferences saved = store.load();
        allergies.addAll(saved.allergies);
        health.addAll(saved.health);
        diet = saved.diet;
        fitnessGoal = saved.fitnessGoal;
        noAllergies = store.loadNoAllergies();
        initialName = store.loadName();
        loaded = true;
    }

    @NonNull public String getInitialName() { return initialName; }
    @NonNull public List<String> getAllergies() { return Collections.unmodifiableList(allergies); }
    public boolean isNoAllergies() { return noAllergies; }
    public boolean hasHealth(@NonNull String option) { return health.contains(option); }
    @Nullable public String getDiet() { return diet; }
    @Nullable public String getFitnessGoal() { return fitnessGoal; }

    /** Adds one allergy, cleaned with the backend's rules; a duplicate counts as added. */
    @NonNull
    public AddResult addAllergy(@Nullable String raw) {
        String clean = AiPreferences.cleanAllergy(raw);
        if (clean == null) return AddResult.INVALID;
        if (allergies.contains(clean)) return AddResult.ADDED;
        if (allergies.size() >= AiPreferences.MAX_ALLERGIES) return AddResult.LIMIT;
        allergies.add(clean);
        noAllergies = false;
        return AddResult.ADDED;
    }

    public void removeAllergy(@NonNull String allergy) {
        allergies.remove(allergy);
    }

    /** "I have no food allergies"; only possible while the list is empty. */
    public void setNoAllergies(boolean value) {
        noAllergies = value && allergies.isEmpty();
    }

    public void setHealth(@NonNull String option, boolean selected) {
        if (selected) health.add(option); else health.remove(option);
    }

    public void setDiet(@Nullable String value) { diet = value; }
    public void setFitnessGoal(@Nullable String value) { fitnessGoal = value; }

    /**
     * Saves the profile when the rules pass (and marks the first-time setup as done).
     * @return NONE when saved, otherwise the first problem to show
     */
    @NonNull
    public FoodProfileRules.Problem save(@Nullable String rawName) {
        FoodProfileRules.Problem problem = FoodProfileRules.check(rawName, allergies, noAllergies);
        if (problem != FoodProfileRules.Problem.NONE) return problem;
        String name = FoodProfileRules.cleanName(rawName);
        store.saveProfile(name, new AiPreferences(allergies, new ArrayList<>(health), diet, fitnessGoal),
                noAllergies);
        return FoodProfileRules.Problem.NONE;
    }
}