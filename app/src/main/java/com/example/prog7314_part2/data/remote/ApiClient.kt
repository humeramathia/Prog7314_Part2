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
 * `app/build.gradle.kts`. Debug and release both talk to the hosted
 * Render API so a physical phone works; pass `-PapiBaseUrl=` to point
 * an emulator at a local Node process.
 */
object ApiClient {

    /**
     * HTTP stack shared by every Retrofit call.
     *
     * The 60 s timeouts cover a Render free-tier cold start, which often
     * takes longer than 15 s after the service has been idle.
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Attaches the Firebase ID token to every request; see
            // [FirebaseAuthInterceptor].
            .addInterceptor(FirebaseAuthInterceptor())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
