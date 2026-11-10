package com.cp470.groupproject.data.remote;

import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.ChatRequest;
import com.cp470.groupproject.data.remote.dto.ChatResponse;
import com.cp470.groupproject.data.remote.dto.HealthResponse;
import com.cp470.groupproject.data.remote.dto.RecommendRequest;
import com.cp470.groupproject.data.remote.dto.RecommendResponse;
import com.cp470.groupproject.data.remote.dto.SearchResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

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

    /**
     * GET /api/v1/recipes/search. Every filter is optional: Retrofit leaves out null @Query values.
     * @param health comma-separated Edamam labels, e.g. "vegetarian,peanut-free"
     * @param limit  1–20 (backend default 10)
     */
    @GET("recipes/search")
    Call<SearchResponse> searchRecipes(@Query("q") String query,
                                       @Nullable @Query("diet") String diet,
                                       @Nullable @Query("health") String health,
                                       @Nullable @Query("cuisine") String cuisine,
                                       @Nullable @Query("mealType") String mealType,
                                       @Nullable @Query("maxTime") Integer maxTime,
                                       @Nullable @Query("limit") Integer limit);

    /** POST /api/v1/recipes/recommend: pantry-ranked recipes, expiring items first. */
    @POST("recipes/recommend")
    Call<RecommendResponse> recommendRecipes(@Body RecommendRequest request);

    // substitute is added in step 5.7
}