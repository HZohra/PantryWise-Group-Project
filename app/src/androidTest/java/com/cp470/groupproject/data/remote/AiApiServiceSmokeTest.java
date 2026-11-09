package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.remote.dto.ApiErrorResponse;
import com.cp470.groupproject.data.remote.dto.ChatRequest;
import com.cp470.groupproject.data.remote.dto.ChatResponse;
import com.cp470.groupproject.data.remote.dto.HealthResponse;
import com.cp470.groupproject.data.remote.dto.InventoryItemDto;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

import retrofit2.Response;

/**
 * Runs ON THE EMULATOR against the real local backend (npm run dev must be running).
 * execute() is synchronous; that's fine here because tests don't run on the UI thread.
 */
@RunWith(AndroidJUnit4.class)
public class AiApiServiceSmokeTest {

    private final AiApiService api = ApiClient.getService();

    @Test
    public void health_returnsOk() throws IOException {
        Response<HealthResponse> response = api.health().execute();

        assertTrue("HTTP " + response.code(), response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("ok", response.body().status);
    }

    @Test
    public void chat_withInventory_returnsReply() throws IOException {
        ChatRequest request = new ChatRequest(
                "What can I cook with my milk? Answer in one sentence.",
                Collections.emptyList(),
                Collections.singletonList(new InventoryItemDto("milk", 1.0, "L", 2)));

        Response<ChatResponse> response = api.chat(request).execute();

        assertTrue("HTTP " + response.code(), response.isSuccessful());
        assertNotNull(response.body());
        assertFalse(response.body().reply.trim().isEmpty());
    }

    @Test
    public void chat_emptyMessage_returnsValidationError() throws IOException {
        ChatRequest request = new ChatRequest("", Collections.emptyList(), Collections.emptyList());

        Response<ChatResponse> response = api.chat(request).execute();

        assertEquals(400, response.code());
        assertNotNull(response.errorBody());
        // Error bodies are NOT converted automatically: parse them with Gson ourselves
        ApiErrorResponse error = new Gson().fromJson(response.errorBody().charStream(), ApiErrorResponse.class);
        assertEquals("VALIDATION_ERROR", error.error.code);
    }
}