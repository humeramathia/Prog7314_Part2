package com.example.prog7314_part2.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class FirebaseAuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser

        // With SKIP_AUTH=true, the local API permits this request.
        if (user == null) {
            return chain.proceed(chain.request())
        }

        val token = try {
            Tasks.await(
                user.getIdToken(false),
                15,
                TimeUnit.SECONDS
            ).token
        } catch (error: Exception) {
            throw IOException(
                "Could not obtain the Firebase ID token.",
                error
            )
        }

        if (token.isNullOrBlank()) {
            throw IOException("Firebase returned an empty ID token.")
        }

        val authenticatedRequest = chain.request()
            .newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}