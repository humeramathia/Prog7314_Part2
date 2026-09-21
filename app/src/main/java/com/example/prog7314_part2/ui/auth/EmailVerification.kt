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

object EmailVerification {

    const val VERIFY_URL = "https://sportsphere-st10276384.onrender.com/verify-email"
    const val CONTINUE_URL = VERIFY_URL

    fun send(user: FirebaseUser, onComplete: (ok: Boolean, message: String) -> Unit) {
        val settings = ActionCodeSettings.newBuilder()
            .setUrl(CONTINUE_URL)
            .setHandleCodeInApp(false)
            .setAndroidPackageName("com.example.prog7314_part2", false, null)
            .build()

        // One Firebase send only. A second attempt on failure is what triggered
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

    fun applyCodeFrom(uri: Uri?, onComplete: (ok: Boolean, message: String) -> Unit) {
        if (uri == null) return
        val mode = uri.getQueryParameter("mode")
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

    fun describeSendError(error: Throwable?): String =
        EmailVerificationMessages.describeSendError(error?.localizedMessage.orEmpty())

    fun describeSendError(detail: String): String =
        EmailVerificationMessages.describeSendError(detail)

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
