package com.example.prog7314_part2.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object EmailVerification {

    const val VERIFY_URL = "https://sportsphere-st10276384.onrender.com/verify-email"

    fun send(user: FirebaseUser, onComplete: (ok: Boolean, message: String) -> Unit) {
        val settings = ActionCodeSettings.newBuilder()
            .setUrl(VERIFY_URL)
            .setHandleCodeInApp(false)
            .build()

        user.sendEmailVerification(settings)
            .addOnCompleteListener { withContinueUrl ->
                if (withContinueUrl.isSuccessful) {
                    onComplete(true, sentMessage())
                    return@addOnCompleteListener
                }

                user.sendEmailVerification()
                    .addOnCompleteListener { fallback ->
                        if (fallback.isSuccessful) {
                            onComplete(true, sentMessage())
                            return@addOnCompleteListener
                        }
                        val detail = fallback.exception?.localizedMessage
                            ?: withContinueUrl.exception?.localizedMessage
                            ?: ""
                        onComplete(
                            false,
                            if (detail.isBlank()) {
                                "Could not send the email. Wait a minute and tap Resend."
                            } else {
                                "Could not send the email: $detail"
                            }
                        )
                    }
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

    private fun sentMessage(): String =
        "Verification email sent. In Gmail, long-press the button, copy the link, and open it in Chrome — not the in-app browser."
}
