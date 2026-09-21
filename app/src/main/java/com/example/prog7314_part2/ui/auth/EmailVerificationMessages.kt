package com.example.prog7314_part2.ui.auth

/**
 * Pure-Kotlin helpers for turning raw Firebase / API error strings into
 * something a user can actually read.
 *
 * Split out of [EmailVerification] so that the mapping logic can be
 * unit-tested (`EmailVerificationMessagesTest`) without an Android
 * runtime or a live Firebase project.
 */
object EmailVerificationMessages {

    /**
     * Maps a raw error detail (Firebase `localizedMessage`, server
     * `error` field, or the empty string) to a short user-facing tip.
     */
    fun describeSendError(detail: String): String {
        val text = detail.lowercase()
        return when {
            isRateLimited(text) ->
                "Firebase paused emails from this device. Wait a few minutes, then tap Resend once."

            // Firebase surfaces "unauthorized_domain" when Action URL
            // domains are missing from the Firebase console. We hide
            // that jargon behind a friendlier "wait and retry" prompt.
            "unauthorized_domain" in text || "allowlisted" in text || "allow listed" in text ->
                "Could not send the email. Wait a minute and tap Resend once."

            detail.isBlank() ->
                "Could not send the email. Wait a minute and tap Resend once."

            else -> "Could not send the email: $detail"
        }
    }

    /**
     * True when [detail] looks like Firebase's rate-limiter text, no
     * matter which variant Firebase happens to return that day.
     */
    fun isRateLimited(detail: String): Boolean {
        val text = detail.lowercase()
        return "blocked all requests" in text ||
            "too_many_attempts" in text ||
            "too many attempts" in text
    }
}
