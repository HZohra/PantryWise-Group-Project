package com.cp470.groupproject.data.remote;

import androidx.annotation.Nullable;

/** Either a successful value OR an {@link AiError}, never both. */
public final class ApiResult<T> {

    @Nullable private final T data;
    @Nullable private final AiError error;

    private ApiResult(@Nullable T data, @Nullable AiError error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(data, null);
    }

    public static <T> ApiResult<T> failure(AiError error) {
        return new ApiResult<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public AiError getError() {
        return error;
    }
}