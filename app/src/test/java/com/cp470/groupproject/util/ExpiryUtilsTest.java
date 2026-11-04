package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import com.cp470.groupproject.data.model.Freshness;

import org.junit.Test;

/** plain jvm test of the freshness classification and the date arithmetic */
public class ExpiryUtilsTest {
    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = ExpiryUtils.DAY_MS;

    @Test
    public void zeroMeansNoDate() {
        assertEquals(Freshness.NO_DATE, ExpiryUtils.classify(0, NOW, 3));
    }

    @Test
    public void pastDateIsExpired() {
        assertEquals(Freshness.EXPIRED, ExpiryUtils.classify(NOW - 1, NOW, 3));
    }

    @Test
    public void insideWindowIsUseSoonIncludingBothLimits() {
        assertEquals(Freshness.USE_SOON, ExpiryUtils.classify(NOW, NOW, 3));
        assertEquals(Freshness.USE_SOON, ExpiryUtils.classify(NOW + 3 * DAY, NOW, 3));
    }

    @Test
    public void afterWindowIsFresh() {
        assertEquals(Freshness.FRESH, ExpiryUtils.classify(NOW + 3 * DAY + 1, NOW, 3));
    }

    @Test
    public void daysUntilRoundsDown() {
        assertEquals(2, ExpiryUtils.daysUntil(NOW + 2 * DAY + 5000, NOW));
        assertEquals(-1, ExpiryUtils.daysUntil(NOW - 1000, NOW));
    }
}