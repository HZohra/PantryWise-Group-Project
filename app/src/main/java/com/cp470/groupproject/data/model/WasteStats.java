package com.cp470.groupproject.data.model;

/** Immutable aggregate of the waste metrics shown on the waste summary screen. */
public final class WasteStats {
    public final int totalCount;
    public final int consumedCount;
    public final int wastedCount;
    public final int expiredCount;
    public final int donatedCount;

    public WasteStats(int consumed, int wasted, int expired, int donated) {
        this.consumedCount = consumed;
        this.wastedCount = wasted;
        this.expiredCount = expired;
        this.donatedCount = donated;
        this.totalCount = consumed + wasted + expired + donated;
    }

    /** @return wasted plus expired entries */
    public int getWasteCount() {
        return wastedCount + expiredCount;
    }

    /** @return share of tracked items that were wasted or expired, from 0 to 100 (0 if nothing is tracked) */
    public double getWastePercent() {
        return totalCount == 0 ? 0.0 : getWasteCount() * 100.0 / totalCount;
    }

    /** @return share of tracked items that were consumed, from 0 to 100 */
    public double getConsumedPercent() {
        return totalCount == 0 ? 0.0 : consumedCount * 100.0 / totalCount;
    }
}