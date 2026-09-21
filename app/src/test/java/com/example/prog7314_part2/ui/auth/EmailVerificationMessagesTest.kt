package com.example.prog7314_part2.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailVerificationMessagesTest {

    @Test
    fun mapsDeviceBlockToWaitMessage() {
        val message = EmailVerificationMessages.describeSendError(
            "We have blocked all requests from this device due to unusual activity. Try again later."
        )
        assertTrue(message.contains("Wait a few minutes"))
        assertFalse(message.contains("We have blocked all requests"))
    }

    @Test
    fun mapsTooManyAttemptsCode() {
        val message = EmailVerificationMessages.describeSendError("TOO_MANY_ATTEMPTS_TRY_LATER")
        assertTrue(message.contains("Wait a few minutes"))
    }

    @Test
    fun keepsUnknownErrorsReadable() {
        assertEquals(
            "Could not send the email: Network error",
            EmailVerificationMessages.describeSendError("Network error")
        )
    }
}
