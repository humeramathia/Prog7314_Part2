package com.example.prog7314_part2.ui.auth

object EmailVerificationMessages {

    fun describeSendError(detail: String): String {
        val text = detail.lowercase()
        return when {
            isRateLimited(text) ->
                "Firebase paused emails from this device. Wait a few minutes, then tap Resend once."

            "unauthorized_domain" in text || "allowlisted" in text || "allow listed" in text ->
                "Could not send the email. Wait a minute and tap Resend once."

            detail.isBlank() ->
                "Could not send the email. Wait a minute and tap Resend once."

            else -> "Could not send the email: $detail"
        }
    }

    fun isRateLimited(detail: String): Boolean {
        val text = detail.lowercase()
        return "blocked all requests" in text ||
            "too_many_attempts" in text ||
            "too many attempts" in text
    }
}
