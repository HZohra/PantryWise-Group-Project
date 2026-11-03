package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.cp470.groupproject.data.entity.Category;

import java.util.List;

/** Data access for {@link Category}. writes must run off the main thread. */

@Dao
public interface CategoryDao {
    /** inserts a category, a duplicate name is ignored. @return the new id, or -1 if ignored */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Category category);

    @Update
    int update(Category category);

    @Delete
    int delete(Category category);

    /** observable list of all categories in id order */
    @Query("SELECT * FROM categories ORDER BY id")
    LiveData<List<Category>> observeAll();

    /** snapshot of all categories. background thread only */
    @Query("SELECT * FROM categories ORDER BY id")
    List<Category> getAllSync();

    /** observable single category by id */
    @Query("SELECT * FROM categories WHERE id = :id")
    LiveData<Category> observeById(long id);

    /** @return the category with this key, or null */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    Category getByName(String name);
}