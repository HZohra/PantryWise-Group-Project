package com.cp470.groupproject.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.ApiClient;
import com.cp470.groupproject.data.remote.ApiResult;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.RecommendResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Needs: npm run dev + adb reverse tcp:3000 tcp:3000. Uses real Edamam calls. */
@RunWith(AndroidJUnit4.class)
public class RecipeRepositoryTest {

    private final Application app = ApplicationProvider.getApplicationContext();

    private ApiResult<RecommendResponse> recommendAndWait(RecipeRepository repo, String query)
            throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ApiResult<RecommendResponse>> result = new AtomicReference<>();
        repo.recommend(query, null, false, 5, r -> {
            result.set(r);
            done.countDown();
        });
        assertTrue("callback never fired", done.await(60, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void recommend_fromPantry_returnsRankedRecipesWithMatch() throws InterruptedException {
        ApiResult<RecommendResponse> result = recommendAndWait(new RecipeRepository(app), null);

        assertTrue("expected success but got " + result.getError(), result.isSuccess());
        RecommendResponse data = result.getData();
        assertNotNull(data);
        assertEquals(data.count, data.recipes.size());
        for (RecipeDto recipe : data.recipes) {
            assertTrue(recipe.recipeId, recipe.recipeId.startsWith("edamam:"));
            assertNotNull("every recommended recipe has a pantry match", recipe.match);
        }
    }

    @Test
    public void recommend_withQuery_searchesThatKeyword() throws InterruptedException {
        ApiResult<RecommendResponse> result = recommendAndWait(new RecipeRepository(app), "  pasta  ");

        assertTrue("expected success but got " + result.getError(), result.isSuccess());
        assertEquals("pasta", result.getData().searchedFor.get(0));
        assertFalse(result.getData().recipes.isEmpty());
    }

    @Test
    public void recommend_serverUnreachable_returnsNetworkError() throws InterruptedException {
        RecipeRepository offline = new RecipeRepository(
                new PantryRepository(app),
                ApiClient.create("http://localhost:1/api/v1/"),
                Executors.newSingleThreadExecutor());

        ApiResult<RecommendResponse> result = recommendAndWait(offline, null);

        assertFalse(result.isSuccess());
        assertEquals(AiError.NETWORK_ERROR, result.getError().code);
    }
}