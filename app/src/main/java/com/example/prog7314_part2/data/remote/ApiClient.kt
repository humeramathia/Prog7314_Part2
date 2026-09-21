package com.example.prog7314_part2.data.remote

import com.example.prog7314_part2.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Lazily-initialised singleton that owns the app's Retrofit + OkHttp
 * stack.
 *
 * The base URL comes from a `buildConfigField` declared in
 * `app/build.gradle.kts`, so debug builds talk to
 * `http://10.0.2.2:3000/` (the emulator's alias for the developer PC)
 * and release builds talk to the hosted API on Render.
 */
object ApiClient {

    /**
     * HTTP stack shared by every Retrofit call.
     *
     * The 15 s timeouts are chosen to cover Render's cold-start latency
     * without blocking the UI for a full minute when the network is
     * genuinely down.
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Attaches the Firebase ID token to every request; see
            // [FirebaseAuthInterceptor].
            .addInterceptor(FirebaseAuthInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * The one and only Retrofit implementation of [SportSphereApi].
     *
     * `by lazy` guarantees the underlying `OkHttpClient` and Retrofit
     * instance are created exactly once per process, on first access.
     */
    val service: SportSphereApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SportSphereApi::class.java)
    }
}
