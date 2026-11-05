package com.example.prog7314_part2.data.biometric

/**
 * Process-scoped unlock flag.
 *
 * Closing the app from Recents kills the process, so the next cold
 * start asks for a fingerprint again. Rotation does not, which is why
 * this lives in a process singleton rather than a ViewModel.
 *
 * No password is stored here — Firebase already holds the signed-in
 * session. Biometrics only prove the person holding the phone is the
 * same one who signed in with SSO (PROG7314 POE tips, s. 3).
 */
object BiometricSession {
    @Volatile
    var unlocked: Boolean = false

    fun markUnlocked() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
    }
}
