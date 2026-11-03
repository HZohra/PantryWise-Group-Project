package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import com.cp470.groupproject.data.model.DispositionCount;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.data.model.WasteStats;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** plain jvm test of the waste aggregation logic */
public class WasteStatsCalculatorTest {

    @Test
    public void aggregatesCountsAndPercentages() {
        WasteStats s = WasteStatsCalculator.fromCounts(Arrays.asList(
                new DispositionCount(ItemStatus.CONSUMED, 7, 7),
                new DispositionCount(ItemStatus.WASTED, 2, 2),
                new DispositionCount(ItemStatus.EXPIRED, 1, 1)));
        assertEquals(10, s.totalCount);
        assertEquals(3, s.getWasteCount());
        assertEquals(30.0, s.getWastePercent(), 0.0001);
        assertEquals(70.0, s.getConsumedPercent(), 0.0001);
    }

    @Test
    public void emptyAndNullGiveZeroWithoutDividingByZero() {
        assertEquals(0.0, WasteStatsCalculator.fromCounts(Collections.emptyList()).getWastePercent(), 0.0);
        assertEquals(0, WasteStatsCalculator.fromCounts(null).totalCount);
    }

    @Test
    public void unknownDispositionsAreIgnored() {
        WasteStats s = WasteStatsCalculator.fromCounts(Arrays.asList(
                new DispositionCount("ACTIVE", 5, 5),
                new DispositionCount(ItemStatus.DONATED, 1, 1)));
        assertEquals(1, s.totalCount);
        assertEquals(1, s.donatedCount);
    }
}