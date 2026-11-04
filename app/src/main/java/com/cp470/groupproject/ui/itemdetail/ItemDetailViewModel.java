package com.cp470.groupproject.ui.itemdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.util.Event;

/**
 * Viewmodel for the item detail screen.
 * communicates with the repository and exposes item data and operation results through livedata.
 */
public class ItemDetailViewModel extends AndroidViewModel {

    /** outcome of a database operation: a message to show and whether the screen should close */
    public static final class UiResult {
        public final int messageRes;
        public final boolean closeScreen;

        UiResult(int messageRes, boolean closeScreen) {
            this.messageRes = messageRes;
            this.closeScreen = closeScreen;
        }
    }

    private final PantryRepository repository;

    private final MutableLiveData<Long> selectedItemId = new MutableLiveData<>();

    private final LiveData<FoodItem> item;

    private final MutableLiveData<Event<UiResult>> operationResult = new MutableLiveData<>();

    public ItemDetailViewModel(@NonNull Application application) {
        super(application);

        repository = new PantryRepository(application);

        item = Transformations.switchMap(selectedItemId, repository::getItemById);
    }

    /** @return the currently selected food item */
    public LiveData<FoodItem> getItem() {
        return item;
    }

    /** @return the result of the latest database operation, delivered only once */
    public LiveData<Event<UiResult>> getOperationResult() {
        return operationResult;
    }

    /** loads an item from the database using its id */
    public void loadItem(long id) {
        Long current = selectedItemId.getValue();
        if (current == null || current != id) {
            selectedItemId.setValue(id);
        }
    }

    /** updates the quantity of the current item */
    public void updateQuantity(double quantity) {
        FoodItem currentItem = item.getValue();

        if (currentItem == null) {
            operationResult.setValue(new Event<>(new UiResult(R.string.msg_item_not_found, false)));
            return;
        }

        currentItem.quantity = quantity;

        repository.updateFoodItem(currentItem, rows -> operationResult.postValue(
                new Event<>(new UiResult(
                        rows > 0 ? R.string.msg_item_updated : R.string.msg_update_failed,
                        false))));
    }

    /** records what happened to the current item and removes it from the active inventory */
    public void recordDisposition(String disposition) {
        FoodItem currentItem = item.getValue();

        if (currentItem == null) {
            operationResult.setValue(new Event<>(new UiResult(R.string.msg_item_not_found, false)));
            return;
        }

        if (!ItemStatus.isDisposition(disposition)) {
            operationResult.setValue(new Event<>(new UiResult(R.string.msg_invalid_disposition, false)));
            return;
        }

        repository.recordDisposition(currentItem, disposition, historyId -> {
            boolean ok = historyId > 0;
            operationResult.postValue(new Event<>(new UiResult(
                    ok ? R.string.msg_disposition_recorded : R.string.msg_disposition_failed,
                    ok)));
        });
    }

    /** permanently deletes the current item */
    public void deleteItem() {
        FoodItem currentItem = item.getValue();

        if (currentItem == null) {
            operationResult.setValue(new Event<>(new UiResult(R.string.msg_item_not_found, false)));
            return;
        }

        repository.deleteFoodItem(currentItem, rows -> {
            boolean ok = rows > 0;
            operationResult.postValue(new Event<>(new UiResult(
                    ok ? R.string.msg_item_deleted : R.string.msg_delete_failed,
                    ok)));
        });
    }
}