package com.cp470.groupproject.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.PreferencesDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Saves the user's food profile on the phone only (no account, nothing on a server):
 * the AI preferences (allergies, "must be" labels, diet, fitness goal), the name, the
 * "I have no food allergies" answer, and whether the first-time setup was completed.
 * Uses Member 5's own file "pantrywise_ai" (shared with the Zero-Waste switch, different keys);
 * the team's display settings live in another file ("PantryPreferences") and are never touched.
 * The name is kept on the phone and is never sent to the AI.
 */
public class AiPreferencesStore {

    /** Same file as RecipeViewModel.PREFS_NAME. */
    public static final String FILE_NAME = "pantrywise_ai";

    static final String KEY_ALLERGIES = "pref_allergies";
    static final String KEY_HEALTH = "pref_health";
    static final String KEY_DIET = "pref_diet";
    static final String KEY_FITNESS_GOAL = "pref_fitness_goal";
    static final String KEY_NAME = "profile_name";
    static final String KEY_NO_ALLERGIES = "profile_no_allergies";
    static final String KEY_SETUP_DONE = "profile_setup_done";

    /** Allergies can't contain a newline (letters, spaces, - and ' only), so it is a safe separator. */
    private static final String ALLERGY_SEPARATOR = "\n";
    /** Health labels are fixed words without commas. */
    private static final String HEALTH_SEPARATOR = ",";

    private final SharedPreferences prefs;

    /** @param prefs the file to use (tests can pass their own) */
    public AiPreferencesStore(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @NonNull
    public static AiPreferencesStore from(@NonNull Context context) {
        return new AiPreferencesStore(
                context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE));
    }


    /**
     * The saved profile as sent to the backend, read now (so edits apply to the very next request).
     * @return null when nothing is saved, or when it can't be read (the request then goes out
     *         without preferences, exactly as before preferences existed, instead of failing)
     */
    @Nullable
    public static PreferencesDto currentDto(@Nullable Context context) {
        if (context == null) return null;
        try {
            return from(context).load().toDto();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The saved AI preferences, cleaned again on load (an old or edited value can't break requests). */
    @NonNull
    public AiPreferences load() {
        return new AiPreferences(
                split(prefs.getString(KEY_ALLERGIES, ""), ALLERGY_SEPARATOR),
                split(prefs.getString(KEY_HEALTH, ""), HEALTH_SEPARATOR),
                prefs.getString(KEY_DIET, null),
                prefs.getString(KEY_FITNESS_GOAL, null));
    }

    /** Saves the AI preferences only (name and setup flag unchanged). */
    public void save(@NonNull AiPreferences preferences) {
        putPreferences(prefs.edit(), preferences).apply();
    }

    /** The saved name, or "" before the first-time setup. Stays on the phone. */
    @NonNull
    public String loadName() {
        return prefs.getString(KEY_NAME, "");
    }

    /** True when the user answered "I have no food allergies". */
    public boolean loadNoAllergies() {
        return prefs.getBoolean(KEY_NO_ALLERGIES, false);
    }

    /** True once the food profile was saved at least once (the first-time screen is then not shown again). */
    public boolean isSetupDone() {
        return prefs.getBoolean(KEY_SETUP_DONE, false);
    }

    /** Saves the whole profile and marks the first-time setup as done. */
    public void saveProfile(@NonNull String name, @NonNull AiPreferences preferences, boolean noAllergies) {
        putPreferences(prefs.edit(), preferences)
                .putString(KEY_NAME, name)
                .putBoolean(KEY_NO_ALLERGIES, noAllergies && preferences.allergies.isEmpty())
                .putBoolean(KEY_SETUP_DONE, true)
                .apply();
    }

    /** Removes the whole food profile (other keys in the file, like Zero-Waste, stay). */
    public void clear() {
        prefs.edit()
                .remove(KEY_ALLERGIES)
                .remove(KEY_HEALTH)
                .remove(KEY_DIET)
                .remove(KEY_FITNESS_GOAL)
                .remove(KEY_NAME)
                .remove(KEY_NO_ALLERGIES)
                .remove(KEY_SETUP_DONE)
                .apply();
    }

    @NonNull
    private static SharedPreferences.Editor putPreferences(@NonNull SharedPreferences.Editor editor,
                                                           @NonNull AiPreferences preferences) {
        return editor
                .putString(KEY_ALLERGIES, String.join(ALLERGY_SEPARATOR, preferences.allergies))
                .putString(KEY_HEALTH, String.join(HEALTH_SEPARATOR, preferences.health))
                .putString(KEY_DIET, preferences.diet)
                .putString(KEY_FITNESS_GOAL, preferences.fitnessGoal);
    }

    @NonNull
    private static List<String> split(@NonNull String saved, @NonNull String separator) {
        return saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(separator)));
    }
}