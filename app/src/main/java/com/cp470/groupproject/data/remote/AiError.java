package com.cp470.groupproject.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A failure the UI can react to.
 * {@link #code} is either the backend's error code (e.g. "AI_TIMEOUT", "RATE_LIMITED")
 * or one of the client-side codes below, used when no usable answer came back from the backend.
 */
public final class AiError {

    // ---- client-side codes (the phone never got a usable answer) ----
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String UNEXPECTED_RESPONSE = "UNEXPECTED_RESPONSE";
    public static final String UNKNOWN = "UNKNOWN";

    // ---- backend codes the UI handles specially ----
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String AI_RATE_LIMITED = "AI_RATE_LIMITED";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @NonNull public final String code;
    /** English, for logs only. The UI shows its own localized string based on {@link #code}. */
    @Nullable public final String message;
    /** HTTP status, or 0 when there was no HTTP response at all. */
    public final int httpStatus;
    /** From the Retry-After header (seconds), when the server sent one. */
    @Nullable public final Integer retryAfterSeconds;

    public AiError(@NonNull String code, @Nullable String message, int httpStatus, @Nullable Integer retryAfterSeconds) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isRateLimited() {
        return RATE_LIMITED.equals(code) || AI_RATE_LIMITED.equals(code);
    }

    @NonNull
    @Override
    public String toString() {
        return "AiError{" + code + ", http=" + httpStatus + ", message=" + message + "}";
    }
}