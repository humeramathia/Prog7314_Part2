package com.example.prog7314_part2.data.biometric

/**
 * Pure rules for when SportSphere should gate the UI behind
 * [androidx.biometric.BiometricPrompt].
 *
 * Kept free of Android types so the same decisions can be unit-tested
 * without a device. Status integers match
 * [androidx.biometric.BiometricManager] and
 * [androidx.biometric.BiometricPrompt] (Android Developers, [s.a.]).
 */
object BiometricDecision {

    const val STATUS_SUCCESS = 0
    const val STATUS_HW_UNAVAILABLE = 1
    const val STATUS_NONE_ENROLLED = 11
    const val STATUS_NO_HARDWARE = 12
    const val STATUS_SECURITY_UPDATE_REQUIRED = 15
    const val STATUS_UNSUPPORTED = -2

    const val ERROR_LOCKOUT = 7
    const val ERROR_LOCKOUT_PERMANENT = 9
    const val ERROR_USER_CANCELED = 10
    const val ERROR_NEGATIVE_BUTTON = 13

    /**
     * Gate the clubhouse only when the user is signed in, they turned
     * the setting on for this account, this process has not unlocked
     * yet, and the phone can actually show a prompt.
     *
     * A new phone with no enrolled biometrics must never lock the user
     * out of an account they already signed into with SSO.
     */
    fun shouldGate(
        loggedIn: Boolean,
        enabledForUser: Boolean,
        unlockedThisProcess: Boolean,
        canAuthenticateStatus: Int
    ): Boolean {
        return loggedIn &&
            enabledForUser &&
            !unlockedThisProcess &&
            canAuthenticateStatus == STATUS_SUCCESS
    }

    /**
     * After the first SSO / email sign-in, ask only when this device
     * can enroll and the user has not already turned the setting on.
     */
    fun shouldOfferEnrollment(
        alreadyEnabled: Boolean,
        canAuthenticateStatus: Int
    ): Boolean {
        return !alreadyEnabled && canAuthenticateStatus == STATUS_SUCCESS
    }

    fun isReady(canAuthenticateStatus: Int): Boolean =
        canAuthenticateStatus == STATUS_SUCCESS

    fun isUserCancel(errorCode: Int): Boolean =
        errorCode == ERROR_USER_CANCELED || errorCode == ERROR_NEGATIVE_BUTTON

    fun isLockout(errorCode: Int): Boolean =
        errorCode == ERROR_LOCKOUT || errorCode == ERROR_LOCKOUT_PERMANENT

    fun reasonForStatus(canAuthenticateStatus: Int): BiometricBlockReason {
        return when (canAuthenticateStatus) {
            STATUS_SUCCESS -> BiometricBlockReason.READY
            STATUS_NO_HARDWARE, STATUS_UNSUPPORTED -> BiometricBlockReason.NO_HARDWARE
            STATUS_NONE_ENROLLED -> BiometricBlockReason.NONE_ENROLLED
            STATUS_HW_UNAVAILABLE, STATUS_SECURITY_UPDATE_REQUIRED ->
                BiometricBlockReason.HW_UNAVAILABLE
            else -> BiometricBlockReason.UNAVAILABLE
        }
    }

    fun reasonForError(errorCode: Int): BiometricBlockReason {
        return when {
            isLockout(errorCode) -> BiometricBlockReason.LOCKOUT
            isUserCancel(errorCode) -> BiometricBlockReason.CANCELLED
            errorCode == STATUS_NONE_ENROLLED -> BiometricBlockReason.NONE_ENROLLED
            errorCode == STATUS_NO_HARDWARE -> BiometricBlockReason.NO_HARDWARE
            errorCode == STATUS_HW_UNAVAILABLE -> BiometricBlockReason.HW_UNAVAILABLE
            else -> BiometricBlockReason.UNAVAILABLE
        }
    }
}

enum class BiometricBlockReason {
    READY,
    NO_HARDWARE,
    NONE_ENROLLED,
    HW_UNAVAILABLE,
    LOCKOUT,
    CANCELLED,
    UNAVAILABLE
}
