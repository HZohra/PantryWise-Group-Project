package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/** Response of POST /api/v1/recipes/substitute */
public class SubstituteResponse {

    /** Answered from the backend's built-in list (instant, no AI). */
    public static final String SOURCE_LOCAL = "local";
    /** Answered by Claude. */
    public static final String SOURCE_AI = "ai";

    public String ingredient;
    public String source;
    /** Items the user owns come first; may be empty when nothing safe exists. */
    public List<SubstituteDto> substitutes;
    /** One short tip, or "" if none. */
    public String tip;
}