package com.cp470.groupproject.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Date arithmetic for the weekly meal planner. plain java, and always based on calendar days
 * (never on 24 hour blocks) so the days where the clock changes do not break it.
 */
public final class WeekUtils {

    private WeekUtils() { }

    /**
     * @param ms any moment of the day
     * @param zone time zone that decides where the day starts
     * @return 00:00 of that day, in milliseconds
     */
    public static long startOfDay(long ms, ZoneId zone) {
        return Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                .atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /**
     * @param ms any moment
     * @param days calendar days to add (may be negative)
     * @param zone time zone of the calendar
     * @return the start of the day that is {@code days} days later
     */
    public static long plusDays(long ms, int days, ZoneId zone) {
        return Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                .plusDays(days).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /**
     * @param ms any moment of the week
     * @param zone time zone of the calendar
     * @param first the day the week starts on
     * @return 00:00 of the first day of that week
     */
    public static long startOfWeek(long ms, ZoneId zone, DayOfWeek first) {
        return Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(first))
                .atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /**
     * last millisecond of a week. it is "start of next week - 1 ms" because the database query
     * includes both ends, so the first day of the next week must stay out.
     *
     * @param weekStartMs start of the week
     * @param zone time zone of the calendar
     * @return the last millisecond of that week
     */
    public static long endOfWeek(long weekStartMs, ZoneId zone) {
        return plusDays(weekStartMs, 7, zone) - 1;
    }

    /** @return the first day of the week for the device language (Monday in Spain, Sunday in the US) */
    public static DayOfWeek defaultFirstDayOfWeek() {
        return WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
    }
}
