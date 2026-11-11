package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;

import org.junit.Test;

public class AiRecipeErrorTextTest {

    private static AiError error(String code) {
        return new AiError(code, null, 0, null);
    }

    @Test
    public void recipeSpecificCodes_getTheirOwnText() {
        assertEquals(R.string.ai_gen_error_empty_pantry,
                AiRecipeErrorText.specificMessage(error(AiError.VALIDATION_ERROR)));
        assertEquals(R.string.ai_gen_error_unsafe,
                AiRecipeErrorText.specificMessage(error(AiRecipeErrorText.AI_UNSAFE_RECIPE)));
        assertEquals(R.string.ai_gen_error_failed,
                AiRecipeErrorText.specificMessage(error(AiRecipeErrorText.AI_BAD_FORMAT)));
    }

    @Test
    public void networkAndBusy_useTheAssistantTexts() {
        assertEquals(0, AiRecipeErrorText.specificMessage(error(AiError.NETWORK_ERROR)));
        assertEquals(0, AiRecipeErrorText.specificMessage(error(AiError.TIMEOUT)));
        assertEquals(0, AiRecipeErrorText.specificMessage(error(AiError.AI_RATE_LIMITED)));
    }
}