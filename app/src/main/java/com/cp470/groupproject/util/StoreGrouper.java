package com.cp470.groupproject.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.entity.ShoppingItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groups the shopping list by the shop where each item should be bought. a shop typed by hand on
 * the item wins; otherwise the shop type suggested by {@link StoreAdvisor} is used, with the name
 * the user gave to that kind of shop if there is one. plain java, so it is tested on the jvm.
 */
public final class StoreGrouper {

    /** items that go to the same shop */
    public static final class StoreGroup {
        /** identifies the group: the lower-case shop name, or the kind of shop if it has no name */
        public final String key;
        /** kind of shop; only meaningful when {@link #name} is null */
        public final StoreType type;
        /** name of the shop, or null to show the generic name of the type */
        @Nullable
        public final String name;
        /** true if some item of the group has its shop typed by hand */
        boolean fromHand;
        /** the items, in the order they came in */
        public final List<ShoppingItem> items = new ArrayList<>();

        StoreGroup(String key, StoreType type, @Nullable String name) {
            this.key = key;
            this.type = type;
            this.name = name;
        }
    }

    /** one line of the grouped list: a group header or an item */
    public static final class ShoppingRow {
        /** the group, for a header row; null for an item row */
        @Nullable
        public final StoreGroup group;
        /** the item, for an item row; null for a header row */
        @Nullable
        public final ShoppingItem item;

        private ShoppingRow(@Nullable StoreGroup group, @Nullable ShoppingItem item) {
            this.group = group;
            this.item = item;
        }

        /** @return true for a group header */
        public boolean isHeader() {
            return group != null;
        }
    }

    private StoreGrouper() { }

    /**
     * @param items the list, in the order it should keep inside each group
     * @param customNames names the user gave to each kind of shop; may be null or partial
     * @return the groups: shops typed by hand first (alphabetical), then the kinds of shop in the
     *         order of {@link StoreType}, with the general supermarket last
     */
    @NonNull
    public static List<StoreGroup> group(@NonNull List<ShoppingItem> items,
                                         @Nullable Map<StoreType, String> customNames) {
        Map<String, StoreGroup> groups = new LinkedHashMap<>();
        for (ShoppingItem item : items) {
            String typed = item.store == null ? "" : item.store.trim();
            StoreType type = null;
            String name;
            boolean byHand = !typed.isEmpty();
            if (byHand) {
                name = typed;
            } else {
                type = StoreAdvisor.suggest(item.name);
                String custom = customNames == null ? null : customNames.get(type);
                name = custom == null || custom.trim().isEmpty() ? null : custom.trim();
            }
            String key = name != null ? "name:" + name.toLowerCase(Locale.ROOT)
                    : "type:" + type.getKey();
            StoreGroup group = groups.get(key);
            if (group == null) {
                group = new StoreGroup(key, type, name);
                groups.put(key, group);
            }
            group.fromHand |= byHand;
            group.items.add(item);
        }
        List<StoreGroup> sorted = new ArrayList<>(groups.values());
        Collections.sort(sorted, ORDER);
        return sorted;
    }

    /** @return one header row before the items of each group */
    @NonNull
    public static List<ShoppingRow> toRows(@NonNull List<StoreGroup> groups) {
        List<ShoppingRow> rows = new ArrayList<>();
        for (StoreGroup group : groups) {
            rows.add(new ShoppingRow(group, null));
            for (ShoppingItem item : group.items) {
                rows.add(new ShoppingRow(null, item));
            }
        }
        return rows;
    }

    /** @return only item rows, for the list that is not grouped by shop */
    @NonNull
    public static List<ShoppingRow> flat(@NonNull List<ShoppingItem> items) {
        List<ShoppingRow> rows = new ArrayList<>();
        for (ShoppingItem item : items) {
            rows.add(new ShoppingRow(null, item));
        }
        return rows;
    }

    private static final Comparator<StoreGroup> ORDER = (a, b) -> {
        if (a.fromHand != b.fromHand) {
            return a.fromHand ? -1 : 1;
        }
        if (a.fromHand) {
            return a.name.compareToIgnoreCase(b.name);
        }
        return Integer.compare(a.type.ordinal(), b.type.ordinal());
    };
}
