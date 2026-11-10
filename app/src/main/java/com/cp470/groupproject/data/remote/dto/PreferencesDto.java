package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Optional user preferences sent with AI and recipe requests.
 * Every field is optional: Gson leaves null fields out of the JSON, and the backend then uses its defaults.
 * Allowed values are checked by the backend (validate.js).
 */
public class PreferencesDto {

    // Fitness goals (must match FITNESS_GOALS in backend/src/middleware/validate.js)
    public static final String GOAL_LOSE_WEIGHT = "lose-weight";
    public static final String GOAL_BUILD_MUSCLE = "build-muscle";
    public static final String GOAL_MAINTAIN_WEIGHT = "maintain-weight";
    public static final String GOAL_EAT_HEALTHIER = "eat-healthier";

    /** Edamam diet, e.g. "high-protein". */
    @Nullable public String diet;
    /** Edamam health labels, e.g. "vegetarian", "peanut-free". */
    @Nullable public List<String> health;
    /** Edamam cuisine, e.g. "mexican". */
    @Nullable public String cuisine;
    /** "breakfast", "lunch", "dinner", "snack" or "teatime". */
    @Nullable public String mealType;
    /** Maximum cooking time in minutes (1–600). */
    @Nullable public Integer maxTime;
    /** Free-text allergies, e.g. "peanut", "kiwi". The AI never suggests these. */
    @Nullable public List<String> allergies;
    /** One of the GOAL_* constants. */
    @Nullable public String fitnessGoal;
}