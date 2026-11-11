package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/** Response of POST /api/v1/recipes/recommend */
public class RecommendResponse {
    /** "standard" or "zeroWaste". */
    public String mode;
    /** false = the pantry was empty, so these are default ideas ("Add food to get personalized suggestions"). */
    public boolean personalized;
    /** true = the first search found nothing and a wider one was used. */
    public boolean broadened;
    /** What was actually searched, e.g. ["spinach","milk"]. */
    public List<String> searchedFor;
    public int count;
    /** Best match first. */
    public List<RecipeDto> recipes;
}