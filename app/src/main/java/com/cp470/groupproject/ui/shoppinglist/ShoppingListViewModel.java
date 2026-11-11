package com.cp470.groupproject.ui.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.shopping.ShoppingListGenerator;
import com.cp470.groupproject.shopping.StorePreferences;
import com.cp470.groupproject.util.Event;
import com.cp470.groupproject.util.ShoppingInputValidator;
import com.cp470.groupproject.util.ShoppingListFormatter;
import com.cp470.groupproject.util.StoreGrouper;
import com.cp470.groupproject.util.StoreGrouper.ShoppingRow;
import com.cp470.groupproject.util.StoreType;
import com.cp470.groupproject.util.WeekUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * State and actions of the shopping list screen. it talks only to the repository, never to views.
 */
public class ShoppingListViewModel extends AndroidViewModel {

    private static final String TAG = "ShoppingListViewModel";

    private final PantryRepository repository;
    private final MutableLiveData<Boolean> generating = new MutableLiveData<>(false);
    private final LiveData<List<ShoppingItem>> items;
    private final LiveData<int[]> summary;
    private final MutableLiveData<Event<UiMessage>> messages = new MutableLiveData<>();
    private final StorePreferences stores;
    private final MutableLiveData<Map<StoreType, String>> storeNames;
    private final MutableLiveData<Boolean> groupByStore;
    private final MediatorLiveData<List<ShoppingRow>> rows = new MediatorLiveData<>();

    /** last deleted item, kept so the user can undo */
    private ShoppingItem lastDeleted;

    /** @param application used to create the repository */
    public ShoppingListViewModel(@NonNull Application application) {
        super(application);
        repository = new PantryRepository(application);
        items = repository.getShoppingItems();
        summary = Transformations.map(items, ShoppingListViewModel::summarize);

        // small settings file: read once here, then kept in memory by android
        stores = StorePreferences.from(application);
        storeNames = new MutableLiveData<>(stores.getStoreNames());
        groupByStore = new MutableLiveData<>(stores.isGroupByStore());
        // the rows of the screen depend on the list and on both settings
        rows.addSource(items, list -> updateRows());
        rows.addSource(storeNames, names -> updateRows());
        rows.addSource(groupByStore, grouped -> updateRows());
    }

    /** @return the list as rows: grouped by shop with headers, or flat, depending on the setting */
    public LiveData<List<ShoppingRow>> getRows() {
        return rows;
    }

    /** @return the names the user gave to each kind of shop */
    public Map<StoreType, String> getStoreNames() {
        return storeNames.getValue();
    }

    /** saves the names of the shops; a blank name goes back to the generic one */
    public void saveStoreNames(@NonNull Map<StoreType, String> names) {
        for (StoreType type : StoreType.values()) {
            stores.setStoreName(type, names.get(type));
        }
        storeNames.setValue(stores.getStoreNames());
    }

    /** @return true if the list is grouped by shop */
    public boolean isGroupByStore() {
        return Boolean.TRUE.equals(groupByStore.getValue());
    }

    /** saves and applies whether the list is grouped by shop */
    public void setGroupByStore(boolean grouped) {
        stores.setGroupByStore(grouped);
        groupByStore.setValue(grouped);
    }

    private void updateRows() {
        List<ShoppingItem> list = items.getValue();
        if (list == null) {
            return;
        }
        rows.setValue(isGroupByStore()
                ? StoreGrouper.toRows(StoreGrouper.group(list, storeNames.getValue()))
                : StoreGrouper.flat(list));
    }

    /** @return the list, ticked items last */
    public LiveData<List<ShoppingItem>> getItems() {
        return items;
    }

    /** @return {pending, completed} counts of the list */
    public LiveData<int[]> getSummary() {
        return summary;
    }

    /** @return one-shot messages for the screen */
    public LiveData<Event<UiMessage>> getMessages() {
        return messages;
    }

    private static int[] summarize(List<ShoppingItem> list) {
        int done = 0;
        if (list != null) {
            for (ShoppingItem item : list) {
                if (item.checked) done++;
            }
        }
        int total = list == null ? 0 : list.size();
        return new int[] {total - done, done};
    }

    /**
     * validates the form and adds the item.
     *
     * @return true when the item was added, so the screen can clear the form
     */
    public boolean addItem(String name, String qtyText, String unit, String store) {
        ShoppingInputValidator.Result result = ShoppingInputValidator.validate(name, qtyText);
        if (!result.isValid()) {
            postValidationError(result);
            return false;
        }
        repository.addShoppingItem(new ShoppingItem(result.name, result.quantity,
                clean(unit), emptyToNull(store)), null);
        messages.setValue(new Event<>(UiMessage.of(R.string.shopping_added, result.name)));
        return true;
    }

    /** ticks or unticks an item */
    public void setChecked(@NonNull ShoppingItem item, boolean checked) {
        repository.setShoppingItemChecked(item.id, checked);
    }

    /**
     * validates the edit dialog and saves the changes.
     *
     * @return true when saved, false when the input is invalid (the dialog stays open)
     */
    public boolean editItem(@NonNull ShoppingItem item, String name, String qtyText,
                            String unit, String store) {
        ShoppingInputValidator.Result result = ShoppingInputValidator.validate(name, qtyText);
        if (!result.isValid()) {
            return false;
        }
        // the list may be showing this very object, so edit a copy to let DiffUtil see the change
        ShoppingItem edited = copyOf(item);
        edited.name = result.name;
        edited.quantity = result.quantity;
        edited.unit = clean(unit);
        edited.store = emptyToNull(store);
        repository.updateShoppingItem(edited);
        return true;
    }

    /** @return the validation error of the edit dialog, or NONE if the input is fine */
    public ShoppingInputValidator.Error validate(String name, String qtyText) {
        return ShoppingInputValidator.validate(name, qtyText).error;
    }

    /** deletes an item and offers to undo it */
    public void deleteItem(@NonNull ShoppingItem item) {
        lastDeleted = copyOf(item);
        repository.deleteShoppingItem(item);
        messages.setValue(new Event<>(UiMessage.undoable(R.string.shopping_deleted)));
    }

    /** puts back the last deleted item (as a new row, keeping its ticked state) */
    public void undoDelete() {
        if (lastDeleted == null) return;
        ShoppingItem restored = copyOf(lastDeleted);
        restored.id = 0;
        repository.addShoppingItem(restored, null);
        lastDeleted = null;
    }

    /** removes all ticked items and reports how many were removed */
    public void clearCompleted() {
        // the callback runs on a background thread
        repository.clearCompletedShoppingItems(count ->
                messages.postValue(new Event<>(
                        UiMessage.plural(R.plurals.shopping_cleared, count))));
    }

    /**
     * fills the list with what is missing for the meals planned in a period.
     *
     * @param from start of the period, in milliseconds
     * @param to end of the period (included), in milliseconds
     */
    public void generateFromPlan(long from, long to) {
        // a second tap while the first generation runs would only repeat the work
        if (Boolean.TRUE.equals(generating.getValue())) return;
        generating.setValue(true);
        // on the write thread, so it runs in order with the other writes
        PantryDatabase.WRITE_EXECUTOR.execute(() -> {
            try {
                new ShoppingListGenerator(repository).generate(from, to, result -> {
                    messages.postValue(new Event<>(generationMessage(result)));
                    generating.postValue(false);
                });
            } catch (RuntimeException e) {
                Log.e(TAG, "could not generate the shopping list", e);
                generating.postValue(false);
            }
        });
    }

    /** @return the message for a generation: no meals to look at, nothing missing, or the count */
    private static UiMessage generationMessage(ShoppingListGenerator.Result result) {
        if (result.meals == 0) {
            return UiMessage.of(R.string.shopping_generated_no_meals);
        }
        if (result.added == 0) {
            return UiMessage.of(R.string.shopping_generated_none);
        }
        return UiMessage.plural(R.plurals.shopping_generated, result.added);
    }

    /** @return true while a generation is running */
    public LiveData<Boolean> getGenerating() {
        return generating;
    }

    /** fills the list from the meals planned from today and the next 7 days */
    public void generateForNextDays() {
        ZoneId zone = ZoneId.systemDefault();
        long from = WeekUtils.startOfDay(System.currentTimeMillis(), zone);
        generateFromPlan(from, WeekUtils.plusDays(from, 7, zone) - 1);
    }

    /** @return how many items of the current list are ticked */
    public int completedCount() {
        int[] counts = summary.getValue();
        return counts == null ? 0 : counts[1];
    }

    /** @return the list as plain text for sharing, or an empty string if there is no list yet */
    public String buildShareText(String header) {
        List<ShoppingItem> list = items.getValue();
        return list == null ? "" : ShoppingListFormatter.format(header, list);
    }

    private void postValidationError(ShoppingInputValidator.Result result) {
        int res = result.error == ShoppingInputValidator.Error.NAME_REQUIRED
                ? R.string.shopping_error_name : R.string.shopping_error_qty;
        messages.setValue(new Event<>(UiMessage.of(res)));
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }

    private static String emptyToNull(String text) {
        String clean = clean(text);
        return clean.isEmpty() ? null : clean;
    }

    private static ShoppingItem copyOf(ShoppingItem source) {
        ShoppingItem copy = new ShoppingItem(source.name, source.quantity, source.unit, source.store);
        copy.id = source.id;
        copy.normalizedName = source.normalizedName;
        copy.checked = source.checked;
        copy.autoGenerated = source.autoGenerated;
        copy.createdAt = source.createdAt;
        return copy;
    }
}
