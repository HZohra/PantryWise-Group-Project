package com.cp470.groupproject.data.remote.dto;

/** One ingredient of an AI recipe. The flags are decided by the backend from the pantry that was sent. */
public class AiRecipeIngredientDto {
    /** English, pantry wording, e.g. "spinach". */
    public String name;
    /** In the app language, e.g. "1 cup" / "1 taza"; may be "". */
    public String amount;
    public boolean inPantry;
    /** One of the pantry items expiring within 3 days (highlighted on screen). */
    public boolean expiring;
    /** Salt, pepper, water…: never shown as "Missing". */
    public boolean staple;
}