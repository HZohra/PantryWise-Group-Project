package com.cp470.groupproject.ui.ai;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.RecommendResponse;
import com.cp470.groupproject.data.repository.RecipeRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State and logic for the Recipes screen (MVVM: no Views here).
 * Survives rotation; RecipeRepository calls back on a background thread, so those updates use postValue().
 */
public class RecipeViewModel extends AndroidViewModel {

    /** Member 5's own preferences file, until the team's preferences screen exists (G16). */
    static final String PREFS_NAME = "pantrywise_ai";
    static final String KEY_ZERO_WASTE = "zero_waste_mode";
    static final int RESULT_LIMIT = 10;

    private final RecipeRepository repository;
    private final SharedPreferences prefs;

    private final MutableLiveData<List<RecipeDto>> recipes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** String resource id of the notice line, or 0 for none. */
    private final MutableLiveData<Integer> notice = new MutableLiveData<>(0);
    /** True after a successful load that found nothing. */
    private final MutableLiveData<Boolean> empty = new MutableLiveData<>(false);
    private final MutableLiveData<AiError> error = new MutableLiveData<>(null);

    /** Only the newest request may update the screen. */
    private final AtomicInteger latestRequest = new AtomicInteger();

    private boolean zeroWaste;
    /** null = recommendations from the pantry; otherwise the searched keyword. */
    @Nullable private String query;
    private boolean loaded;

    public RecipeViewModel(@NonNull Application application) {
        this(application, new RecipeRepository(application));
    }

    RecipeViewModel(@NonNull Application application, @NonNull RecipeRepository repository) {
        super(application);
        this.repository = repository;
        this.prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.zeroWaste = prefs.getBoolean(KEY_ZERO_WASTE, false);
    }

    public LiveData<List<RecipeDto>> getRecipes() { return recipes; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<Integer> getNotice() { return notice; }
    public LiveData<Boolean> isEmpty() { return empty; }
    /** Non-null when an error should be shown; call {@link #onErrorShown()} after showing it. */
    public LiveData<AiError> getError() { return error; }

    public boolean isZeroWaste() { return zeroWaste; }

    /** Main thread. Loads recommendations the first time only (not again after rotation). */
    public void loadIfNeeded() {
        if (!loaded) {
            load();
        }
    }

    /**
     * Main thread. Searches a keyword, ranked by the pantry.
     * @return false if there is nothing to search for (the screen shows a validation message instead)
     */
    public boolean search(@Nullable String rawQuery) {
        String clean = RecipeRepository.cleanQuery(rawQuery);
        if (clean == null) {
            return false;
        }
        query = clean;
        load();
        return true;
    }

    /** Main thread. Back to pantry recommendations (does nothing if no search is active). */
    public void clearSearch() {
        if (query == null) {
            return;
        }
        query = null;
        load();
    }

    /** Main thread. Saves the switch and reloads (does nothing if the value didn't change, e.g. after rotation). */
    public void setZeroWaste(boolean enabled) {
        if (enabled == zeroWaste) {
            return;
        }
        zeroWaste = enabled;
        prefs.edit().putBoolean(KEY_ZERO_WASTE, enabled).apply();
        load();
    }

    /** Main thread. Repeats the last request. */
    public void retry() {
        load();
    }

    /** Main thread. Call after the Snackbar is shown so it doesn't reappear on rotation. */
    public void onErrorShown() {
        error.setValue(null);
    }

    /** Main thread. */
    private void load() {
        loaded = true;
        final int request = latestRequest.incrementAndGet();
        final boolean zeroWasteRequested = zeroWaste;
        loading.setValue(true);
        error.setValue(null);

        repository.recommend(query, null, zeroWasteRequested, RESULT_LIMIT, result -> {
            // background thread from here on
            if (request != latestRequest.get()) {
                return; // a newer search or toggle replaced this request
            }
            RecommendResponse data = result.getData();
            if (result.isSuccess() && data != null) {
                List<RecipeDto> list = data.recipes == null ? Collections.emptyList() : data.recipes;
                recipes.postValue(list);
                notice.postValue(chooseNotice(data, zeroWasteRequested));
                empty.postValue(list.isEmpty());
            } else {
                AiError failure = result.getError() != null
                        ? result.getError()
                        : new AiError(AiError.UNEXPECTED_RESPONSE, "Empty response", 200, null);
                error.postValue(failure); // the old list stays visible
            }
            loading.postValue(false);
        });
    }

    /**
     * Which notice to show above the list (0 = none):
     * empty pantry (UC 2a) → broadened search (UC 4c) → Zero-Waste with nothing expiring.
     */
    @StringRes
    static int chooseNotice(@NonNull RecommendResponse data, boolean zeroWasteRequested) {
        if (!data.personalized) {
            return R.string.ai_recipes_empty_pantry;
        }
        if (data.broadened) {
            return R.string.ai_recipes_broadened;
        }
        if (zeroWasteRequested && data.recipes != null && !data.recipes.isEmpty() && noneUseExpiring(data.recipes)) {
            return R.string.ai_recipes_nothing_expiring;
        }
        return 0;
    }

    private static boolean noneUseExpiring(@NonNull List<RecipeDto> list) {
        for (RecipeDto recipe : list) {
            if (recipe.match != null && recipe.match.expiringUsed != null && !recipe.match.expiringUsed.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}