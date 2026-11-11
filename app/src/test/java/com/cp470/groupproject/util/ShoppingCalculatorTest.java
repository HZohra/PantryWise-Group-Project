package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.model.MealType;
import com.cp470.groupproject.data.model.StockSummary;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tests the rule that turns planned meals and the inventory into "what to buy". */
public class ShoppingCalculatorTest {

    private static MealPlan plan(boolean completed, String... ingredients) {
        MealPlan plan = new MealPlan(0, MealType.DINNER, "id", "Meal",
                new ArrayList<>(Arrays.asList(ingredients)));
        plan.completed = completed;
        return plan;
    }

    private static MealPlan plan(String... ingredients) {
        return plan(false, ingredients);
    }

    private static StockSummary stock(String name, String unit, double quantity) {
        StockSummary row = new StockSummary();
        row.normalizedName = name;
        row.unit = unit;
        row.totalQuantity = quantity;
        return row;
    }

    private static List<ShoppingItem> compute(List<MealPlan> plans, List<StockSummary> stock,
                                              List<ShoppingItem> list) {
        return ShoppingCalculator.computeMissing(plans, stock, list);
    }

    private static ShoppingItem byName(List<ShoppingItem> items, String name) {
        for (ShoppingItem item : items) {
            if (item.name.equals(name)) return item;
        }
        return null;
    }

    @Test
    public void allMissing_whenStockEmpty() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("200 g flour", "2 eggs")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(2, result.size());
        assertEquals(200, byName(result, "Flour").quantity, 1e-9);
        assertEquals("g", byName(result, "Flour").unit);
        assertEquals(2, byName(result, "Eggs").quantity, 1e-9);
    }

    @Test
    public void partiallyMissing_subtractsStock() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("2 eggs")),
                Collections.singletonList(stock("eggs", "pcs", 1)), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).quantity, 1e-9);
    }

    @Test
    public void fullyCovered_producesNothing() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("2 eggs")),
                Collections.singletonList(stock("eggs", "pcs", 6)), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void sumsSameIngredientAcrossMeals() {
        List<ShoppingItem> result = compute(
                Arrays.asList(plan("2 eggs"), plan("2 eggs")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals(4, result.get(0).quantity, 1e-9);
    }

    @Test
    public void completedMeal_isIgnored() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan(true, "2 eggs")),
                Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void unitConversion_kgCoversGrams() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("150 g rice")),
                Collections.singletonList(stock("rice", "kg", 1)), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void incompatibleUnits_treatedAsCovered() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("2 cup sugar")),
                Collections.singletonList(stock("sugar", "g", 500)), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void presenceOnly_missingAddsOne() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("soy sauce")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals("Soy sauce", result.get(0).name);
        assertEquals(1, result.get(0).quantity, 1e-9);
        assertEquals("", result.get(0).unit);
    }

    @Test
    public void presenceOnly_inStockIsCovered() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("soy sauce")),
                Collections.singletonList(stock("soy sauce", "pcs", 1)), Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void explicitNeedWinsOverPresence() {
        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("sugar", "1 tbsp sugar")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals("tbsp", result.get(0).unit);
    }

    @Test
    public void manualPendingItem_preventsDuplicate() {
        ShoppingItem manual = new ShoppingItem("Sugar", 1, "", null);

        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("1 tbsp sugar")),
                Collections.emptyList(), Collections.singletonList(manual));

        assertTrue(result.isEmpty());
    }

    @Test
    public void checkedOrAutoItems_doNotPreventGeneration() {
        ShoppingItem ticked = new ShoppingItem("Sugar", 1, "", null);
        ticked.checked = true;
        ShoppingItem auto = new ShoppingItem("Sugar", 1, "", null);
        auto.autoGenerated = true;

        List<ShoppingItem> result = compute(
                Collections.singletonList(plan("1 tbsp sugar")),
                Collections.emptyList(), Arrays.asList(ticked, auto));

        assertEquals(1, result.size());
    }

    @Test
    public void covers_wordSubset() {
        assertTrue(ShoppingCalculator.covers("whole milk", "milk"));
        assertFalse(ShoppingCalculator.covers("milk", "oat milk"));
    }

    @Test
    public void covers_simplePlural() {
        assertTrue(ShoppingCalculator.covers("egg", "eggs"));
        assertTrue(ShoppingCalculator.covers("eggs", "egg"));
    }

    @Test
    public void outputsAreAutoGeneratedAndUnchecked() {
        ShoppingItem item = compute(
                Collections.singletonList(plan("2 eggs")),
                Collections.emptyList(), Collections.emptyList()).get(0);

        assertTrue(item.autoGenerated);
        assertFalse(item.checked);
    }

    @Test
    public void demoScenario_matchesExpectedList() {
        List<MealPlan> plans = Arrays.asList(
                plan("200 g flour", "2 eggs", "300 ml milk", "1 tbsp sugar"),
                plan("150 g rice", "2 eggs", "soy sauce"));
        List<StockSummary> inventory = Arrays.asList(
                stock("whole milk", "L", 1), stock("flour", "g", 300),
                stock("eggs", "pcs", 1), stock("rice", "kg", 1));

        List<ShoppingItem> result = compute(plans, inventory, Collections.emptyList());

        assertEquals(3, result.size());
        assertEquals(3, byName(result, "Eggs").quantity, 1e-9);
        assertEquals("", byName(result, "Eggs").unit);
        assertEquals(1, byName(result, "Sugar").quantity, 1e-9);
        assertEquals("tbsp", byName(result, "Sugar").unit);
        assertEquals(1, byName(result, "Soy sauce").quantity, 1e-9);
    }
}
