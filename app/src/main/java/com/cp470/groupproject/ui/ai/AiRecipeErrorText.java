package com.cp470.groupproject.ui.ai;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;

/**
 * Turns an {@link AiError} from POST /ai/recipe into a localized message.
 * Recipe-specific codes get their own text; network, timeout and "busy" reuse the assistant's texts.
 */
final class AiRecipeErrorText {

    /** Backend: the AI kept using an allergy ingredient (after one retry). */
    static final String AI_UNSAFE_RECIPE = "AI_UNSAFE_RECIPE";
    /** Backend: the AI answer was unusable (after one retry). */
    static final String AI_BAD_FORMAT = "AI_BAD_FORMAT";

    private AiRecipeErrorText() { }

    /** A recipe-specific message, or 0 when the general assistant text fits better. Plain Java: unit-tested. */
    @StringRes
    static int specificMessage(@NonNull AiError error) {
        switch (error.code) {
            case AiError.VALIDATION_ERROR:
                return R.string.ai_gen_error_empty_pantry; // the only 400: nothing usable in the pantry
            case AI_UNSAFE_RECIPE:
                return R.string.ai_gen_error_unsafe;
            case AI_BAD_FORMAT:
            case AiError.UNEXPECTED_RESPONSE:
            case AiError.UNKNOWN:
                return R.string.ai_gen_error_failed;
            default:
                return 0;
        }
    }

    @NonNull
    static String message(@NonNull Context context, @NonNull AiError error) {
        int specific = specificMessage(error);
        return specific != 0 ? context.getString(specific) : AiErrorText.message(context, error);
    }
}