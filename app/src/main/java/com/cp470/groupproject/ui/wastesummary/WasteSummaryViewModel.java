package com.cp470.groupproject.ui.wastesummary;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.data.model.WasteStats;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.util.ExpiryUtils;

/**
 * Viewmodel for the waste summary screen.
 * exposes the waste statistics of the selected period through livedata.
 */
public class WasteSummaryViewModel extends AndroidViewModel {

    private final MutableLiveData<Long> since = new MutableLiveData<>();

    private final LiveData<WasteStats> stats;

    /** last selected period, used to avoid reloading when the same period is selected again */
    private int currentDays = -1;

    public WasteSummaryViewModel(@NonNull Application application) {
        super(application);

        PantryRepository repository = new PantryRepository(application);

        stats = Transformations.switchMap(since, repository::getWasteStats);
    }

    /** @return the statistics of the selected period */
    public LiveData<WasteStats> getStats() {
        return stats;
    }

    /**
     * selects the look-back period
     * @param days number of days to look back, 0 or less means all time
     */
    public void setPeriodDays(int days) {
        if (days == currentDays) return;

        currentDays = days;
        since.setValue(days <= 0
                ? 0L
                : System.currentTimeMillis() - days * ExpiryUtils.DAY_MS);
    }
}