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
import com.cp470.groupproject.data.remote.dto.AiRecipeRequest;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.cp470.groupproject.data.remote.dto.ChatMessageDto;
import com.cp470.groupproject.data.remote.dto.ChatRequest;
import com.cp470.groupproject.data.remote.dto.ChatResponse;
import com.cp470.groupproject.data.remote.dto.InventoryItemDto;
import com.cp470.groupproject.data.remote.dto.PreferencesDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Single entry point for the AI assistant's data (Team Member 5): chat and AI recipes.
 * Reads the pantry from PantryRepository (read-only) and talks to the backend through Retrofit.
 * Every request carries the app language, so replies come back in English or Spanish (US-8).
 * Callbacks are delivered ON A BACKGROUND THREAD: callers must use LiveData.postValue().
 */
public class AiRepository {

    private static final String TAG = "AiRepository";
    static final int MAX_HISTORY = 20; // LIMITS.historyMax

    public interface Callback<T> {
        void onResult(@NonNull ApiResult<T> result);
    }

    private final PantryRepository pantry;
    private final AiApiService api;
    private final Executor executor;

    public AiRepository(@NonNull Application application) {
        this(new PantryRepository(application), ApiClient.getService(), Executors.newSingleThreadExecutor());
    }

    public AiRepository(@NonNull PantryRepository pantry, @NonNull AiApiService api, @NonNull Executor executor) {
        this.pantry = pantry;
        this.api = api;
        this.executor = executor;
    }

    /** Sends a chat message without saved preferences. */
    public void sendChatMessage(@NonNull String message,
                                @NonNull List<ChatMessageDto> history,
                                @NonNull Callback<String> callback) {
        sendChatMessage(message, history, null, callback);
    }

    /**
     * Sends a chat message with the current pantry, the app language and (optionally) saved preferences.
     * @param preferences null until the preferences screen exists (G16)
     */
    public void sendChatMessage(@NonNull String message,
                                @NonNull List<ChatMessageDto> history,
                                @Nullable PreferencesDto preferences,
                                @NonNull Callback<String> callback) {
        executor.execute(() -> {
            ApiResult<String> result;
            try {
                List<InventoryItemDto> inventory = InventoryMapper.toDtos(
                        pantry.getActiveItemsSync(), System.currentTimeMillis());
                ChatRequest request = new ChatRequest(message, lastN(history, MAX_HISTORY), inventory,
                        appLanguageTag(), preferences);

                ApiResult<ChatResponse> response = ApiCalls.execute(api.chat(request));
                result = response.isSuccess()
                        ? ApiResult.success(response.getData().reply)
                        : ApiResult.failure(response.getError());
            } catch (RuntimeException e) {
                Log.e(TAG, "chat failed unexpectedly", e);
                result = ApiResult.failure(new AiError(AiError.UNKNOWN, e.getMessage(), 0, null));
            }
            callback.onResult(result);
        });
    }

    /**
     * One AI recipe built around the pantry's expiring food (US-2, UC-2), in the app language.
     * The backend checks the structure and allergens and retries once if needed.
     * @param preferences null until the preferences screen exists (G16)
     * @param servings    clamped to 1..{@link AiRecipeRequest#MAX_SERVINGS}
     */
    public void generateRecipe(@Nullable PreferencesDto preferences,
                               int servings,
                               @NonNull Callback<AiRecipeResponse> callback) {
        int cleanServings = Math.max(1, Math.min(servings, AiRecipeRequest.MAX_SERVINGS));
        executor.execute(() -> {
            ApiResult<AiRecipeResponse> result;
            try {
                List<InventoryItemDto> inventory = InventoryMapper.toDtos(
                        pantry.getActiveItemsSync(), System.currentTimeMillis());
                AiRecipeRequest request = new AiRecipeRequest(inventory, appLanguageTag(), preferences, cleanServings);
                result = ApiCalls.execute(api.generateRecipe(request));
            } catch (RuntimeException e) {
                Log.e(TAG, "AI recipe failed unexpectedly", e);
                result = ApiResult.failure(new AiError(AiError.UNKNOWN, e.getMessage(), 0, null));
            }
            callback.onResult(result);
        });
    }

    /**
     * The app's current language as a tag such as "en-US" or "es-MX".
     * Follows the device language (and a per-app language, if the team adds one).
     */
    @NonNull
    static String appLanguageTag() {
        return Locale.getDefault().toLanguageTag();
    }

    @NonNull
    static <T> List<T> lastN(@NonNull List<T> list, int n) {
        return list.size() <= n ? list : new ArrayList<>(list.subList(list.size() - n, list.size()));
    }
}