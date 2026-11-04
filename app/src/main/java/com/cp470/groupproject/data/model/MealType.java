package com.cp470.groupproject.data.model;

/** String constants for the meal of the day a recipe is planned for. */
public final class MealType {
    public static final String BREAKFAST = "BREAKFAST";
    public static final String LUNCH = "LUNCH";
    public static final String DINNER = "DINNER";
    public static final String SNACK = "SNACK";

    /** all meal types in the order they happen during the day */
    public static final String[] ALL = {BREAKFAST, LUNCH, DINNER, SNACK};

    private MealType() { }
}