package com.cp470.groupproject.data.model;

/**
 * Text constantes that describe the status of a given food and the result that gets
 * stored in the consume history.
 *
 * They get stored as text in Room so that the db es legible and the values don't change
 * if an enum gets reordered between versions of the app.
 */
public final class ItemStatus {
    /** Item is in inventory */
    public static final String ACTIVE = "ACTIVE";
    /** Item got consumed */
    public static final String CONSUMED = "CONSUMED";
    /** Item was thrown away before being expired */
    public static final String WASTED = "WASTED";
    /** Item got expired and was thrown away */
    public static final String EXPIRED = "EXPIRED";
    /** Item got donated */
    public static final String DONATED = "DONATED";

    /** The 4 possible results when an item goes out of the inventory */
    public static final String[] DISPOSITIONS = {CONSUMED, WASTED, EXPIRED, DONATED};

    private ItemStatus() { }

    /** @return true if {@code status} is one of the results */
    public static boolean isDisposition(String status) {
        for (String s : DISPOSITIONS) {
            if (s.equals(status)) return true;
        }
        return false;
    }

    /** @return true if the result is waste (WASTED or EXPIRED) */
    public static boolean isWaste(String status) {
        return WASTED.equals(status) || EXPIRED.equals(status);
    }
}