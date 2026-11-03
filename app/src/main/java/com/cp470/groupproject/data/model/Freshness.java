package com.cp470.groupproject.data.model;

/** Freshness bucket of an inventory item, used by the fresh, use soon and expired filters. */
public enum Freshness {
    /** expires after the "use soon" window */
    FRESH,
    /** expires within the "use soon" window (3 days by default) */
    USE_SOON,
    /** the expiration date has passed */
    EXPIRED,
    /** no expiration date recorded (queries treat it as fresh) */
    NO_DATE
}