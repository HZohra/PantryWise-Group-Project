package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

/** The date picker's UTC midnight vs the plan's local start of day (the "day shifts" trap). */
public class PlanDatesTest {

    private static final TimeZone TORONTO = TimeZone.getTimeZone("America/Toronto");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long at(TimeZone zone, int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(zone);
        c.clear();
        c.set(year, month, day, hour, minute, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void pickedThursday_isSavedAsThursdayMidnightInToronto() {
        long pickerValue = at(UTC, 2026, Calendar.NOVEMBER, 12, 0, 0);

        assertEquals(at(TORONTO, 2026, Calendar.NOVEMBER, 12, 0, 0),
                PlanDates.localStartOfDay(pickerValue, TORONTO));
    }

    @Test
    public void lateEveningInToronto_todayIsStillTheLocalDate() {
        long now = at(TORONTO, 2026, Calendar.NOVEMBER, 10, 23, 30); // already Nov 11 in UTC

        assertEquals(at(UTC, 2026, Calendar.NOVEMBER, 10, 0, 0), PlanDates.todayAsUtcDate(now, TORONTO));
    }

    @Test
    public void roundTrip_todayThenBack_isLocalMidnightToday() {
        long now = at(TORONTO, 2026, Calendar.NOVEMBER, 10, 9, 0);

        long saved = PlanDates.localStartOfDay(PlanDates.todayAsUtcDate(now, TORONTO), TORONTO);

        assertEquals(at(TORONTO, 2026, Calendar.NOVEMBER, 10, 0, 0), saved);
    }
}