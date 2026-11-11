package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.model.Freshness;
import com.cp470.groupproject.data.remote.dto.InventoryItemDto;
import com.cp470.groupproject.util.ExpiryUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** JVM unit tests for InventoryMapper (no emulator needed). */
public class InventoryMapperTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = ExpiryUtils.DAY_MS;
    private static final long HOUR = DAY / 24;

    private static FoodItem item(String name, String normalized, double qty, String unit, long expiry) {
        FoodItem f = new FoodItem(name, qty, unit, 0, 0, 0, expiry);
        f.normalizedName = normalized;
        return f;
    }

    @Test
    public void daysUntilExpiry_noDate_isNull() {
        assertNull(InventoryMapper.daysUntilExpiry(0, NOW));
    }

    @Test
    public void daysUntilExpiry_roundsDown_likeExpiryUtils() {
        assertEquals(Integer.valueOf(0), InventoryMapper.daysUntilExpiry(NOW + HOUR, NOW));
        assertEquals(Integer.valueOf(2), InventoryMapper.daysUntilExpiry(NOW + 2 * DAY + 12 * HOUR, NOW));
        assertEquals(Integer.valueOf(-1), InventoryMapper.daysUntilExpiry(NOW - HOUR, NOW));
    }

    @Test
    public void negativeDays_matchesExpiredFreshness() {
        long[] offsets = { -3 * DAY, -HOUR, -1, 0, 1, HOUR, DAY, 5 * DAY };
        for (long offset : offsets) {
            long expiry = NOW + offset;
            boolean aiSaysExpired = InventoryMapper.daysUntilExpiry(expiry, NOW) < 0;
            boolean inventorySaysExpired =
                    ExpiryUtils.classify(expiry, NOW, ExpiryUtils.DEFAULT_USE_SOON_DAYS) == Freshness.EXPIRED;
            assertEquals("offset " + offset, inventorySaysExpired, aiSaysExpired);
        }
    }

    @Test
    public void toDto_prefersNormalizedName_andCleansEmptyValues() {
        InventoryItemDto dto = InventoryMapper.toDto(item("2% Milk 1L", "milk", 0, "", 0), NOW);
        assertEquals("milk", dto.name);
        assertNull(dto.quantity);
        assertNull(dto.unit);
        assertNull(dto.daysUntilExpiry);
    }

    @Test
    public void toDto_fallsBackToName_andClampsLongValues() {
        String longName = new String(new char[150]).replace('\0', 'a');
        InventoryItemDto dto = InventoryMapper.toDto(item(longName, "", 1, "a-very-long-unit-name-here", 0), NOW);
        assertEquals(InventoryMapper.MAX_NAME_LENGTH, dto.name.length());
        assertEquals(InventoryMapper.MAX_UNIT_LENGTH, dto.unit.length());
    }

    @Test
    public void toDtos_sortsMostUrgentFirst_unknownLast() {
        List<FoodItem> items = Arrays.asList(
                item("Rice", "rice", 1, "kg", 0),
                item("Eggs", "egg", 6, "pcs", NOW + 10 * DAY),
                item("Spinach", "spinach", 1, "bag", NOW + DAY + HOUR),
                item("Yogurt", "yogurt", 1, "cup", NOW - HOUR));

        List<InventoryItemDto> dtos = InventoryMapper.toDtos(items, NOW);

        assertEquals("yogurt", dtos.get(0).name);
        assertEquals("spinach", dtos.get(1).name);
        assertEquals("egg", dtos.get(2).name);
        assertEquals("rice", dtos.get(3).name);
    }

    @Test
    public void toDtos_keepsAtMostMaxItems() {
        List<FoodItem> items = new ArrayList<>();
        for (int i = 0; i < 250; i++) items.add(item("item" + i, "", 1, "pcs", 0));
        assertEquals(InventoryMapper.MAX_ITEMS, InventoryMapper.toDtos(items, NOW).size());
    }
}