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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Emulator tests. Needs: npm run dev + adb reverse tcp:3000 tcp:3000. */
@RunWith(AndroidJUnit4.class)
public class AiRepositoryTest {

    private final Application app = ApplicationProvider.getApplicationContext();

    private ApiResult<String> sendAndWait(AiRepository repo, String message) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ApiResult<String>> result = new AtomicReference<>();
        repo.sendChatMessage(message, Collections.emptyList(), r -> {
            result.set(r);
            done.countDown();
        });
        assertTrue("callback never fired", done.await(60, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void chat_withRealPantry_returnsReply() throws InterruptedException {
        ApiResult<String> result = sendAndWait(new AiRepository(app), "Suggest one quick snack. One sentence.");
        assertTrue("expected success but got " + result.getError(), result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData().trim().isEmpty());
    }

    @Test
    public void chat_emptyMessage_returnsValidationError() throws InterruptedException {
        ApiResult<String> result = sendAndWait(new AiRepository(app), "");
        assertFalse(result.isSuccess());
        assertEquals(AiError.VALIDATION_ERROR, result.getError().code);
        assertEquals(400, result.getError().httpStatus);
    }

    @Test
    public void chat_serverUnreachable_returnsNetworkError() throws InterruptedException {
        AiRepository offline = new AiRepository(
                new PantryRepository(app),
                ApiClient.create("http://localhost:1/api/v1/"),
                Executors.newSingleThreadExecutor());
        ApiResult<String> result = sendAndWait(offline, "Hello");
        assertFalse(result.isSuccess());
        assertEquals(AiError.NETWORK_ERROR, result.getError().code);
        assertEquals(0, result.getError().httpStatus);
    }
}