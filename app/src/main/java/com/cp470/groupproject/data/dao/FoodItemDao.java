package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.model.StockSummary;

import java.util.List;

/**
 * Data access for {@link FoodItem}.
 *
 * methods returning {@link LiveData} are observable and safe to call from the main thread.
 * methods that write or return plain values are synchronous and must be called off the main thread
 * (the repository takes care of that).
 *
 * the freshness filters split the active inventory without gaps or overlaps:
 *   - expired: 0 < expirationDate < now
 *   - use soon: now < expirationDate < useSoonLimit
 *   - fresh: expirationDate = 0 (unknown) or expirationDate > useSoonLimit
 */

@Dao
public interface FoodItemDao {

    // ------------------------------------------------------------------ crud

    /** inserts an item. @return the new row id */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(FoodItem item);

    /** inserts several items in one transaction. @return the new row ids */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertAll(List<FoodItem> items);

    /** updates an existing item matched by primary key. @return rows updated (0 or 1) */
    @Update
    int update(FoodItem item);

    /** permanently deletes an item. @return rows deleted (0 or 1) */
    @Delete
    int delete(FoodItem item);

    /** changes only the status column (used when recording a disposition) */
    @Query("UPDATE food_items SET status = :status WHERE id = :id")
    int updateStatus(long id, String status);

    /** @return the item with this id regardless of its status, or null */
    @Query("SELECT * FROM food_items WHERE id = :id")
    FoodItem getById(long id);

    /** observable version of {@link #getById(long)} */
    @Query("SELECT * FROM food_items WHERE id = :id")
    LiveData<FoodItem> observeById(long id);

    /** @return the active item with this barcode, or null (avoids duplicates after a scan) */
    @Query("SELECT * FROM food_items WHERE barcode = :barcode AND " + FoodSql.ACTIVE + " LIMIT 1")
    FoodItem findActiveByBarcode(String barcode);





    // --------------------------------------------------------- observable lists

    /** all active items, soonest expiry first */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE + FoodSql.ORDER)
    LiveData<List<FoodItem>> observeActive();

    /** active items stored in one location (fridge, freezer or pantry) */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND storageLocationId = :locationId" + FoodSql.ORDER)
    LiveData<List<FoodItem>> observeByLocation(long locationId);

    /** active items of one category */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND categoryId = :categoryId" + FoodSql.ORDER)
    LiveData<List<FoodItem>> observeByCategory(long categoryId);

    /** "fresh" filter: no date, or expires after {@code useSoonLimit} */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND (expirationDate = 0 OR expirationDate > :useSoonLimit)" + FoodSql.ORDER)
    LiveData<List<FoodItem>> observeFresh(long useSoonLimit);

    /** "use soon" filter: expires between {@code now} and {@code useSoonLimit}, both included */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND expirationDate > 0 AND expirationDate >= :now AND expirationDate <= :useSoonLimit"
            + FoodSql.ORDER)
    LiveData<List<FoodItem>> observeUseSoon(long now, long useSoonLimit);

    /** "expired" filter: a date exists and it is before {@code now} */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND expirationDate > 0 AND expirationDate < :now" + FoodSql.ORDER)
    LiveData<List<FoodItem>> observeExpired(long now);







    // ----------------------------------------- synchronous (recipes, shopping)

    /** snapshot of the whole active inventory. background thread only */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE + FoodSql.ORDER)
    List<FoodItem> getActiveSync();

    /**
     * items that are still good but expire between {@code now} and {@code useSoonLimit}
     * used by the zero-waste recipe suggestions (it uses the expirationDate index)
     */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND expirationDate > 0 AND expirationDate >= :now AND expirationDate <= :useSoonLimit"
            + FoodSql.ORDER)
    List<FoodItem> getExpiringSoonSync(long now, long useSoonLimit);

    /** active items whose normalized name is exactly {@code normalizedName} */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND normalizedName = :normalizedName" + FoodSql.ORDER)
    List<FoodItem> findActiveByNormalizedName(String normalizedName);

    /** active items whose normalized name contains {@code term} (so "milk" finds "oat milk") */
    @Query("SELECT * FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND normalizedName LIKE '%' || :term || '%'" + FoodSql.ORDER)
    List<FoodItem> searchActiveByName(String term);

    /**
     * total stock per ingredient and unit, ignoring expired items
     * lets the shopping list compute needed quantity minus available quantity
     */
    @Query("SELECT normalizedName AS normalizedName, unit AS unit, SUM(quantity) AS totalQuantity "
            + "FROM food_items WHERE " + FoodSql.ACTIVE
            + " AND (expirationDate = 0 OR expirationDate >= :now) "
            + "GROUP BY normalizedName, unit ORDER BY normalizedName")
    List<StockSummary> getStockSummarySync(long now);

    /** @return the number of active items */
    @Query("SELECT COUNT(*) FROM food_items WHERE " + FoodSql.ACTIVE)
    int countActive();
}