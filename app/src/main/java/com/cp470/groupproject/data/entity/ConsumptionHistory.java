package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Log entry recording what happened to a food item (consumed, wasted, expired or donated).
 *
 * design choice: the entry stores a snapshot (name, quantity, unit, category)instead of a foreign
 * key, so waste statistics stay correct even if the original FoodItem row is deleted later.
 */

@Entity(tableName = "consumption_history",
        indices = {@Index("timestamp"), @Index("disposition")})
public class ConsumptionHistory {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** id the item had in food_items (informational only, not a foreign key) */
    public long foodItemId;

    @NonNull
    public String foodName = "";

    @NonNull
    public String normalizedName = "";

    public double quantity;

    @NonNull
    public String unit = "";

    public long categoryId;

    /** one of the ItemStatus disposition constants */
    @NonNull
    public String disposition = "";

    /** when the disposition was recorded, in milliseconds */
    public long timestamp;

    /** builds a log entry from a food item at the moment it leaves the inventory */
    public static ConsumptionHistory fromFoodItem(@NonNull FoodItem item,
                                                  @NonNull String disposition, long timestamp) {
        ConsumptionHistory h = new ConsumptionHistory();
        h.foodItemId = item.id;
        h.foodName = item.name;
        h.normalizedName = item.normalizedName;
        h.quantity = item.quantity;
        h.unit = item.unit;
        h.categoryId = item.categoryId;
        h.disposition = disposition;
        h.timestamp = timestamp;
        return h;
    }
}