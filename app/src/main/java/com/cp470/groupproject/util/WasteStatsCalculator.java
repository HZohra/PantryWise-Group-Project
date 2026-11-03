package com.cp470.groupproject.util;

import com.cp470.groupproject.data.model.DispositionCount;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.data.model.WasteStats;

import java.util.List;

/** Turns the raw per-disposition query rows into {@link WasteStats}. plain java. */
public final class WasteStatsCalculator {
    private WasteStatsCalculator() { }

    /**
     * @param rows grouped counts from the dao (may be null or empty)
     * @return the statistics, unknown dispositions are ignored
     */
    public static WasteStats fromCounts(List<DispositionCount> rows) {
        int consumed = 0, wasted = 0, expired = 0, donated = 0;
        if (rows != null) {
            for (DispositionCount row : rows) {
                if (row == null || row.disposition == null) continue;
                switch (row.disposition) {
                    case ItemStatus.CONSUMED: consumed += row.itemCount; break;
                    case ItemStatus.WASTED:   wasted   += row.itemCount; break;
                    case ItemStatus.EXPIRED:  expired  += row.itemCount; break;
                    case ItemStatus.DONATED:  donated  += row.itemCount; break;
                    default: break;
                }
            }
        }
        return new WasteStats(consumed, wasted, expired, donated);
    }
}