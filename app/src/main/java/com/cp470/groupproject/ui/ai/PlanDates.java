package com.cp470.groupproject.ui.ai;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Date conversions for planning a meal. The Material date picker works with UTC midnight, but the
 * meal plan stores the start of the day in the phone's time zone; without this, a meal planned for
 * Thursday in Kitchener (UTC-5) would be saved on Wednesday evening. Plain Java, unit-tested.
 */
final class PlanDates {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private PlanDates() { }

    /** @param utcDate a date picker selection (UTC midnight) @return that day's local start of day */
    static long localStartOfDay(long utcDate, TimeZone zone) {
        Calendar utc = Calendar.getInstance(UTC);
        utc.setTimeInMillis(utcDate);
        Calendar local = Calendar.getInstance(zone);
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return local.getTimeInMillis();
    }

    /** @return today in the phone's time zone, as the date picker expects it (UTC midnight) */
    static long todayAsUtcDate(long now, TimeZone zone) {
        Calendar local = Calendar.getInstance(zone);
        local.setTimeInMillis(now);
        Calendar utc = Calendar.getInstance(UTC);
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return utc.getTimeInMillis();
    }
}