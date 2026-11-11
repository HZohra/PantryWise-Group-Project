package com.cp470.groupproject.ui.ai;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;

/**
 * Turns an {@link AiError} into a localized message for the user.
 * AiError.message is English and for logs only; the UI always uses these strings.
 */
final class AiErrorText {

    private AiErrorText() { }

    @NonNull
    static String message(@NonNull Context context, @NonNull AiError error) {
        if (error.isRateLimited()) {
            Integer seconds = error.retryAfterSeconds;
            return seconds != null && seconds > 0
                    ? context.getString(R.string.ai_error_rate_limited_seconds, seconds)
                    : context.getString(R.string.ai_error_rate_limited);
        }
        switch (error.code) {
            case AiError.NETWORK_ERROR:
                return context.getString(R.string.ai_error_network);
            case AiError.TIMEOUT:
            case "AI_TIMEOUT":
                return context.getString(R.string.ai_error_timeout);
            case "AI_UNAVAILABLE":
            case "AI_CONFIG_ERROR":
                return context.getString(R.string.ai_error_unavailable);
            default:
                return context.getString(R.string.ai_error_generic);
        }
    }

    /** Rate limits say "try again shortly" (no button); bad input won't succeed on retry. */
    static boolean isRetryable(@NonNull AiError error) {
        return !error.isRateLimited() && !AiError.VALIDATION_ERROR.equals(error.code);
    }
}