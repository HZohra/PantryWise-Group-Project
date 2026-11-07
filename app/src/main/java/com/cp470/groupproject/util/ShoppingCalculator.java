package com.cp470.groupproject.util;

import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.MealPlanIngredient;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.model.StockSummary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out what has to be bought: the ingredients of the planned meals minus what is in stock.
 * plain java (no android classes, no logging), so the whole business rule is tested on the jvm.
 */
public final class ShoppingCalculator {

    /** amount still needed of one ingredient, in a base unit */
    private static final class Need {
        final String name;
        final String displayName;
        final String unit;
        double quantity;

        Need(String name, String displayName, String unit) {
            this.name = name;
            this.displayName = displayName;
            this.unit = unit;
        }
    }

    private ShoppingCalculator() { }

    /**
     * @param plans planned meals; the ones already cooked are ignored
     * @param stock stock of the inventory by ingredient and unit (expired food is not included)
     * @param currentList the shopping list as it is now
     * @return new auto-generated, unticked items for what is missing (not saved yet)
     */
    public static List<ShoppingItem> computeMissing(List<MealPlan> plans, List<StockSummary> stock,
                                                    List<ShoppingItem> currentList) {
        Map<String, Need> explicit = new LinkedHashMap<>();
        Map<String, String> presence = new LinkedHashMap<>();
        collectNeeds(plans, explicit, presence);

        List<ShoppingItem> missing = new ArrayList<>();
        for (Need need : explicit.values()) {
            double available = 0;
            boolean coveredInOtherUnit = false;
            for (StockSummary row : stock) {
                if (row.totalQuantity <= 0 || !covers(row.normalizedName, need.name)) continue;
                if (UnitUtils.sameDimension(row.unit, need.unit)) {
                    available += UnitUtils.toBase(row.totalQuantity, row.unit).value;
                } else {
                    coveredInOtherUnit = true;
                }
            }
            double shortfall = need.quantity - available;
            // stock in a unit that cannot be compared (cups vs grams): better not to bother
            boolean cannotCompare = available == 0 && coveredInOtherUnit;
            if (shortfall > 1e-9 && !cannotCompare && !onManualList(currentList, need.name)) {
                UnitUtils.Quantity pretty = UnitUtils.pretty(shortfall, need.unit);
                missing.add(newItem(need.displayName, pretty.value,
                        UnitUtils.displayUnit(pretty.unit)));
            }
        }
        for (Map.Entry<String, String> entry : presence.entrySet()) {
            if (!inStock(stock, entry.getKey()) && !onManualList(currentList, entry.getKey())) {
                missing.add(newItem(entry.getValue(), 1, ""));
            }
        }
        return missing;
    }

    /** sums the explicit needs by name and unit, and keeps the amount-less ones apart */
    private static void collectNeeds(List<MealPlan> plans, Map<String, Need> explicit,
                                     Map<String, String> presence) {
        for (MealPlan plan : plans) {
            if (plan.completed) continue;
            // the lines were parsed when the meal was saved, so nothing is read again here
            for (MealPlanIngredient line : plan.ingredients) {
                if (line.normalizedName.isEmpty()) continue;
                if (line.explicitQuantity) {
                    String key = line.normalizedName + "|" + line.unit;
                    Need need = explicit.get(key);
                    if (need == null) {
                        need = new Need(line.normalizedName, line.displayName(), line.unit);
                        explicit.put(key, need);
                    }
                    need.quantity += line.quantity;
                } else {
                    presence.put(line.normalizedName, line.displayName());
                }
            }
        }
        // an ingredient that also has an amount somewhere does not need a presence entry
        Set<String> withAmount = new HashSet<>();
        for (Need need : explicit.values()) {
            withAmount.add(need.name);
        }
        presence.keySet().removeAll(withAmount);
    }

    private static boolean inStock(List<StockSummary> stock, String name) {
        for (StockSummary row : stock) {
            if (row.totalQuantity > 0 && covers(row.normalizedName, name)) {
                return true;
            }
        }
        return false;
    }

    /** true if the user already has a pending item typed by hand for this ingredient */
    private static boolean onManualList(List<ShoppingItem> list, String name) {
        for (ShoppingItem item : list) {
            if (item.checked || item.autoGenerated) continue;
            String itemName = item.normalizedName.isEmpty()
                    ? IngredientNormalizer.normalize(item.name) : item.normalizedName;
            if (covers(itemName, name)) {
                return true;
            }
        }
        return false;
    }

    private static ShoppingItem newItem(String name, double quantity, String unit) {
        ShoppingItem item = new ShoppingItem(name, quantity, unit, null);
        item.autoGenerated = true;
        item.checked = false;
        return item;
    }

    /**
     * true if every word of {@code needName} is a word of {@code stockName}. a trailing "s" is
     * ignored on words longer than 3 letters, so "eggs" and "egg" match. "whole milk" covers
     * "milk", but "milk" does not cover "oat milk".
     */
    static boolean covers(String stockName, String needName) {
        Set<String> stockWords = words(stockName);
        Set<String> needWords = words(needName);
        return !needWords.isEmpty() && stockWords.containsAll(needWords);
    }

    private static Set<String> words(String name) {
        Set<String> result = new HashSet<>();
        for (String word : name.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            boolean plural = word.length() > 3 && word.endsWith("s");
            result.add(plural ? word.substring(0, word.length() - 1) : word);
        }
        return result;
    }
}
