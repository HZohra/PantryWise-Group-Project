package com.cp470.groupproject.ui.mealplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.model.MealType;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tests how meals are placed on the seven days of the planner. */
public class MealPlanGrouperTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    /** monday 2026-11-02 00:00 */
    private static final long WEEK = at(2026, 11, 2);

    private static long at(int y, int m, int d) {
        return ZonedDateTime.of(y, m, d, 0, 0, 0, 0, UTC).toInstant().toEpochMilli();
    }

    private static MealPlan plan(long date, String type, long createdAt) {
        MealPlan plan = new MealPlan(date, type, "id", "Title", new ArrayList<>());
        plan.createdAt = createdAt;
        return plan;
    }

    @Test
    public void group_alwaysReturnsSevenDays() {
        List<DayPlan> days = MealPlanGrouper.group(Collections.emptyList(), WEEK, UTC);

        assertEquals(7, days.size());
        for (DayPlan day : days) {
            assertTrue(day.meals.isEmpty());
        }
        assertEquals(WEEK, days.get(0).dayStartMs);
        assertEquals(at(2026, 11, 8), days.get(6).dayStartMs);
    }

    @Test
    public void group_placesMealOnItsDay() {
        MealPlan wednesday = plan(at(2026, 11, 4), MealType.DINNER, 1);

        List<DayPlan> days = MealPlanGrouper.group(Collections.singletonList(wednesday), WEEK, UTC);

        assertSame(wednesday, days.get(2).get(MealType.DINNER));
    }

    @Test
    public void group_separatesMealTypes() {
        List<MealPlan> plans = Arrays.asList(
                plan(at(2026, 11, 4), MealType.BREAKFAST, 1),
                plan(at(2026, 11, 4), MealType.DINNER, 1));

        DayPlan day = MealPlanGrouper.group(plans, WEEK, UTC).get(2);

        assertEquals(2, day.meals.size());
    }

    @Test
    public void group_duplicateSlot_keepsNewest() {
        MealPlan older = plan(at(2026, 11, 4), MealType.LUNCH, 100);
        MealPlan newer = plan(at(2026, 11, 4), MealType.LUNCH, 200);

        DayPlan day = MealPlanGrouper.group(Arrays.asList(newer, older), WEEK, UTC).get(2);

        assertSame(newer, day.get(MealType.LUNCH));
    }

    @Test
    public void group_ignoresPlansOutsideWeek() {
        MealPlan nextWeek = plan(at(2026, 11, 9), MealType.LUNCH, 1);

        List<DayPlan> days = MealPlanGrouper.group(Collections.singletonList(nextWeek), WEEK, UTC);

        for (DayPlan day : days) {
            assertNull(day.get(MealType.LUNCH));
        }
    }
}
