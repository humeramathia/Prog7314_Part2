package com.example.prog7314_part2.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.SendVerificationResponse
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Centralised helpers for Firebase's email-verification flow.
 *
 * All of the noisy logic that used to be duplicated across Register and
 * Confirm-Email screens lives here so callers only see three simple
 * entry points:
 *  - [send]              — kicks off a Firebase verification email
 *                          (with an API-side fallback if the device is
 *                          rate-limited).
 *  - [refreshVerified]   — polls whether the current user has verified
 *                          without spamming Firebase.
 *  - [applyCodeFrom]     — applies the `oobCode` when the user opens
 *                          the app via the verification deep link.
 */
object EmailVerification {

    /** Landing page for the verification link (also handles the oobCode server-side). */
    const val VERIFY_URL = "https://sportsphere-st10276384.onrender.com/verify-email"

    /** Firebase requires an `ActionCodeSettings.url` — reuse the same page. */
    const val CONTINUE_URL = VERIFY_URL

    /**
     * Sends the "Please verify your email" mail via Firebase Auth.
     *
     * If Firebase blocks the request with a device-level rate limit
     * ("blocked all requests from this device"), we transparently retry
     * once through our own API using an ID token, which routes the send
     * through the server's IP instead of the device's.
     *
     * @param user       Currently signed-in Firebase user.
     * @param onComplete Called on the main thread with (`ok`, message).
     */
    fun send(user: FirebaseUser, onComplete: (ok: Boolean, message: String) -> Unit) {
        val settings = ActionCodeSettings.newBuilder()
            .setUrl(CONTINUE_URL)
            .setHandleCodeInApp(false)
            .setAndroidPackageName("com.example.prog7314_part2", false, null)
            .build()

        // Only one Firebase attempt — a second retry inside the same
        // call sequence was what previously triggered
        // "We have blocked all requests from this device".
        user.sendEmailVerification(settings)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    onComplete(true, sentMessage())
                    return@addOnCompleteListener
                }

                val error = result.exception
                if (isRateLimited(error)) {
                    sendViaApi(onComplete)
                    return@addOnCompleteListener
                }

                onComplete(false, describeSendError(error))
            }
    }

    /**
     * Applies a verification link's `oobCode` to the current Firebase
     * user. Called from [com.example.prog7314_part2.MainActivity] when
     * the app is opened via `https://<host>/verify-email?...`.
     *
     * Returns silently when [uri] does not contain a `verifyEmail` code
     * so the deep-link handler in `MainActivity` can call this
     * unconditionally without checking the query first.
     */
    fun applyCodeFrom(uri: Uri?, onComplete: (ok: Boolean, message: String) -> Unit) {
        if (uri == null) return
        val mode = uri.getQueryParameter("mode")
        // Accept both the canonical `oobCode` and the accidentally
        // lower-cased variant some clients use.
        val oobCode = uri.getQueryParameter("oobCode") ?: uri.getQueryParameter("oobcode")
        if (oobCode.isNullOrBlank()) return
        if (!mode.isNullOrBlank() && mode != "verifyEmail") return

        FirebaseAuth.getInstance().applyActionCode(oobCode)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    FirebaseAuth.getInstance().currentUser?.reload()
                    onComplete(true, "Email verified. You can continue in the app.")
                } else {
                    onComplete(
                        false,
                        result.exception?.localizedMessage
                            ?: "Could not apply the verification link."
                    )
                }
            }
    }

    /**
     * Refreshes the Firebase user and reports whether their email is
     * now verified.
     *
     * A forced ID-token refresh (`getIdToken(true)`) is required to
     * pick up server-side changes made from the verification link on
     * another device or in a browser.
     *
     * @param onComplete `(verified, error)` where `error == "signed-out"`
     *   means no user was available at all.
     */
    fun refreshVerified(onComplete: (verified: Boolean, error: String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onComplete(false, "signed-out")
            return
        }

        user.getIdToken(true)
            .continueWithTask { tokenTask ->
                if (!tokenTask.isSuccessful) {
                    throw tokenTask.exception
                        ?: IllegalStateException("Could not refresh your session.")
                }
                val refreshed = FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("signed-out")
                refreshed.reload()
            }
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(false, task.exception?.localizedMessage)
                    return@addOnCompleteListener
                }
                val verified = FirebaseAuth.getInstance().currentUser?.isEmailVerified == true
                onComplete(verified, null)
            }
    }

    /**
     * Best-effort launch of the device's default mail client so the
     * user can pop over to Gmail without leaving the app manually.
     *
     * @return `true` if an email app was opened, `false` if no app
     *   claims [Intent.CATEGORY_APP_EMAIL].
     */
    fun openInbox(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** User-facing translation of a Firebase send error. */
    fun describeSendError(error: Throwable?): String =
        EmailVerificationMessages.describeSendError(error?.localizedMessage.orEmpty())

    /** Same as [describeSendError] but works from a raw string (used by the API branch). */
    fun describeSendError(detail: String): String =
        EmailVerificationMessages.describeSendError(detail)

    /**
     * Server-side fallback: routes the verification-email send through
     * our own API, which uses the Firebase Admin SDK and therefore hits
     * a different rate-limit bucket than the client SDK.
     */
    private fun sendViaApi(onComplete: (ok: Boolean, message: String) -> Unit) {
        ApiClient.service.sendVerificationEmail().enqueue(
            object : Callback<SendVerificationResponse> {
                override fun onResponse(
                    call: Call<SendVerificationResponse>,
                    response: Response<SendVerificationResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.ok == true) {
                        onComplete(true, sentMessage())
                        return
                    }
                    val raw = body?.error
                        ?: response.errorBody()?.string().orEmpty()
                    onComplete(
                        false,
                        describeSendError(raw.ifBlank { "TOO_MANY_ATTEMPTS_TRY_LATER" })
                    )
                }

                override fun onFailure(
                    call: Call<SendVerificationResponse>,
                    error: Throwable
                ) {
                    if (call.isCanceled) return
                    onComplete(
                        false,
                        describeSendError("TOO_MANY_ATTEMPTS_TRY_LATER")
                    )
                }
            }
        )
    }

    private fun isRateLimited(error: Throwable?): Boolean =
        error is FirebaseTooManyRequestsException ||
            EmailVerificationMessages.isRateLimited(error?.localizedMessage.orEmpty())

    private fun sentMessage(): String =
        "Verification email sent. In Gmail, long-press the button, copy the link, and open it in Chrome — not the in-app browser."
}
