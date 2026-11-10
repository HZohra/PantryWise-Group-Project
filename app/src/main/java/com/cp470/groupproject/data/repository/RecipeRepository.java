package com.cp470.groupproject.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.AiApiService;
import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.ApiCalls;
import com.cp470.groupproject.data.remote.ApiClient;
import com.cp470.groupproject.data.remote.ApiResult;
import com.cp470.groupproject.data.remote.InventoryMapper;
import com.cp470.groupproject.data.remote.dto.InventoryItemDto;
import com.cp470.groupproject.data.remote.dto.PreferencesDto;
import com.cp470.groupproject.data.remote.dto.RecommendRequest;
import com.cp470.groupproject.data.remote.dto.RecommendResponse;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Recipe data for the Recipes screen (Team Member 5).
 * Both "recommend from my pantry" and keyword search go through /recipes/recommend,
 * so every result is ranked by the same pantry match ("You have X of Y", expiring badge),
 * allergens are filtered out and an empty search is broadened once (all done by the backend).
 * Callbacks are delivered ON A BACKGROUND THREAD: callers must use LiveData.postValue().
 */
public class RecipeRepository {

    private static final String TAG = "RecipeRepository";
    /** Backend LIMITS.queryMax. */
    public static final int MAX_QUERY_LENGTH = 100;

    public interface Callback<T> {
        void onResult(@NonNull ApiResult<T> result);
    }

    private final PantryRepository pantry;
    private final AiApiService api;
    private final Executor executor;

    public RecipeRepository(@NonNull Application application) {
        this(new PantryRepository(application), ApiClient.getService(), Executors.newSingleThreadExecutor());
    }

    public RecipeRepository(@NonNull PantryRepository pantry, @NonNull AiApiService api, @NonNull Executor executor) {
        this.pantry = pantry;
        this.api = api;
        this.executor = executor;
    }

    /**
     * Pantry-ranked recipes, best match first.
     * @param query       null or blank = recommend from the pantry; otherwise search this keyword
     * @param preferences null until the preferences screen exists (G16)
     * @param zeroWaste   true = only recipes that use expiring food, ranked by urgency
     * @param limit       clamped to 1..{@link RecommendRequest#MAX_LIMIT}
     */
    public void recommend(@Nullable String query,
                          @Nullable PreferencesDto preferences,
                          boolean zeroWaste,
                          int limit,
                          @NonNull Callback<RecommendResponse> callback) {
        String cleanQuery = cleanQuery(query);
        int cleanLimit = Math.max(1, Math.min(limit, RecommendRequest.MAX_LIMIT));
        String mode = zeroWaste ? RecommendRequest.MODE_ZERO_WASTE : RecommendRequest.MODE_STANDARD;

        executor.execute(() -> {
            ApiResult<RecommendResponse> result;
            try {
                List<InventoryItemDto> inventory = InventoryMapper.toDtos(
                        pantry.getActiveItemsSync(), System.currentTimeMillis());
                RecommendRequest request = new RecommendRequest(inventory, cleanQuery, preferences, cleanLimit, mode);
                result = ApiCalls.execute(api.recommendRecipes(request));
            } catch (RuntimeException e) {
                Log.e(TAG, "recommend failed unexpectedly", e);
                result = ApiResult.failure(new AiError(AiError.UNKNOWN, e.getMessage(), 0, null));
            }
            callback.onResult(result);
        });
    }

    /** Trimmed and capped at the backend limit; null when there is nothing to search for. */
    @Nullable
    static String cleanQuery(@Nullable String query) {
        if (query == null) return null;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= MAX_QUERY_LENGTH ? trimmed : trimmed.substring(0, MAX_QUERY_LENGTH).trim();
    }
}