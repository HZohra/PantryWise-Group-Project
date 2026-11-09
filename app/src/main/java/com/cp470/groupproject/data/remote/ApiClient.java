package com.cp470.groupproject.data.remote;

import com.cp470.groupproject.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the single Retrofit/OkHttp instance used by the whole app. */
public final class ApiClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 10;
    /**
     * Longer than the backend's worst case (20 s Claude timeout + 1 retry), so the app
     * receives the backend's clean 504 AI_TIMEOUT instead of a vague client-side timeout.
     */
    private static final long READ_TIMEOUT_SECONDS = 45;
    private static final long WRITE_TIMEOUT_SECONDS = 15;

    private ApiClient() { } // no instances: static access only

    /** "Holder" idiom: created on first use, and the JVM guarantees it's created only once (thread-safe). */
    private static final class Holder {
        static final AiApiService SERVICE = create(BuildConfig.AI_BASE_URL);
    }

    /** The shared API service. */
    public static AiApiService getService() {
        return Holder.SERVICE;
    }

    /** Package-private so tests can point it at a different server if needed. */
    public static AiApiService create(String baseUrl) {
        OkHttpClient.Builder http = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Log requests and responses to Logcat in DEBUG builds only. Never in release:
        // bodies contain the user's messages and pantry.
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            http.addInterceptor(logging);
        }

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(http.build())
                .addConverterFactory(GsonConverterFactory.create()) // JSON ↔ Java objects
                .build()
                .create(AiApiService.class);
    }
}