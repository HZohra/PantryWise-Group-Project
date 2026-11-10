package com.cp470.groupproject.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.ApiResult;
import com.cp470.groupproject.data.remote.dto.SubstituteResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Needs: npm run dev + adb reverse tcp:3000 tcp:3000. Uses the local swap list (no Claude call). */
@RunWith(AndroidJUnit4.class)
public class RecipeSubstituteTest {

    private final Application app = ApplicationProvider.getApplicationContext();

    private ApiResult<SubstituteResponse> substituteAndWait(String ingredient) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ApiResult<SubstituteResponse>> result = new AtomicReference<>();
        new RecipeRepository(app).substitute("Banana bread", ingredient,
                Arrays.asList("1/2 cup butter", "2 bananas", "1 cup flour"), null, r -> {
                    result.set(r);
                    done.countDown();
                });
        assertTrue("callback never fired", done.await(60, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void commonIngredient_isAnsweredFromLocalList() throws InterruptedException {
        ApiResult<SubstituteResponse> result = substituteAndWait("butter");

        assertTrue("expected success but got " + result.getError(), result.isSuccess());
        assertEquals(SubstituteResponse.SOURCE_LOCAL, result.getData().source);
        assertFalse(result.getData().substitutes.isEmpty());
    }

    @Test
    public void blankIngredient_returnsValidationError() throws InterruptedException {
        ApiResult<SubstituteResponse> result = substituteAndWait("   ");

        assertFalse(result.isSuccess());
        assertEquals(AiError.VALIDATION_ERROR, result.getError().code);
    }
}