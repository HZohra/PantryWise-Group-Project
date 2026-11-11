package com.cp470.groupproject.ui.mealplanner;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.shopping.ShoppingListGenerator;
import com.cp470.groupproject.util.Event;
import com.cp470.groupproject.util.WeekUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * State and actions of the meal planner. the displayed week lives here, so it survives a
 * rotation; changing the week only changes the database query.
 */
public class MealPlannerViewModel extends AndroidViewModel {

    private static final String TAG = "MealPlannerViewModel";

    private final PantryRepository repository;
    private final MutableLiveData<Boolean> generating = new MutableLiveData<>(false);
    private final ZoneId zone = ZoneId.systemDefault();
    private final MutableLiveData<Long> weekStart = new MutableLiveData<>();
    private final LiveData<List<DayPlan>> days;
    private final LiveData<List<FavoriteRecipe>> favorites;
    private final LiveData<List<String>> inventoryNames;
    /** one-shot messages, as string resource ids */
    private final MutableLiveData<Event<Integer>> messages = new MutableLiveData<>();
    private final MutableLiveData<Event<ShoppingListGenerator.Result>> generated =
            new MutableLiveData<>();

    /** @param application used to create the repository */
    public MealPlannerViewModel(@NonNull Application application) {
        super(application);
        repository = new PantryRepository(application);
        weekStart.setValue(WeekUtils.startOfWeek(System.currentTimeMillis(), zone,
                WeekUtils.defaultFirstDayOfWeek()));
        days = Transformations.switchMap(weekStart, start ->
                Transformations.map(
                        repository.getMealPlans(start, WeekUtils.endOfWeek(start, zone)),
                        plans -> MealPlanGrouper.group(plans, start, zone)));
        favorites = repository.getFavoriteRecipes();
        inventoryNames = Transformations.map(repository.getActiveItems(), items -> {
            Set<String> names = new TreeSet<>();
            for (FoodItem item : items) {
                if (!item.normalizedName.isEmpty()) names.add(item.normalizedName);
            }
            return new ArrayList<>(names);
        });
    }

    /** @return start of the displayed week, in milliseconds */
    public LiveData<Long> getWeekStart() {
        return weekStart;
    }

    /** @return the seven days of the displayed week */
    public LiveData<List<DayPlan>> getDays() {
        return days;
    }

    /** @return the saved recipes the user can pick from */
    public LiveData<List<FavoriteRecipe>> getFavorites() {
        return favorites;
    }

    /** @return the names of the food in the inventory, to suggest them as ingredients */
    public LiveData<List<String>> getInventoryNames() {
        return inventoryNames;
    }

    /** @return one-shot messages (string resource ids) */
    public LiveData<Event<Integer>> getMessages() {
        return messages;
    }

    /** @return the time zone used for the calendar */
    public ZoneId getZone() {
        return zone;
    }

    /** shows the week before the current one */
    public void previousWeek() {
        shiftWeek(-7);
    }

    /** shows the week after the current one */
    public void nextWeek() {
        shiftWeek(7);
    }

    /** shows the week that contains today */
    public void goToToday() {
        weekStart.setValue(WeekUtils.startOfWeek(System.currentTimeMillis(), zone,
                WeekUtils.defaultFirstDayOfWeek()));
    }

    /**
     * puts back the week saved in the activity state. it does nothing if it is already shown, so
     * the query is not run again for nothing.
     *
     * @param weekStartMs start of the week to show
     */
    public void restoreWeek(long weekStartMs) {
        Long current = weekStart.getValue();
        if (current == null || current != weekStartMs) {
            weekStart.setValue(weekStartMs);
        }
    }

    private void shiftWeek(int days) {
        Long current = weekStart.getValue();
        if (current != null) {
            weekStart.setValue(WeekUtils.plusDays(current, days, zone));
        }
    }

    /**
     * saves a meal in a slot. if the slot already has a meal it is updated, so a slot never holds
     * two meals.
     *
     * @param dayStartMs start of the day
     * @param mealType one of the {@code MealType} constants
     * @param recipeId id of a saved recipe, or null for a custom one
     * @param title name of the meal
     * @param ingredients ingredient texts such as "200 g flour", already built by the form
     * @return false if the title is empty (nothing is saved)
     */
    public boolean saveMeal(long dayStartMs, @NonNull String mealType, @Nullable String recipeId,
                            String title, @NonNull List<String> ingredients) {
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            return false;
        }
        MealPlan existing = findMeal(dayStartMs, mealType);
        if (existing != null) {
            // edit a copy: the list may be showing this very object
            MealPlan updated = new MealPlan(existing.planDate, existing.mealType,
                    recipeId != null ? recipeId : existing.recipeId, cleanTitle, ingredients);
            updated.id = existing.id;
            updated.completed = existing.completed;
            updated.createdAt = existing.createdAt;
            repository.updateMealPlan(updated);
        } else {
            String id = recipeId != null ? recipeId : "manual-" + UUID.randomUUID();
            repository.addMealPlan(
                    new MealPlan(dayStartMs, mealType, id, cleanTitle, ingredients), null);
        }
        messages.setValue(new Event<>(R.string.planner_meal_saved));
        return true;
    }

    /** @return what each generation did (one-shot) */
    public LiveData<Event<ShoppingListGenerator.Result>> getGenerated() {
        return generated;
    }

    /**
     * fills the shopping list with what is missing for the displayed week. days already gone are
     * left out; a week that is completely over has nothing to generate.
     */
    public void generateForDisplayedWeek() {
        Long start = weekStart.getValue();
        if (start == null) return;
        long from = Math.max(WeekUtils.startOfDay(System.currentTimeMillis(), zone), start);
        long to = WeekUtils.endOfWeek(start, zone);
        if (from > to) {
            messages.setValue(new Event<>(R.string.planner_nothing_to_generate));
            return;
        }
        // a second tap while the first generation runs would only repeat the work
        if (Boolean.TRUE.equals(generating.getValue())) return;
        generating.setValue(true);
        // on the write thread, so it runs in order with the other writes
        PantryDatabase.WRITE_EXECUTOR.execute(() -> {
            try {
                new ShoppingListGenerator(repository).generate(from, to, result -> {
                    generated.postValue(new Event<>(result));
                    generating.postValue(false);
                });
            } catch (RuntimeException e) {
                Log.e(TAG, "could not generate the shopping list", e);
                generating.postValue(false);
            }
        });
    }

    /** @return true while a generation is running */
    public LiveData<Boolean> getGenerating() {
        return generating;
    }

    /** removes a planned meal */
    public void clearMeal(@NonNull MealPlan plan) {
        repository.deleteMealPlan(plan);
        messages.setValue(new Event<>(R.string.planner_meal_removed));
    }

    /** marks a meal as cooked, or back as not cooked */
    public void toggleCooked(@NonNull MealPlan plan) {
        repository.setMealPlanCompleted(plan.id, !plan.completed);
    }

    private MealPlan findMeal(long dayStartMs, String mealType) {
        List<DayPlan> current = days.getValue();
        if (current == null) return null;
        for (DayPlan day : current) {
            if (day.dayStartMs == dayStartMs) {
                return day.get(mealType);
            }
        }
        return null;
    }
}
