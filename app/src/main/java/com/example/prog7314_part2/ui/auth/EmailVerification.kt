package com.example.prog7314_part2.ui.auth

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object EmailVerification {

    fun send(user: FirebaseUser, onComplete: (ok: Boolean, message: String) -> Unit) {
        user.sendEmailVerification()
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    onComplete(
                        true,
                        "Verification email sent. Check inbox and spam, then open the Verify link."
                    )
                    return@addOnCompleteListener
                }
                val detail = result.exception?.localizedMessage.orEmpty()
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
}
