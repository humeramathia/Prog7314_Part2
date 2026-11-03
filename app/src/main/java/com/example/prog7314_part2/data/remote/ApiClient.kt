package com.example.prog7314_part2.data.remote

import com.example.prog7314_part2.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Lazily-initialised singleton that owns the app's Retrofit + OkHttp
 * stack. Both variants use the hosted API by default; debug builds
 * can override the URL with the sportsphere.apiBaseUrl Gradle property.
 */
object ApiClient {

    /**
     * HTTP stack shared by every Retrofit call.
     *
     * The longer read timeout lets a sleeping hosted API wake up.
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Attaches the Firebase ID token to every request; see
            // [FirebaseAuthInterceptor].
            .addInterceptor(FirebaseAuthInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
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
