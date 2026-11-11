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
import com.cp470.groupproject.data.remote.dto.SubstituteRequest;
import com.cp470.groupproject.data.remote.dto.SubstituteResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Recipe data for the Recipes and Recipe details screens (Team Member 5).
 * Both "recommend from my pantry" and keyword search go through /recipes/recommend,
 * so every result is ranked by the same pantry match ("You have X of Y", expiring badge),
 * recipes with the user's allergens are flagged and listed last (or removed when the user
 * chose to hide them), and an empty search is broadened once (all done by the backend).
 * Substitutions go through /recipes/substitute (local swap list first, Claude only if needed).
 * Callbacks are delivered ON A BACKGROUND THREAD: callers must use LiveData.postValue().
 */
public class RecipeRepository {

    private static final String TAG = "RecipeRepository";
    /** Backend LIMITS.queryMax. */
    public static final int MAX_QUERY_LENGTH = 100;
    /** Backend LIMITS.titleMax, nameMax, ingredientLinesMax and lineMax. */
    static final int MAX_TITLE_LENGTH = 150;
    static final int MAX_INGREDIENT_LENGTH = 100;
    static final int MAX_LINES = 50;
    static final int MAX_LINE_LENGTH = 200;

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
     * Pantry-ranked recipes, best match first; recipes with an allergen are flagged and listed last.
     * @param query       null or blank = recommend from the pantry; otherwise search this keyword
     * @param preferences the saved food profile, or null when nothing is saved
     * @param zeroWaste   true = only recipes that use expiring food, ranked by urgency
     * @param limit       clamped to 1..{@link RecommendRequest#MAX_LIMIT}
     */
    public void recommend(@Nullable String query,
                          @Nullable PreferencesDto preferences,
                          boolean zeroWaste,
                          int limit,
                          @NonNull Callback<RecommendResponse> callback) {
        recommend(query, preferences, false, zeroWaste, limit, callback);
    }

    /**
     * Same, plus the user's "Hide recipes that contain my allergies" switch.
     * @param hideAllergens true = recipes with an allergen are removed; false = flagged and listed last
     */
    public void recommend(@Nullable String query,
                          @Nullable PreferencesDto preferences,
                          boolean hideAllergens,
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
                request.hideAllergens = hideAllergens;
                result = ApiCalls.execute(api.recommendRecipes(request));
            } catch (RuntimeException e) {
                Log.e(TAG, "recommend failed unexpectedly", e);
                result = ApiResult.failure(new AiError(AiError.UNKNOWN, e.getMessage(), 0, null));
            }
            callback.onResult(result);
        });
    }

    /**
     * Substitutes for one missing ingredient, items the user owns first, in the app language (US-3).
     * @param recipeTitle     shown to the AI as context
     * @param ingredient      the food name, e.g. "egg" (IngredientMatcher gives it), not the whole line
     * @param ingredientLines the recipe's lines, as context
     * @param preferences     the saved food profile, or null when nothing is saved
     */
    public void substitute(@NonNull String recipeTitle,
                           @NonNull String ingredient,
                           @Nullable List<String> ingredientLines,
                           @Nullable PreferencesDto preferences,
                           @NonNull Callback<SubstituteResponse> callback) {
        String cleanTitle = clampText(recipeTitle, MAX_TITLE_LENGTH);
        String cleanIngredient = clampText(ingredient, MAX_INGREDIENT_LENGTH);
        List<String> cleanLines = cleanLines(ingredientLines);

        executor.execute(() -> {
            ApiResult<SubstituteResponse> result;
            try {
                List<InventoryItemDto> inventory = InventoryMapper.toDtos(
                        pantry.getActiveItemsSync(), System.currentTimeMillis());
                SubstituteRequest request = new SubstituteRequest(cleanTitle, cleanIngredient, cleanLines,
                        inventory, AiRepository.appLanguageTag(), preferences);
                result = ApiCalls.execute(api.substitute(request));
            } catch (RuntimeException e) {
                Log.e(TAG, "substitute failed unexpectedly", e);
                result = ApiResult.failure(new AiError(AiError.UNKNOWN, e.getMessage(), 0, null));
            }
            callback.onResult(result);
        });
    }

    /** Trimmed and capped at the backend limit; null when there is nothing to search for. */
    @Nullable
    public static String cleanQuery(@Nullable String query) {
        if (query == null) return null;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= MAX_QUERY_LENGTH ? trimmed : trimmed.substring(0, MAX_QUERY_LENGTH).trim();
    }

    /** Trimmed and cut to max characters (never null). */
    @NonNull
    static String clampText(@Nullable String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max).trim();
    }

    /** At most MAX_LINES non-blank lines, each at most MAX_LINE_LENGTH characters. */
    @NonNull
    static List<String> cleanLines(@Nullable List<String> lines) {
        List<String> result = new ArrayList<>();
        if (lines == null) return result;
        for (String line : lines) {
            String clean = clampText(line, MAX_LINE_LENGTH);
            if (!clean.isEmpty()) result.add(clean);
            if (result.size() == MAX_LINES) break;
        }
        return result;
    }
}