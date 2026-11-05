package com.cp470.groupproject.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calendar arithmetic for expiration reminders. plain java (no android classes), so it can be
 * unit tested on the jvm.
 */

public final class ExpirationTiming {

    private ExpirationTiming() { }

    /**
     * whole calendar days between today and an expiration date.
     * it compares dates, not 24 hour blocks: at 23:50, something expiring at 00:10 tomorrow is
     * "tomorrow" (1), not "today". {@link ExpiryUtils#daysUntil} counts exact 24 hour blocks instead.
     *
     * @param expirationMs expiration date in milliseconds
     * @param nowMs current time in milliseconds
     * @param zone time zone used to decide where a day starts and ends
     * @return 0 for the same day, positive for the future, negative if already past
     */
    public static int calendarDaysUntil(long expirationMs, long nowMs, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate();
        LocalDate expiry = Instant.ofEpochMilli(expirationMs).atZone(zone).toLocalDate();
        return (int) ChronoUnit.DAYS.between(today, expiry);
    }

    /**
     * time from now until the next occurrence of {@code hour}:00.
     * exactly at that hour counts as "already passed", so the answer is then a full day. it adds a
     * calendar day (not 24 hours), so daylight saving changes are respected.
     *
     * @param nowMs current time in milliseconds
     * @param hour hour of the day, 0 to 23
     * @param zone time zone in which the hour is read
     * @return real milliseconds to wait, always greater than zero
     */
    public static long delayUntilNextHour(long nowMs, int hour, ZoneId zone) {
        ZonedDateTime now = Instant.ofEpochMilli(nowMs).atZone(zone);
        ZonedDateTime next = now.toLocalDate().atTime(hour, 0).atZone(zone);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).toMillis();
    }
}
