package com.cp470.groupproject.data.model;

import androidx.room.Ignore;

/** Query result row: how many log entries (and how much quantity) per disposition. */
public class DispositionCount {
    /** one of the {@link ItemStatus} disposition constants */
    public String disposition;

    /** number of log entries with this disposition */
    public int itemCount;

    /** sum of the quantities of those entries */
    public double totalQuantity;

    /** empty constructor required by room */
    public DispositionCount() { }

    @Ignore
    public DispositionCount(String disposition, int itemCount, double totalQuantity) {
        this.disposition = disposition;
        this.itemCount = itemCount;
        this.totalQuantity = totalQuantity;
    }
}