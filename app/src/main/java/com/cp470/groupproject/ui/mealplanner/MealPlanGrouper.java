package com.cp470.groupproject.ui.mealplanner;

import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.util.WeekUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Distributes the meals of a week over its seven days. plain java, so it is tested on the jvm. */
public final class MealPlanGrouper {

    private MealPlanGrouper() { }

    /**
     * groups the plans by day. a plan belongs to the day that contains its date, and when a slot
     * has several plans the newest one wins.
     *
     * @param plans plans read from the database
     * @param weekStartMs start of the first day of the week
     * @param zone time zone of the calendar
     * @return always 7 consecutive days starting at {@code weekStartMs}
     */
    public static List<DayPlan> group(List<MealPlan> plans, long weekStartMs, ZoneId zone) {
        List<DayPlan> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            long dayStart = WeekUtils.plusDays(weekStartMs, i, zone);
            Map<String, MealPlan> meals = new HashMap<>();
            if (plans != null) {
                for (MealPlan plan : plans) {
                    if (WeekUtils.startOfDay(plan.planDate, zone) != dayStart) continue;
                    MealPlan current = meals.get(plan.mealType);
                    if (current == null || plan.createdAt > current.createdAt) {
                        meals.put(plan.mealType, plan);
                    }
                }
            }
            days.add(new DayPlan(dayStart, meals));
        }
        return days;
    }
}
