package com.example.prog7314_part2.data.remote

import com.example.prog7314_part2.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * NOTE: this client does not attach a Firebase ID token yet, because `main`
 * does not have Firebase Auth wired up (see the `Mo` branch, which adds
 * FirebaseAuthInterceptor for the login/performance/settings screens).
 * The Learn endpoints are GET-only and read fine against a deployed API
 * running with SKIP_AUTH=true. Once real auth lands on `main`, add
 * `.addInterceptor(FirebaseAuthInterceptor())` below to match it.
 */
object ApiClient {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val service: SportSphereApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SportSphereApi::class.java)
    }
}
