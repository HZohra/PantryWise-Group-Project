package com.cp470.groupproject.data.remote;

import com.cp470.groupproject.data.remote.dto.ChatRequest;
import com.cp470.groupproject.data.remote.dto.ChatResponse;
import com.cp470.groupproject.data.remote.dto.HealthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * The PantryWise backend API, as Retrofit sees it.
 * Paths are RELATIVE (no leading "/") so they are appended to BuildConfig.AI_BASE_URL (".../api/v1/").
 */
public interface AiApiService {

    /** GET /api/v1/health */
    @GET("health")
    Call<HealthResponse> health();

    /** POST /api/v1/ai/chat */
    @POST("ai/chat")
    Call<ChatResponse> chat(@Body ChatRequest request);

    // search, recommend and substitute are added in step 5.6
}