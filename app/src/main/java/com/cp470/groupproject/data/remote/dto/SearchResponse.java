package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/** Response of GET /api/v1/recipes/search (recipes have no "match"). */
public class SearchResponse {
    public int count;
    public List<RecipeDto> recipes;
}