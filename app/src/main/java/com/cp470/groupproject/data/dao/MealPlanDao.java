package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.MealPlanIngredient;
import com.cp470.groupproject.data.model.MealPlanWithIngredients;

import java.util.List;

/** Data access for the meal plan. writes must run off the main thread. */
@Dao
public interface MealPlanDao {
    /**
     * adds a planned meal
     * @return the new id
     */
    @Insert
    long insert(MealPlan plan);

    @Update
    int update(MealPlan plan);

    @Delete
    int delete(MealPlan plan);

    /** marks a planned meal as cooked or not cooked */
    @Query("UPDATE meal_plans SET completed = :completed WHERE id = :id")
    int setCompleted(long id, boolean completed);

    /**
     * adds the ingredient rows of a meal (their {@code mealPlanId} must already be set).
     * background thread only
     */
    @Insert
    void insertIngredients(List<MealPlanIngredient> ingredients);

    /**
     * removes all the ingredient rows of one meal, before saving its new list. background thread only
     *
     * @return rows deleted
     */
    @Query("DELETE FROM meal_plan_ingredients WHERE mealPlanId = :mealPlanId")
    int deleteIngredients(long mealPlanId);

    /**
     * planned meals between two days (both included), by day and then by meal of the day, each one
     * with its ingredients. {@code @Transaction} because room runs a second query for the children.
     */
    @Transaction
    @Query("SELECT * FROM meal_plans WHERE planDate BETWEEN :from AND :to ORDER BY planDate ASC, "
            + "CASE mealType WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END")
    LiveData<List<MealPlanWithIngredients>> observeBetween(long from, long to);

    /** synchronous version of {@link #observeBetween(long, long)}. background thread only */
    @Transaction
    @Query("SELECT * FROM meal_plans WHERE planDate BETWEEN :from AND :to ORDER BY planDate ASC, "
            + "CASE mealType WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END")
    List<MealPlanWithIngredients> getBetweenSync(long from, long to);

    /** removes the plans of days before {@code before} */
    @Query("DELETE FROM meal_plans WHERE planDate < :before")
    int deleteBefore(long before);
}