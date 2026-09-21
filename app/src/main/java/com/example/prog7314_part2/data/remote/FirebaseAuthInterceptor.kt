package com.example.prog7314_part2.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp interceptor that stamps every outgoing request with a
 * `Authorization: Bearer <firebase-id-token>` header.
 *
 * The server (see `api/src/middleware/auth.js`) verifies that token with
 * the Firebase Admin SDK, so without this interceptor every protected
 * route would return `401 Missing Bearer token`.
 *
 * Requests made before the user has signed in (e.g. `GET /api/sports`
 * during the sport picker on a local `SKIP_AUTH=true` build) are allowed
 * to proceed unauthenticated — the API decides whether to accept them.
 */
class FirebaseAuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser

        // With SKIP_AUTH=true the local API permits anonymous requests,
        // so let them through untouched.
        if (user == null) {
            return chain.proceed(chain.request())
        }

        // `getIdToken(false)` returns the cached token unless it's within
        // ~5 minutes of expiry, in which case Firebase transparently
        // refreshes it. `Tasks.await` blocks the call thread — that's
        // fine because OkHttp already dispatches interceptors on a
        // background thread pool.
        val token = try {
            Tasks.await(
                user.getIdToken(false),
                15,
                TimeUnit.SECONDS
            ).token
        } catch (error: Exception) {
            // Convert the checked task exception into an IOException so
            // Retrofit surfaces it via `Callback.onFailure` instead of
            // crashing the interceptor chain.
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
