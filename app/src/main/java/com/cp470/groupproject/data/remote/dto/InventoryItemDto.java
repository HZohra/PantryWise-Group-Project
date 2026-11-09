package com.cp470.groupproject.data.remote.dto;

/**
 * One pantry item as the backend expects it.
 * Boxed types (Double, Integer) so "unknown" can be null: Gson then leaves the field out of the JSON,
 * and the backend treats a missing field as null.
 */
public class InventoryItemDto {
    public String name;
    public Double quantity;
    public String unit;
    public Integer daysUntilExpiry; // null = no expiry date; negative = already expired

    public InventoryItemDto(String name, Double quantity, String unit, Integer daysUntilExpiry) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.daysUntilExpiry = daysUntilExpiry;
    }
}