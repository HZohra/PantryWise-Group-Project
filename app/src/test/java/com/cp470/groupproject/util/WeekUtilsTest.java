package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Tests the week arithmetic of the meal planner. */
public class WeekUtilsTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static long at(ZoneId zone, int y, int m, int d, int h, int min) {
        return ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli();
    }

    @Test
    public void startOfDay_isMidnightLocal() {
        assertEquals(at(UTC, 2026, 11, 4, 0, 0),
                WeekUtils.startOfDay(at(UTC, 2026, 11, 4, 15, 30), UTC));
    }

    @Test
    public void startOfWeek_mondayFirst() {
        assertEquals(at(UTC, 2026, 11, 2, 0, 0),
                WeekUtils.startOfWeek(at(UTC, 2026, 11, 4, 12, 0), UTC, DayOfWeek.MONDAY));
    }

    @Test
    public void startOfWeek_sundayFirst() {
        assertEquals(at(UTC, 2026, 11, 1, 0, 0),
                WeekUtils.startOfWeek(at(UTC, 2026, 11, 4, 12, 0), UTC, DayOfWeek.SUNDAY));
    }

    @Test
    public void startOfWeek_sameDayWhenAlreadyFirst() {
        assertEquals(at(UTC, 2026, 11, 2, 0, 0),
                WeekUtils.startOfWeek(at(UTC, 2026, 11, 2, 18, 45), UTC, DayOfWeek.MONDAY));
    }

    @Test
    public void endOfWeek_isNextWeekMinusOneMs() {
        long start = at(UTC, 2026, 11, 2, 0, 0);
        long nextWeek = WeekUtils.startOfWeek(at(UTC, 2026, 11, 9, 10, 0), UTC, DayOfWeek.MONDAY);

        assertEquals(nextWeek, WeekUtils.endOfWeek(start, UTC) + 1);
    }

    @Test
    public void plusDays_acrossMonthAndYear() {
        assertEquals(at(UTC, 2027, 1, 4, 0, 0),
                WeekUtils.plusDays(at(UTC, 2026, 12, 30, 0, 0), 5, UTC));
    }

    @Test
    public void plusDays_acrossDstChange() {
        // toronto clocks jump forward at 02:00 on 2026-03-08, so that day only has 23 hours
        ZoneId toronto = ZoneId.of("America/Toronto");
        long sunday = at(toronto, 2026, 3, 8, 0, 0);
        long monday = at(toronto, 2026, 3, 9, 0, 0);

        assertEquals(monday, WeekUtils.plusDays(sunday, 1, toronto));
        assertEquals(23 * 3_600_000L, monday - sunday);
    }
}
