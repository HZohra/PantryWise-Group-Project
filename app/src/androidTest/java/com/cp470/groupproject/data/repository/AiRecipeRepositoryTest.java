package com.cp470.groupproject.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.ApiClient;
import com.cp470.groupproject.data.remote.ApiResult;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Needs: npm run dev + adb reverse tcp:3000 tcp:3000, and sample food in the pantry
 * (DevTools → "Fill database with sample food"). The first test makes one real Claude call.
 */
@RunWith(AndroidJUnit4.class)
public class AiRecipeRepositoryTest {

    private final Application app = ApplicationProvider.getApplicationContext();

    private ApiResult<AiRecipeResponse> generateAndWait(AiRepository repo) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ApiResult<AiRecipeResponse>> result = new AtomicReference<>();
        repo.generateRecipe(null, 2, r -> {
            result.set(r);
            done.countDown();
        });
        assertTrue("callback never fired", done.await(120, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void generate_withSamplePantry_returnsCheckedRecipe() throws InterruptedException {
        ApiResult<AiRecipeResponse> result = generateAndWait(new AiRepository(app));

        assertTrue("expected success but got " + result.getError(), result.isSuccess());
        AiRecipeResponse data = result.getData();
        assertTrue(data.recipe.recipeId.startsWith("ai:"));
        assertFalse(data.recipe.title.trim().isEmpty());
        assertTrue(data.recipe.ingredients.size() >= 2);
        assertTrue(data.recipe.steps.size() >= 2);
    }

    @Test
    public void generate_serverUnreachable_returnsNetworkError() throws InterruptedException {
        AiRepository offline = new AiRepository(
                new PantryRepository(app),
                ApiClient.create("http://localhost:1/api/v1/"),
                Executors.newSingleThreadExecutor());

        ApiResult<AiRecipeResponse> result = generateAndWait(offline);

        assertFalse(result.isSuccess());
        assertEquals(AiError.NETWORK_ERROR, result.getError().code);
    }
}