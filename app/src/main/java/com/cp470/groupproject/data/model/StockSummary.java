package com.cp470.groupproject.data.model;

/**
 * Query result row: total stock of one normalized ingredient in one unit.
 * used by the recipe engine and the shopping list to subtract what a recipe needs from
 * what is available.
 */

public class StockSummary {
    /** normalized ingredient name, e.g. "milk" */
    public String normalizedName;

    /** unit of measure the quantity is expressed in */
    public String unit;

    /** sum of the quantities of all active, non-expired items */
    public double totalQuantity;
}