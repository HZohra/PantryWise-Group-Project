package com.cp470.groupproject.ui.inventory;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.StorageLocation;
import com.cp470.groupproject.data.repository.PantryRepository;

import java.util.List;

public class InventoryViewModel extends AndroidViewModel {

    private final PantryRepository repository;
    private final MutableLiveData<Long> selectedLocationId = new MutableLiveData<>();
    private final LiveData<List<FoodItem>> foodItems;

    public InventoryViewModel(@NonNull Application application) {
        super(application);

        repository = new PantryRepository(application);
        selectedLocationId.setValue(StorageLocation.FRIDGE_ID);

        foodItems = Transformations.switchMap(
                selectedLocationId,
                locationId -> repository.getItemsByLocation(locationId)
        );
    }

    public LiveData<List<FoodItem>> getFoodItems() {
        return foodItems;
    }

    public void selectLocation(long locationId) {
        selectedLocationId.setValue(locationId);
    }
}