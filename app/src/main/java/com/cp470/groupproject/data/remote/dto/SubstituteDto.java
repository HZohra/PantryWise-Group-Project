package com.cp470.groupproject.data.remote.dto;

/** One suggested replacement. "name" is English (pantry wording); amount and notes follow the app language. */
public class SubstituteDto {
    public String name;
    public String amount;
    public String notes;
    /** Decided by the backend from the pantry that was sent. */
    public boolean inPantry;
}