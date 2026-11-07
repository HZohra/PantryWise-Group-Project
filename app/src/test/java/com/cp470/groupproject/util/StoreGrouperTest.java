package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.util.StoreGrouper.ShoppingRow;
import com.cp470.groupproject.util.StoreGrouper.StoreGroup;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Tests how the shopping list is grouped by shop. */
public class StoreGrouperTest {

    private static ShoppingItem item(String name, String store) {
        return new ShoppingItem(name, 1, "", store);
    }

    private static Map<StoreType, String> names(StoreType type, String name) {
        Map<StoreType, String> map = new EnumMap<>(StoreType.class);
        map.put(type, name);
        return map;
    }

    @Test
    public void group_explicitStore_beatsAdvisor() {
        List<StoreGroup> groups = StoreGrouper.group(
                Collections.singletonList(item("chicken", "Walmart")), null);

        assertEquals(1, groups.size());
        assertEquals("Walmart", groups.get(0).name);
    }

    @Test
    public void group_emptyStore_usesAdvisorType() {
        List<StoreGroup> groups = StoreGrouper.group(
                Collections.singletonList(item("chicken", null)), null);

        assertEquals(StoreType.BUTCHER, groups.get(0).type);
        assertNull(groups.get(0).name);
    }

    @Test
    public void group_blankStore_isTreatedAsEmpty() {
        List<StoreGroup> groups = StoreGrouper.group(
                Collections.singletonList(item("bread", "   ")), null);

        assertEquals(StoreType.BAKERY, groups.get(0).type);
    }

    @Test
    public void group_customName_titlesTheTypeGroup() {
        List<StoreGroup> groups = StoreGrouper.group(
                Collections.singletonList(item("chicken", null)),
                names(StoreType.BUTCHER, "Carnes Lopez"));

        assertEquals("Carnes Lopez", groups.get(0).name);
    }

    @Test
    public void group_sameNameDifferentCase_mergesGroups() {
        List<StoreGroup> groups = StoreGrouper.group(Arrays.asList(
                item("rice", "mercadona"), item("milk", null)),
                names(StoreType.GENERAL, "Mercadona"));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).items.size());
    }

    @Test
    public void group_generalGroup_isLast() {
        List<StoreGroup> groups = StoreGrouper.group(Arrays.asList(
                item("rice", null), item("bread", null), item("chicken", null)), null);

        assertEquals(StoreType.BUTCHER, groups.get(0).type);
        assertEquals(StoreType.BAKERY, groups.get(1).type);
        assertEquals(StoreType.GENERAL, groups.get(2).type);
    }

    @Test
    public void group_namedByHandGroups_areFirstAndAlphabetical() {
        List<StoreGroup> groups = StoreGrouper.group(Arrays.asList(
                item("chicken", null), item("rice", "Zeta"), item("milk", "alpha")), null);

        assertEquals("alpha", groups.get(0).name);
        assertEquals("Zeta", groups.get(1).name);
        assertEquals(StoreType.BUTCHER, groups.get(2).type);
    }

    @Test
    public void group_preservesInputOrderInsideGroup() {
        ShoppingItem first = item("rice", null);
        ShoppingItem second = item("flour", null);
        List<StoreGroup> groups = StoreGrouper.group(Arrays.asList(first, second), null);

        assertSame(first, groups.get(0).items.get(0));
        assertSame(second, groups.get(0).items.get(1));
    }

    @Test
    public void group_emptyList_returnsNoGroups() {
        assertTrue(StoreGrouper.group(Collections.emptyList(), null).isEmpty());
    }

    @Test
    public void group_checkedItems_stayInTheirGroup() {
        ShoppingItem done = item("chicken", null);
        done.checked = true;
        List<StoreGroup> groups = StoreGrouper.group(Arrays.asList(item("beef", null), done), null);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).items.size());
    }

    @Test
    public void toRows_addsOneHeaderBeforeEachGroup() {
        List<ShoppingRow> rows = StoreGrouper.toRows(StoreGrouper.group(Arrays.asList(
                item("chicken", null), item("beef", null), item("rice", null)), null));

        assertEquals(5, rows.size());
        assertTrue(rows.get(0).isHeader());
        assertFalse(rows.get(1).isHeader());
        assertFalse(rows.get(2).isHeader());
        assertTrue(rows.get(3).isHeader());
        assertFalse(rows.get(4).isHeader());
    }

    @Test
    public void toRows_noGroups_returnsNoRows() {
        assertTrue(StoreGrouper.toRows(Collections.emptyList()).isEmpty());
    }

    @Test
    public void flat_returnsOnlyItemRows() {
        List<ShoppingRow> rows = StoreGrouper.flat(Arrays.asList(
                item("chicken", null), item("rice", null)));

        assertEquals(2, rows.size());
        assertFalse(rows.get(0).isHeader());
        assertFalse(rows.get(1).isHeader());
    }
}
