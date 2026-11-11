package com.cp470.groupproject.scanner.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import com.google.gson.JsonObject;

public interface UsdaApiService {

    @GET("foods/search")
    Call<JsonObject> searchFood(
            @Query("query") String barcode,
            @Query("dataType") String dataType,
            @Query("api_key") String apiKey
    );

}
