package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Tests the calendar day arithmetic used by the expiration alert texts. */
public class ExpirationTimingTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static long at(ZoneId zone, int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
                .toInstant().toEpochMilli();
    }

    @Test
    public void calendarDaysUntil_sameDay_isZero() {
        long now = at(UTC, 2026, 11, 4, 10, 0);
        long expiry = at(UTC, 2026, 11, 4, 23, 59);

        assertEquals(0, ExpirationTiming.calendarDaysUntil(expiry, now, UTC));
    }

    @Test
    public void calendarDaysUntil_nextDay_isOneEvenIfOnlyMinutesApart() {
        long now = at(UTC, 2026, 11, 4, 23, 50);
        long expiry = at(UTC, 2026, 11, 5, 0, 10);

        assertEquals(1, ExpirationTiming.calendarDaysUntil(expiry, now, UTC));
    }

    @Test
    public void calendarDaysUntil_past_isNegative() {
        long now = at(UTC, 2026, 11, 4, 10, 0);

        assertEquals(-1, ExpirationTiming.calendarDaysUntil(at(UTC, 2026, 11, 3, 8, 0), now, UTC));
        assertEquals(-3, ExpirationTiming.calendarDaysUntil(at(UTC, 2026, 11, 1, 20, 0), now, UTC));
    }

    @Test
    public void calendarDaysUntil_respectsTimeZone() {
        // 2026-11-05 02:00 UTC is still Nov 4 (22:00) in Toronto but already Nov 5 in UTC
        long now = at(UTC, 2026, 11, 4, 12, 0);
        long expiry = at(UTC, 2026, 11, 5, 2, 0);
        ZoneId toronto = ZoneId.of("America/Toronto");

        assertEquals(1, ExpirationTiming.calendarDaysUntil(expiry, now, UTC));
        assertEquals(0, ExpirationTiming.calendarDaysUntil(expiry, now, toronto));
    }

    private static final long HOUR_MS = 3_600_000L;

    @Test
    public void delayUntilNextHour_beforeTarget() {
        long now = at(UTC, 2026, 11, 4, 8, 0);

        assertEquals(HOUR_MS, ExpirationTiming.delayUntilNextHour(now, 9, UTC));
    }

    @Test
    public void delayUntilNextHour_exactlyAtTarget_isNextDay() {
        long now = at(UTC, 2026, 11, 4, 9, 0);

        assertEquals(24 * HOUR_MS, ExpirationTiming.delayUntilNextHour(now, 9, UTC));
    }

    @Test
    public void delayUntilNextHour_afterTarget() {
        long now = at(UTC, 2026, 11, 4, 10, 0);

        assertEquals(23 * HOUR_MS, ExpirationTiming.delayUntilNextHour(now, 9, UTC));
    }

    @Test
    public void delayUntilNextHour_justBeforeMidnight() {
        long now = at(UTC, 2026, 11, 4, 23, 59);

        assertEquals(9 * HOUR_MS + 60_000L, ExpirationTiming.delayUntilNextHour(now, 9, UTC));
    }

    @Test
    public void delayUntilNextHour_dstSpringForward() {
        // toronto clocks jump forward 1 h on 2026-03-08: from 10:00 to the next 09:00 the wall
        // clock shows 23 h but only 22 real hours pass (a naive "+24 h" would be wrong)
        ZoneId toronto = ZoneId.of("America/Toronto");
        long now = at(toronto, 2026, 3, 7, 10, 0);

        assertEquals(22 * HOUR_MS, ExpirationTiming.delayUntilNextHour(now, 9, toronto));
    }
}
