package com.example.prog7314_part2.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.SendVerificationResponse
import com.example.prog7314_part2.data.remote.VerificationLinkResponse
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

    /**
     * Sends the "Please verify your email" mail via Firebase Auth.
     *
     * Default Firebase mail (no custom continue URL) is more likely to
     * land in Hotmail / Outlook. If the client SDK fails for any reason,
     * including a device-level rate limit, we retry once through the API
     * so the send uses the server's IP.
     *
     * @param user       Currently signed-in Firebase user.
     * @param preferApi  True on Resend: try the API first, then the client.
     * @param onComplete Called on the main thread with (`ok`, message).
     */
    fun send(
        user: FirebaseUser,
        preferApi: Boolean = false,
        onComplete: (ok: Boolean, message: String) -> Unit
    ) {
        if (preferApi) {
            sendViaApi { ok, message ->
                if (ok) onComplete(true, message) else sendFromClient(user, onComplete)
            }
        } else {
            sendFromClient(user) { ok, message ->
                if (ok) onComplete(true, message) else sendViaApi(onComplete)
            }
        }
    }

    private fun sendFromClient(
        user: FirebaseUser,
        onComplete: (ok: Boolean, message: String) -> Unit
    ) {
        user.sendEmailVerification()
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    onComplete(true, sentMessage())
                    return@addOnCompleteListener
                }
                onComplete(false, describeSendError(result.exception))
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
    fun openInbox(context: Context, email: String? = null): Boolean {
        val host = email?.substringAfter("@")?.lowercase().orEmpty()
        val webInbox = when (host) {
            "hotmail.com", "outlook.com", "live.com", "msn.com" ->
                Uri.parse("https://outlook.live.com/mail/")
            "gmail.com", "googlemail.com" ->
                Uri.parse("https://mail.google.com")
            else -> null
        }
        if (webInbox != null) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, webInbox).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (_: Exception) {
            }
        }
        val inbox = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(inbox)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Asks the API for a verify-email URL and opens it. Hotmail often never
     * shows Firebase's message; this confirms the account without the inbox.
     */
    fun openVerificationLink(context: Context, onComplete: (ok: Boolean, message: String) -> Unit) {
        ApiClient.service.verificationLink().enqueue(
            object : Callback<VerificationLinkResponse> {
                override fun onResponse(
                    call: Call<VerificationLinkResponse>,
                    response: Response<VerificationLinkResponse>
                ) {
                    val link = response.body()?.link
                    if (!response.isSuccessful || link.isNullOrBlank()) {
                        val raw = response.body()?.error
                            ?: response.errorBody()?.string().orEmpty()
                        onComplete(
                            false,
                            describeSendError(raw.ifBlank { "Could not build a verification link." })
                        )
                        return
                    }
                    val opened = try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                        true
                    } catch (_: Exception) {
                        false
                    }
                    if (opened) {
                        onComplete(true, "Chrome will confirm the address. Then return here and tap I have confirmed.")
                    } else {
                        onComplete(false, "Could not open the verification link. Install Chrome and try again.")
                    }
                }

                override fun onFailure(
                    call: Call<VerificationLinkResponse>,
                    error: Throwable
                ) {
                    if (call.isCanceled) return
                    onComplete(false, describeSendError(error.localizedMessage.orEmpty()))
                }
            }
        )
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

    private fun sentMessage(): String =
        "Verification email sent. Check inbox and Junk (Hotmail often hides it). If it does not arrive, tap Verify in Chrome."
}
