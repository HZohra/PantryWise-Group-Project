package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.cp470.groupproject.data.entity.StorageLocation;

import java.util.List;

/**
 * Data access for {@link StorageLocation}.items are filtered by location through
 * FoodItemDao.observeByLocation.
 */

@Dao
public interface StorageLocationDao {
    /**
     * inserts a location, a duplicate name is ignored.
     *
     * @return the new id, or -1 if ignored
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(StorageLocation location);

    @Update
    int update(StorageLocation location);

    @Delete
    int delete(StorageLocation location);

    /** observable list of all locations in id order */
    @Query("SELECT * FROM storage_locations ORDER BY id")
    LiveData<List<StorageLocation>> observeAll();

    /** snapshot of all locations. background thread only */
    @Query("SELECT * FROM storage_locations ORDER BY id")
    List<StorageLocation> getAllSync();

    /** observable single location by id */
    @Query("SELECT * FROM storage_locations WHERE id = :id")
    LiveData<StorageLocation> observeById(long id);
}