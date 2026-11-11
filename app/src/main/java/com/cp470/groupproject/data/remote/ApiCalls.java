package com.cp470.groupproject.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.ApiErrorResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Runs a Retrofit call synchronously and turns every outcome into an {@link ApiResult}.
 * Shared by AiRepository and RecipeRepository, so both handle errors identically.
 * Call it from a background thread only.
 */
public final class ApiCalls {

    private static final String TAG = "ApiCalls";
    private static final Gson GSON = new Gson();

    private ApiCalls() { }

    @NonNull
    public static <T> ApiResult<T> execute(@NonNull Call<T> call) {
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

    /** Reads the backend's {"error":{...}} body and the Retry-After header. Package-private for tests. */
    @NonNull
    static AiError parseError(@NonNull Response<?> response) {
        Integer retryAfter = parseSeconds(response.headers().get("Retry-After"));
        try (ResponseBody body = response.errorBody()) {
            if (body != null) {
                ApiErrorResponse parsed = GSON.fromJson(body.charStream(), ApiErrorResponse.class);
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
}