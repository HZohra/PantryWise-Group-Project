package com.cp470.groupproject.data.remote.dto;

/** A pantry item a recipe would use up before it spoils: {"name":"spinach","daysUntilExpiry":1} */
public class ExpiringItemDto {
    public String name;
    /** 0 = today, 1 = tomorrow … up to 3 (the backend's "expiring soon" window). */
    public int daysUntilExpiry;
}