package com.cp470.groupproject.ui.ai;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;

/**
 * Turns an {@link AiError} into a localized message for the Recipes screen.
 * (The chat has its own wording in AiErrorText: "the assistant…".)
 */
final class RecipeErrorText {

    /** Edamam quota reached (from the backend). */
    static final String RECIPES_RATE_LIMITED = "RECIPES_RATE_LIMITED";

    private RecipeErrorText() { }

    @NonNull
    static String message(@NonNull Context context, @NonNull AiError error) {
        if (isBusy(error)) {
            return context.getString(R.string.ai_recipes_error_busy);
        }
        if (AiError.NETWORK_ERROR.equals(error.code) || AiError.TIMEOUT.equals(error.code)) {
            return context.getString(R.string.ai_recipes_error_network);
        }
        return context.getString(R.string.ai_recipes_error_generic);
    }

    /** Our limit or Edamam's: the user should wait, not tap Retry right away. */
    static boolean isBusy(@NonNull AiError error) {
        return error.isRateLimited() || RECIPES_RATE_LIMITED.equals(error.code);
    }

    /** Busy and invalid requests won't succeed on an immediate retry. */
    static boolean isRetryable(@NonNull AiError error) {
        return !isBusy(error) && !AiError.VALIDATION_ERROR.equals(error.code);
    }
}