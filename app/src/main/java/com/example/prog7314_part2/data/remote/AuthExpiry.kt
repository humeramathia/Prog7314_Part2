package com.example.prog7314_part2.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Signs the user out when the API returns 401, as the POE tips require.
 * [FirebaseAuthInterceptor] calls [notifyUnauthorized] off the main thread;
 * the actual sign-out runs on the main looper so session prefs stay consistent.
 */
object AuthExpiry {

    private const val TAG = "Auth"
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var onExpired: (() -> Unit)? = null

    fun install(onExpired: () -> Unit) {
        this.onExpired = onExpired
    }

    fun notifyUnauthorized() {
        main.post {
            Log.i(TAG, "API returned 401; signing out")
            onExpired?.invoke()
        }
    }
}
