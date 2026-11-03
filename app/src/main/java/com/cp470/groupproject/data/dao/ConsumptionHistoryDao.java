package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.cp470.groupproject.data.entity.ConsumptionHistory;
import com.cp470.groupproject.data.model.DispositionCount;

import java.util.List;

/** Data access for the consumption and waste log. */
@Dao
public interface ConsumptionHistoryDao {
    /** appends a log entry. @return the new id */
    @Insert
    long insert(ConsumptionHistory entry);

    /** full log, newest first */
    @Query("SELECT * FROM consumption_history ORDER BY timestamp DESC")
    LiveData<List<ConsumptionHistory>> observeAll();

    /** entries between {@code from} and {@code to}, newest first. background thread only */
    @Query("SELECT * FROM consumption_history WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    List<ConsumptionHistory> getBetweenSync(long from, long to);

    /** totals per disposition since {@code since} (0 = all time), feeds the waste screen */
    @Query("SELECT disposition AS disposition, COUNT(*) AS itemCount, IFNULL(SUM(quantity), 0) AS totalQuantity "
            + "FROM consumption_history WHERE timestamp >= :since GROUP BY disposition")
    LiveData<List<DispositionCount>> observeCountsSince(long since);

    /** synchronous version of {@link #observeCountsSince(long)} */
    @Query("SELECT disposition AS disposition, COUNT(*) AS itemCount, IFNULL(SUM(quantity), 0) AS totalQuantity "
            + "FROM consumption_history WHERE timestamp >= :since GROUP BY disposition")
    List<DispositionCount> getCountsSinceSync(long since);

    /** erases the whole log. @return rows deleted */
    @Query("DELETE FROM consumption_history")
    int deleteAll();
}