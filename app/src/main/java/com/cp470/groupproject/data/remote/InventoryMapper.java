package com.cp470.groupproject.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.remote.dto.InventoryItemDto;
import com.cp470.groupproject.util.ExpiryUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Converts Room {@link FoodItem}s into the shape the backend expects.
 * Pure Java; values are clamped to the backend's validation limits, because ONE invalid
 * item would make the backend reject the WHOLE request.
 */
public final class InventoryMapper {

    public static final int MAX_ITEMS = 200;       // LIMITS.inventoryMax
    public static final int MAX_NAME_LENGTH = 100; // LIMITS.nameMax
    public static final int MAX_UNIT_LENGTH = 20;  // LIMITS.unitMax

    private InventoryMapper() { }

    /** Uses the team's ExpiryUtils so "expired" means the same as on the inventory screen. null = no date. */
    @Nullable
    public static Integer daysUntilExpiry(long expirationMillis, long nowMillis) {
        if (expirationMillis <= 0) {
            return null;
        }
        return (int) ExpiryUtils.daysUntil(expirationMillis, nowMillis); // changed: cast works for int or long
    }

    @NonNull
    public static InventoryItemDto toDto(@NonNull FoodItem item, long nowMillis) {
        // changed: null-safe, in case normalizedName/name/unit can be null
        String name = !isBlank(item.normalizedName) ? item.normalizedName
                : (item.name == null ? "" : item.name);
        Double quantity = item.quantity > 0 ? item.quantity : null;
        String unit = isBlank(item.unit) ? null : clamp(item.unit, MAX_UNIT_LENGTH);
        return new InventoryItemDto(clamp(name, MAX_NAME_LENGTH), quantity, unit,
                daysUntilExpiry(item.expirationDate, nowMillis));
    }

    /** Drops nameless items, sorts most urgent first (unknown expiry last), keeps at most MAX_ITEMS. */
    @NonNull
    public static List<InventoryItemDto> toDtos(@NonNull List<FoodItem> items, long nowMillis) {
        List<InventoryItemDto> result = new ArrayList<>();
        for (FoodItem item : items) {
            if (item == null) continue;
            InventoryItemDto dto = toDto(item, nowMillis);
            if (!dto.name.isEmpty()) result.add(dto);
        }
        result.sort(Comparator.comparing(
                (InventoryItemDto d) -> d.daysUntilExpiry,
                Comparator.nullsLast(Comparator.<Integer>naturalOrder()))); // changed: explicit type
        return result.size() > MAX_ITEMS ? new ArrayList<>(result.subList(0, MAX_ITEMS)) : result;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @NonNull
    private static String clamp(@NonNull String value, int max) {
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}