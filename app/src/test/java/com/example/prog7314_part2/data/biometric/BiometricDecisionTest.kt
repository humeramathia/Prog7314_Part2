package com.example.prog7314_part2.data.biometric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricDecisionTest {

    @Test
    fun gatesOnlyWhenSignedInEnabledLockedAndReady() {
        assertTrue(
            BiometricDecision.shouldGate(
                loggedIn = true,
                enabledForUser = true,
                unlockedThisProcess = false,
                canAuthenticateStatus = BiometricDecision.STATUS_SUCCESS
            )
        )
    }

    @Test
    fun doesNotGateWhenAlreadyUnlockedThisProcess() {
        assertFalse(
            BiometricDecision.shouldGate(
                loggedIn = true,
                enabledForUser = true,
                unlockedThisProcess = true,
                canAuthenticateStatus = BiometricDecision.STATUS_SUCCESS
            )
        )
    }

    @Test
    fun doesNotLockOutAPhoneWithNothingEnrolled() {
        assertFalse(
            BiometricDecision.shouldGate(
                loggedIn = true,
                enabledForUser = true,
                unlockedThisProcess = false,
                canAuthenticateStatus = BiometricDecision.STATUS_NONE_ENROLLED
            )
        )
    }

    @Test
    fun doesNotGateWhenTheSettingIsOff() {
        assertFalse(
            BiometricDecision.shouldGate(
                loggedIn = true,
                enabledForUser = false,
                unlockedThisProcess = false,
                canAuthenticateStatus = BiometricDecision.STATUS_SUCCESS
            )
        )
    }

    @Test
    fun offersEnrollmentOnlyWhenReadyAndNotAlreadyOn() {
        assertTrue(
            BiometricDecision.shouldOfferEnrollment(
                alreadyEnabled = false,
                canAuthenticateStatus = BiometricDecision.STATUS_SUCCESS
            )
        )
        assertFalse(
            BiometricDecision.shouldOfferEnrollment(
                alreadyEnabled = true,
                canAuthenticateStatus = BiometricDecision.STATUS_SUCCESS
            )
        )
        assertFalse(
            BiometricDecision.shouldOfferEnrollment(
                alreadyEnabled = false,
                canAuthenticateStatus = BiometricDecision.STATUS_NO_HARDWARE
            )
        )
    }

    @Test
    fun mapsHardwareAndEnrollmentStatuses() {
        assertEquals(
            BiometricBlockReason.READY,
            BiometricDecision.reasonForStatus(BiometricDecision.STATUS_SUCCESS)
        )
        assertEquals(
            BiometricBlockReason.NO_HARDWARE,
            BiometricDecision.reasonForStatus(BiometricDecision.STATUS_NO_HARDWARE)
        )
        assertEquals(
            BiometricBlockReason.NONE_ENROLLED,
            BiometricDecision.reasonForStatus(BiometricDecision.STATUS_NONE_ENROLLED)
        )
        assertEquals(
            BiometricBlockReason.HW_UNAVAILABLE,
            BiometricDecision.reasonForStatus(BiometricDecision.STATUS_HW_UNAVAILABLE)
        )
    }

    @Test
    fun treatsPromptCancelAndLockoutSeparately() {
        assertTrue(BiometricDecision.isUserCancel(BiometricDecision.ERROR_USER_CANCELED))
        assertTrue(BiometricDecision.isLockout(BiometricDecision.ERROR_LOCKOUT))
        assertEquals(
            BiometricBlockReason.LOCKOUT,
            BiometricDecision.reasonForError(BiometricDecision.ERROR_LOCKOUT_PERMANENT)
        )
        assertEquals(
            BiometricBlockReason.CANCELLED,
            BiometricDecision.reasonForError(BiometricDecision.ERROR_NEGATIVE_BUTTON)
        )
    }
}
