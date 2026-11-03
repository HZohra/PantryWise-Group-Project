package com.cp470.groupproject.data.dao;

/** Shared sql fragments so every inventory query uses the same filter and ordering. */
public final class FoodSql {
    /** only items that are still in the inventory */
    public static final String ACTIVE = "status = 'ACTIVE'";

    /** soonest expiry first, items without a date last, then alphabetical */
    public static final String ORDER =
            " ORDER BY CASE WHEN expirationDate = 0 THEN 1 ELSE 0 END, expirationDate ASC, name COLLATE NOCASE ASC";

    private FoodSql() { }
}