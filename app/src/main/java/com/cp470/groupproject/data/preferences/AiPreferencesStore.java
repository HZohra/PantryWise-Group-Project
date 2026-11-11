package com.cp470.groupproject.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Saves the AI food preferences on the phone only (no account, nothing on a server).
 * Uses Member 5's own file "pantrywise_ai" (shared with the Zero-Waste switch, different keys);
 * the team's display settings live in another file ("PantryPreferences") and are never touched.
 */
public class AiPreferencesStore {

    /** Same file as RecipeViewModel.PREFS_NAME. */
    public static final String FILE_NAME = "pantrywise_ai";

    static final String KEY_ALLERGIES = "pref_allergies";
    static final String KEY_HEALTH = "pref_health";
    static final String KEY_DIET = "pref_diet";
    static final String KEY_FITNESS_GOAL = "pref_fitness_goal";

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

    /** The saved preferences, cleaned again on load (a value saved by an older version can't break requests). */
    @NonNull
    public AiPreferences load() {
        return new AiPreferences(
                split(prefs.getString(KEY_ALLERGIES, ""), ALLERGY_SEPARATOR),
                split(prefs.getString(KEY_HEALTH, ""), HEALTH_SEPARATOR),
                prefs.getString(KEY_DIET, null),
                prefs.getString(KEY_FITNESS_GOAL, null));
    }

    /** Saves in the background (apply); the next request reads the new values. */
    public void save(@NonNull AiPreferences preferences) {
        prefs.edit()
                .putString(KEY_ALLERGIES, String.join(ALLERGY_SEPARATOR, preferences.allergies))
                .putString(KEY_HEALTH, String.join(HEALTH_SEPARATOR, preferences.health))
                .putString(KEY_DIET, preferences.diet)
                .putString(KEY_FITNESS_GOAL, preferences.fitnessGoal)
                .apply();
    }

    /** Removes the food preferences only (other keys in the file, like Zero-Waste, stay). */
    public void clear() {
        prefs.edit()
                .remove(KEY_ALLERGIES)
                .remove(KEY_HEALTH)
                .remove(KEY_DIET)
                .remove(KEY_FITNESS_GOAL)
                .apply();
    }

    @NonNull
    private static List<String> split(@NonNull String saved, @NonNull String separator) {
        return saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(separator)));
    }
}