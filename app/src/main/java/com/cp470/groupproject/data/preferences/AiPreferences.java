package com.cp470.groupproject.data.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.PreferencesDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The user's food preferences for the AI features (Team Member 5): allergies, "must be" labels,
 * diet and fitness goal. Every value is cleaned with the SAME rules as the backend
 * (backend/src/middleware/validate.js), because one invalid value makes the backend reject the
 * whole request. Immutable; plain Java, so every rule is unit-tested.
 */
public final class AiPreferences {

    /** Health labels the backend accepts (HEALTH_LABELS), in the order the screen shows them. */
    public static final List<String> HEALTH_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            "vegetarian", "vegan", "pescatarian", "gluten-free", "dairy-free", "egg-free",
            "peanut-free", "tree-nut-free", "soy-free", "fish-free", "shellfish-free",
            "pork-free", "red-meat-free", "alcohol-free", "kosher"));

    /** Diets the backend accepts (DIETS). */
    public static final List<String> DIET_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            "balanced", "high-fiber", "high-protein", "low-carb", "low-fat", "low-sodium"));

    /** Fitness goals the backend accepts (FITNESS_GOALS). */
    public static final List<String> GOAL_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            PreferencesDto.GOAL_LOSE_WEIGHT, PreferencesDto.GOAL_BUILD_MUSCLE,
            PreferencesDto.GOAL_MAINTAIN_WEIGHT, PreferencesDto.GOAL_EAT_HEALTHIER));

    /** Backend LIMITS.allergiesMax and allergyMax. */
    public static final int MAX_ALLERGIES = 20;
    public static final int MAX_ALLERGY_LENGTH = 50;

    /** Backend ALLERGY_PATTERN: letters in any alphabet, spaces, hyphens, apostrophes ("tree nuts", "maní"). */
    private static final Pattern ALLERGY_PATTERN = Pattern.compile("^\\p{L}[\\p{L}\\s'-]*$");

    @NonNull public final List<String> allergies;
    @NonNull public final List<String> health;
    @Nullable public final String diet;
    @Nullable public final String fitnessGoal;

    /**
     * Cleans every value: invalid allergies and unknown labels, diets or goals are dropped,
     * duplicates removed, at most {@link #MAX_ALLERGIES} allergies kept.
     */
    public AiPreferences(@Nullable List<String> allergies, @Nullable List<String> health,
                         @Nullable String diet, @Nullable String fitnessGoal) {
        this.allergies = Collections.unmodifiableList(cleanAllergies(allergies));
        this.health = Collections.unmodifiableList(cleanFromList(health, HEALTH_OPTIONS));
        this.diet = pick(diet, DIET_OPTIONS);
        this.fitnessGoal = pick(fitnessGoal, GOAL_OPTIONS);
    }

    @NonNull
    public static AiPreferences empty() {
        return new AiPreferences(null, null, null, null);
    }

    /** True when nothing is set; requests then carry no preferences at all. */
    public boolean isEmpty() {
        return allergies.isEmpty() && health.isEmpty() && diet == null && fitnessGoal == null;
    }

    /**
     * One allergy as the backend expects it: lowercase, single spaces, letters only.
     * @return the cleaned text, or null when it isn't valid (e.g. "peanut2", "<b>", too long)
     */
    @Nullable
    public static String cleanAllergy(@Nullable String raw) {
        if (raw == null) return null;
        String clean = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (clean.isEmpty() || clean.length() > MAX_ALLERGY_LENGTH) return null;
        return ALLERGY_PATTERN.matcher(clean).matches() ? clean : null;
    }

    /** The same preferences plus one more allergy (unchanged if it is invalid, a duplicate or over the limit). */
    @NonNull
    public AiPreferences withAllergy(@Nullable String raw) {
        List<String> more = new ArrayList<>(allergies);
        more.add(raw);
        return new AiPreferences(more, health, diet, fitnessGoal);
    }

    /** The same preferences without this allergy. */
    @NonNull
    public AiPreferences withoutAllergy(@NonNull String allergy) {
        List<String> fewer = new ArrayList<>(allergies);
        fewer.remove(allergy);
        return new AiPreferences(fewer, health, diet, fitnessGoal);
    }

    /**
     * What is sent to the backend; null when nothing is set (the request then looks exactly like
     * before preferences existed). Empty lists are left out, so Gson doesn't send them.
     */
    @Nullable
    public PreferencesDto toDto() {
        if (isEmpty()) return null;
        PreferencesDto dto = new PreferencesDto();
        dto.allergies = allergies.isEmpty() ? null : new ArrayList<>(allergies);
        dto.health = health.isEmpty() ? null : new ArrayList<>(health);
        dto.diet = diet;
        dto.fitnessGoal = fitnessGoal;
        return dto;
    }

    // ---------------------------------------------------------------- cleaning

    @NonNull
    private static List<String> cleanAllergies(@Nullable List<String> raw) {
        Set<String> clean = new LinkedHashSet<>();
        if (raw != null) {
            for (String entry : raw) {
                String allergy = cleanAllergy(entry);
                if (allergy != null) clean.add(allergy);
                if (clean.size() == MAX_ALLERGIES) break;
            }
        }
        return new ArrayList<>(clean);
    }

    /** Keeps known values only, in the order of {@code allowed}, without duplicates. */
    @NonNull
    private static List<String> cleanFromList(@Nullable List<String> raw, @NonNull List<String> allowed) {
        List<String> clean = new ArrayList<>();
        if (raw == null) return clean;
        Set<String> wanted = new LinkedHashSet<>();
        for (String value : raw) {
            if (value != null) wanted.add(value.trim().toLowerCase(Locale.ROOT));
        }
        for (String option : allowed) {
            if (wanted.contains(option)) clean.add(option);
        }
        return clean;
    }

    @Nullable
    private static String pick(@Nullable String value, @NonNull List<String> allowed) {
        if (value == null) return null;
        String clean = value.trim().toLowerCase(Locale.ROOT);
        return allowed.contains(clean) ? clean : null;
    }
}