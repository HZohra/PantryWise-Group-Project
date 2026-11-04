package com.cp470.groupproject.util;

import com.cp470.groupproject.data.model.Freshness;

/** Date arithmetic and freshness classification. plain java, no android classes. */

public final class ExpiryUtils {
    /** milliseconds in one day */
    public static final long DAY_MS = 24L * 60 * 60 * 1000;

    /** default "use soon" window in days */
    public static final int DEFAULT_USE_SOON_DAYS = 3;

    private ExpiryUtils() { }

    /** @return {@code now} plus {@code days} days, in milliseconds */
    public static long useSoonLimit(long now, int days) {
        return now + days * DAY_MS;
    }

    /**
     * classifies an expiration date. the boundaries match the dao queries exactly:
     * expired if before now, use soon if inside the window (limits included), otherwise fresh
     */
    public static Freshness classify(long expirationDate, long now, int useSoonDays) {
        if (expirationDate <= 0) return Freshness.NO_DATE;
        if (expirationDate < now) return Freshness.EXPIRED;
        if (expirationDate <= useSoonLimit(now, useSoonDays)) return Freshness.USE_SOON;
        return Freshness.FRESH;
    }

    /** @return whole days from now until the expiry (negative if already expired, rounded down) */
    public static int daysUntil(long expirationDate, long now) {
        return (int) Math.floor((expirationDate - now) / (double) DAY_MS);
    }
}