package com.cp470.groupproject.data.repository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.data.dao.CategoryDao;
import com.cp470.groupproject.data.dao.ConsumptionHistoryDao;
import com.cp470.groupproject.data.dao.FavoriteRecipeDao;
import com.cp470.groupproject.data.dao.FoodItemDao;
import com.cp470.groupproject.data.dao.MealPlanDao;
import com.cp470.groupproject.data.dao.ShoppingItemDao;
import com.cp470.groupproject.data.dao.StorageLocationDao;
import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.Category;
import com.cp470.groupproject.data.entity.ConsumptionHistory;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FavoriteRecipeIngredient;
import com.cp470.groupproject.data.entity.FavoriteRecipeStep;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.MealPlanIngredient;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.entity.StorageLocation;
import com.cp470.groupproject.data.model.FavoriteRecipeWithIngredients;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.data.model.MealPlanWithIngredients;
import com.cp470.groupproject.data.model.StockSummary;
import com.cp470.groupproject.data.model.WasteStats;
import com.cp470.groupproject.util.ExpiryUtils;
import com.cp470.groupproject.util.IngredientNormalizer;
import com.cp470.groupproject.util.WasteStatsCalculator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Single access point to local data (the "model" layer of mvvm).
 *
 * viewmodels talk only to this class, never to the daos. reads return{@link LiveData}
 * (observable and safe on the main thread). writes run on a background executor and can report
 * back through a {@link ResultCallback}, which is invoked on the background thread (use postValue
 * or runOnUiThread).
 *
 * methods whose name ends in {@code Sync} must be called off the main thread.
 */

public class PantryRepository {

    /** callback for the result of an asynchronous write */
    public interface ResultCallback<T> {
        void onResult(T result);
    }

    private final PantryDatabase database;
    private final FoodItemDao foodItemDao;
    private final CategoryDao categoryDao;
    private final StorageLocationDao locationDao;
    private final ShoppingItemDao shoppingDao;
    private final FavoriteRecipeDao favoriteDao;
    private final ConsumptionHistoryDao historyDao;
    private final MealPlanDao mealPlanDao;
    private final Executor executor;

    /** production constructor */
    public PantryRepository(@NonNull Application application) {
        this(PantryDatabase.getInstance(application), PantryDatabase.WRITE_EXECUTOR);
    }

    /** test constructor: inject an in-memory database and a direct executor */
    public PantryRepository(@NonNull PantryDatabase database, @NonNull Executor executor) {
        this.database = database;
        this.foodItemDao = database.foodItemDao();
        this.categoryDao = database.categoryDao();
        this.locationDao = database.storageLocationDao();
        this.shoppingDao = database.shoppingItemDao();
        this.favoriteDao = database.favoriteRecipeDao();
        this.historyDao = database.consumptionHistoryDao();
        this.mealPlanDao = database.mealPlanDao();
        this.executor = executor;
    }









    // ================================================================ inventory

    /** observable active inventory, soonest expiry first */
    public LiveData<List<FoodItem>> getActiveItems() {
        return foodItemDao.observeActive();
    }

    /** observable single item by id */
    public LiveData<FoodItem> getItemById(long id) {
        return foodItemDao.observeById(id);
    }

    /** active items in one storage location */
    public LiveData<List<FoodItem>> getItemsByLocation(long locationId) {
        return foodItemDao.observeByLocation(locationId);
    }

    /** active items in one category */
    public LiveData<List<FoodItem>> getItemsByCategory(long categoryId) {
        return foodItemDao.observeByCategory(categoryId);
    }

    /** "fresh" filter (evaluated at call time, call again to refresh as time passes) */
    public LiveData<List<FoodItem>> getFreshItems() {
        long now = System.currentTimeMillis();
        return foodItemDao.observeFresh(
                ExpiryUtils.useSoonLimit(now, ExpiryUtils.DEFAULT_USE_SOON_DAYS));
    }

    /** "use soon" filter: expires within the default 3-day window */
    public LiveData<List<FoodItem>> getUseSoonItems() {
        long now = System.currentTimeMillis();
        return foodItemDao.observeUseSoon(now,
                ExpiryUtils.useSoonLimit(now, ExpiryUtils.DEFAULT_USE_SOON_DAYS));
    }

    /** "expired" filter */
    public LiveData<List<FoodItem>> getExpiredItems() {
        return foodItemDao.observeExpired(System.currentTimeMillis());
    }

    /**
     * inserts an item. the normalized name is computed here, so every producer
     * (manual entry, barcode scan, usda autofill) gets consistent matching keys
     *
     * @param callback receives the new row id (may be null)
     */
    public void insertFoodItem(@NonNull FoodItem item, @Nullable ResultCallback<Long> callback) {
        executor.execute(() -> {
            item.normalizedName = IngredientNormalizer.normalize(item.name);
            long id = foodItemDao.insert(item);
            if (callback != null) callback.onResult(id);
        });
    }

    /** updates the item details (the name is normalized again) */
    public void updateFoodItem(@NonNull FoodItem item, @Nullable ResultCallback<Integer> callback) {
        executor.execute(() -> {
            item.normalizedName = IngredientNormalizer.normalize(item.name);
            int rows = foodItemDao.update(item);
            if (callback != null) callback.onResult(rows);
        });
    }

    /** permanently deletes an item (no waste log entry, use recordDisposition for that) */
    public void deleteFoodItem(@NonNull FoodItem item, @Nullable ResultCallback<Integer> callback) {
        executor.execute(() -> {
            int rows = foodItemDao.delete(item);
            if (callback != null) callback.onResult(rows);
        });
    }

    /**
     * removes an item from the active inventory and logs what happened to it
     * both steps run in one transaction: either the status change and the log entry are both saved,
     * or neither is
     *
     * @param disposition one of {@link ItemStatus#DISPOSITIONS}
     * @param callback receives the new history row id (may be null)
     * @throws IllegalArgumentException if {@code disposition} is not a valid outcome
     */
    public void recordDisposition(@NonNull FoodItem item, @NonNull String disposition,
                                  @Nullable ResultCallback<Long> callback) {
        if (!ItemStatus.isDisposition(disposition)) {
            throw new IllegalArgumentException("not a disposition: " + disposition);
        }
        executor.execute(() -> {
            final long[] historyId = new long[1];
            database.runInTransaction(() -> {
                historyId[0] = historyDao.insert(ConsumptionHistory.fromFoodItem(
                        item, disposition, System.currentTimeMillis()));
                foodItemDao.updateStatus(item.id, disposition);
            });
            if (callback != null) callback.onResult(historyId[0]);
        });
    }







    // ---- synchronous reads for the recipe engine and shopping list (background thread only)

    /** snapshot of the whole active inventory */
    public List<FoodItem> getActiveItemsSync() {
        return foodItemDao.getActiveSync();
    }

    /** items expiring within {@code days} days (not yet expired), soonest first */
    public List<FoodItem> getExpiringSoonSync(int days) {
        long now = System.currentTimeMillis();
        return foodItemDao.getExpiringSoonSync(now, ExpiryUtils.useSoonLimit(now, days));
    }

    /** active items whose normalized name contains {@code term} */
    public List<FoodItem> searchActiveByNameSync(@NonNull String term) {
        return foodItemDao.searchActiveByName(IngredientNormalizer.normalize(term));
    }

    /** available (non-expired) stock per ingredient and unit */
    public List<StockSummary> getStockSummarySync() {
        return foodItemDao.getStockSummarySync(System.currentTimeMillis());
    }

    /** @return the active item with this barcode, or null */
    @Nullable
    public FoodItem findActiveByBarcodeSync(@NonNull String barcode) {
        return foodItemDao.findActiveByBarcode(barcode);
    }




    // =========================================================== reference data

    /** observable list of categories */
    public LiveData<List<Category>> getCategories() {
        return categoryDao.observeAll();
    }

    /** observable single category by id */
    public LiveData<Category> getCategoryById(long id) {
        return categoryDao.observeById(id);
    }

    /** observable list of storage locations */
    public LiveData<List<StorageLocation>> getStorageLocations() {
        return locationDao.observeAll();
    }

    /** observable single storage location by id */
    public LiveData<StorageLocation> getStorageLocationById(long id) {
        return locationDao.observeById(id);
    }







    // ============================================================ shopping list

    /** observable shopping list */
    public LiveData<List<ShoppingItem>> getShoppingItems() {
        return shoppingDao.observeAll();
    }

    /** adds an item to the shopping list (the name is normalized) */
    public void addShoppingItem(@NonNull ShoppingItem item, @Nullable ResultCallback<Long> callback) {
        executor.execute(() -> {
            item.normalizedName = IngredientNormalizer.normalize(item.name);
            long id = shoppingDao.insert(item);
            if (callback != null) callback.onResult(id);
        });
    }

    /** ticks or unticks a shopping item */
    public void setShoppingItemChecked(long id, boolean checked) {
        executor.execute(() -> shoppingDao.setChecked(id, checked));
    }

    /** updates a shopping item (the name is normalized again, it may have been edited) */
    public void updateShoppingItem(@NonNull ShoppingItem item) {
        executor.execute(() -> {
            item.normalizedName = IngredientNormalizer.normalize(item.name);
            shoppingDao.update(item);
        });
    }

    /** snapshot of the shopping list. background thread only */
    public List<ShoppingItem> getShoppingItemsSync() {
        return shoppingDao.getAllSync();
    }

    /**
     * replaces the pending auto-generated items by {@code items}, in one transaction (either both
     * steps are saved or neither). manual and ticked items are not touched.
     *
     * @param items the new auto-generated items
     * @param callback receives how many items were inserted (background thread), may be null
     */
    public void replaceAutoGeneratedShoppingItems(@NonNull List<ShoppingItem> items,
                                                  @Nullable ResultCallback<Integer> callback) {
        executor.execute(() -> {
            database.runInTransaction(() -> {
                shoppingDao.deleteAutoGeneratedPending();
                for (ShoppingItem item : items) {
                    item.normalizedName = IngredientNormalizer.normalize(item.name);
                    item.autoGenerated = true;
                }
                if (!items.isEmpty()) shoppingDao.insertAll(items);
            });
            if (callback != null) callback.onResult(items.size());
        });
    }

    /** removes an item from the shopping list */
    public void deleteShoppingItem(@NonNull ShoppingItem item) {
        executor.execute(() -> shoppingDao.delete(item));
    }

    /** "clear completed": deletes all ticked items, the callback receives how many */
    public void clearCompletedShoppingItems(@Nullable ResultCallback<Integer> callback) {
        executor.execute(() -> {
            int rows = shoppingDao.deleteChecked();
            if (callback != null) callback.onResult(rows);
        });
    }





    // ================================================================ favorites

    /** observable list of favorite recipes, newest first */
    public LiveData<List<FavoriteRecipe>> getFavoriteRecipes() {
        return Transformations.map(favoriteDao.observeAll(),
                FavoriteRecipeWithIngredients::toRecipes);
    }

    /** snapshot of the favorite recipes with their ingredients. background thread only */
    public List<FavoriteRecipe> getFavoriteRecipesSync() {
        return FavoriteRecipeWithIngredients.toRecipes(favoriteDao.getAllSync());
    }

    /**
     * one saved recipe with its ingredients and steps, or null if it is not saved.
     * background thread only
     */
    @Nullable
    public FavoriteRecipe getFavoriteRecipeSync(@NonNull String recipeId) {
        FavoriteRecipeWithIngredients row = favoriteDao.getByRecipeIdSync(recipeId);
        return row == null ? null : row.toRecipe();
    }

    /** emits 1 if the recipe is saved, otherwise 0 */
    public LiveData<Integer> isFavorite(@NonNull String recipeId) {
        return favoriteDao.observeIsFavorite(recipeId);
    }

    /**
     * saves a recipe as favorite. saving the same recipe again replaces it, and the old
     * ingredient and step rows are deleted with the old row (cascade). the recipe, its
     * ingredients and its steps are written in one transaction, so a failure never leaves a
     * recipe without its ingredients or steps.
     */
    public void saveFavoriteRecipe(@NonNull FavoriteRecipe recipe) {
        executor.execute(() -> database.runInTransaction(() -> {
            long id = favoriteDao.insert(recipe);
            List<FavoriteRecipeIngredient> rows = new ArrayList<>(recipe.ingredients);
            for (FavoriteRecipeIngredient row : rows) {
                row.id = 0;
                row.favoriteId = id;
            }
            favoriteDao.insertIngredients(rows);
            List<FavoriteRecipeStep> steps = new ArrayList<>(recipe.steps);
            for (FavoriteRecipeStep step : steps) {
                // a step that came from the database keeps its old id otherwise
                step.id = 0;
                step.favoriteId = id;
            }
            favoriteDao.insertSteps(steps);
        }));
    }

    /** removes a favorite recipe */
    public void removeFavoriteRecipe(@NonNull String recipeId) {
        executor.execute(() -> favoriteDao.deleteByRecipeId(recipeId));
    }






    // ============================================================ waste tracking

    /** observable full consumption log, newest first */
    public LiveData<List<ConsumptionHistory>> getHistory() {
        return historyDao.observeAll();
    }

    /**
     * waste metrics for the entries recorded at or after {@code since}
     *
     * @param since time in milliseconds, 0 means all time
     */
    public LiveData<WasteStats> getWasteStats(long since) {
        return Transformations.map(historyDao.observeCountsSince(since),
                WasteStatsCalculator::fromCounts);
    }





    // ============================================================== meal plan

    /** observable planned meals between two days (both included) */
    public LiveData<List<MealPlan>> getMealPlans(long from, long to) {
        return Transformations.map(mealPlanDao.observeBetween(from, to),
                MealPlanWithIngredients::toMealPlans);
    }

    /** snapshot of the planned meals between two days. background thread only */
    public List<MealPlan> getMealPlansSync(long from, long to) {
        return MealPlanWithIngredients.toMealPlans(mealPlanDao.getBetweenSync(from, to));
    }

    /**
     * adds a planned meal with its ingredients in one transaction, the callback receives the new
     * id (may be null)
     */
    public void addMealPlan(@NonNull MealPlan plan, @Nullable ResultCallback<Long> callback) {
        executor.execute(() -> {
            long[] id = new long[1];
            database.runInTransaction(() -> {
                id[0] = mealPlanDao.insert(plan);
                saveMealPlanIngredients(plan, id[0]);
            });
            if (callback != null) callback.onResult(id[0]);
        });
    }

    /** updates a planned meal and replaces its ingredients, in one transaction */
    public void updateMealPlan(@NonNull MealPlan plan) {
        executor.execute(() -> database.runInTransaction(() -> {
            mealPlanDao.update(plan);
            mealPlanDao.deleteIngredients(plan.id);
            saveMealPlanIngredients(plan, plan.id);
        }));
    }

    /** writes the ingredient rows of a meal that is already saved. runs inside a transaction */
    private void saveMealPlanIngredients(MealPlan plan, long mealPlanId) {
        List<MealPlanIngredient> rows = new ArrayList<>(plan.ingredients);
        for (MealPlanIngredient row : rows) {
            // a row that came from the database keeps its old id otherwise
            row.id = 0;
            row.mealPlanId = mealPlanId;
        }
        mealPlanDao.insertIngredients(rows);
    }

    /** deletes a planned meal (its ingredient rows are deleted by the cascade) */
    public void deleteMealPlan(@NonNull MealPlan plan) {
        executor.execute(() -> mealPlanDao.delete(plan));
    }

    /** marks a planned meal as cooked or not cooked */
    public void setMealPlanCompleted(long id, boolean completed) {
        executor.execute(() -> mealPlanDao.setCompleted(id, completed));
    }

    /**
     * normalized names of the ingredients needed by the meals that are not cooked yet
     * the shopping list can compare them with {@link #getStockSummarySync()}
     * background thread only
     */
    public List<String> getPlannedIngredientsSync(long from, long to) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (MealPlan plan : getMealPlansSync(from, to)) {
            if (plan.completed) continue;
            for (MealPlanIngredient ingredient : plan.ingredients) {
                if (!ingredient.normalizedName.isEmpty()) names.add(ingredient.normalizedName);
            }
        }
        return new ArrayList<>(names);
    }
}