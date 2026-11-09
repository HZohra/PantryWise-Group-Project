package com.cp470.groupproject.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.AiApiService;
import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.ApiClient;
import com.cp470.groupproject.data.remote.ApiResult;
import com.cp470.groupproject.data.remote.InventoryMapper;
import com.cp470.groupproject.data.remote.dto.ApiErrorResponse;
import com.cp470.groupproject.data.remote.dto.ChatMessageDto;
import com.cp470.groupproject.data.remote.dto.ChatRequest;
import com.cp470.groupproject.data.remote.dto.ChatResponse;
import com.cp470.groupproject.data.remote.dto.InventoryItemDto;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Single entry point for the AI assistant's data (Team Member 5).
 * Reads the pantry from PantryRepository (read-only) and talks to the backend through Retrofit.
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
    private final Gson gson = new Gson();

    public AiRepository(@NonNull Application application) {
        this(new PantryRepository(application), ApiClient.getService(), Executors.newSingleThreadExecutor());
    }

    public AiRepository(@NonNull PantryRepository pantry, @NonNull AiApiService api, @NonNull Executor executor) {
        this.pantry = pantry;
        this.api = api;
        this.executor = executor;
    }

    public void sendChatMessage(@NonNull String message,
                                @NonNull List<ChatMessageDto> history,
                                @NonNull Callback<String> callback) {
        executor.execute(() -> {
            ApiResult<String> result;
            try {
                List<InventoryItemDto> inventory = InventoryMapper.toDtos(
                        pantry.getActiveItemsSync(), System.currentTimeMillis());
                ChatRequest request = new ChatRequest(message, lastN(history, MAX_HISTORY), inventory);

                ApiResult<ChatResponse> response = execute(api.chat(request));
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

    @NonNull
    <T> ApiResult<T> execute(@NonNull Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                return ApiResult.success(response.body());
            }
            return ApiResult.failure(parseError(response));
        } catch (SocketTimeoutException e) { // before IOException: it's a subclass
            return ApiResult.failure(new AiError(AiError.TIMEOUT, "The server took too long to answer", 0, null));
        } catch (IOException e) {
            return ApiResult.failure(new AiError(AiError.NETWORK_ERROR, "Could not reach the server", 0, null));
        }
    }

    @NonNull
    private AiError parseError(@NonNull Response<?> response) {
        Integer retryAfter = parseSeconds(response.headers().get("Retry-After"));
        try (ResponseBody body = response.errorBody()) {
            if (body != null) {
                ApiErrorResponse parsed = gson.fromJson(body.charStream(), ApiErrorResponse.class);
                if (parsed != null && parsed.error != null && parsed.error.code != null) {
                    return new AiError(parsed.error.code, parsed.error.message, response.code(), retryAfter);
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "could not parse error body for HTTP " + response.code(), e);
        }
        return new AiError(AiError.UNEXPECTED_RESPONSE, "HTTP " + response.code(), response.code(), retryAfter);
    }

    @Nullable
    private static Integer parseSeconds(@Nullable String header) {
        if (header == null) return null;
        try {
            return Integer.valueOf(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    static <T> List<T> lastN(@NonNull List<T> list, int n) {
        return list.size() <= n ? list : new ArrayList<>(list.subList(list.size() - n, list.size()));
    }
}