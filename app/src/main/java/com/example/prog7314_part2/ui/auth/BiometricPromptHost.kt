package com.example.prog7314_part2.ui.auth

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.prog7314_part2.data.biometric.BiometricDecision

/**
 * Thin wrapper around [BiometricPrompt].
 *
 * The host **must** be an [FragmentActivity] / [androidx.appcompat.app.AppCompatActivity].
 * A plain ComponentActivity is not enough (PROG7314 POE tips, s. 3;
 * Android Developers, [s.a.]).
 *
 * Authenticators are `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`, so a
 * cancelled fingerprint can fall through to the device PIN without us
 * ever storing a SportSphere password on the phone.
 */
class BiometricPromptHost(private val activity: FragmentActivity) {

    fun canAuthenticateStatus(): Int {
        val status = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
        Log.i(TAG, "canAuthenticate status=$status")
        return status
    }

    fun authenticate(
        title: String,
        subtitle: String,
        onResult: (BiometricPromptOutcome) -> Unit
    ) {
        val status = canAuthenticateStatus()
        if (status != BiometricManager.BIOMETRIC_SUCCESS) {
            Log.w(TAG, "prompt skipped status=$status")
            onResult(BiometricPromptOutcome.Unavailable(status))
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    Log.i(TAG, "success type=${result.authenticationType}")
                    onResult(BiometricPromptOutcome.Success)
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    Log.w(TAG, "error code=$errorCode message=$errString")
                    onResult(
                        BiometricPromptOutcome.Error(
                            errorCode = errorCode,
                            message = errString.toString()
                        )
                    )
                }

                override fun onAuthenticationFailed() {
                    Log.w(TAG, "unrecognized biometric")
                }
            }
        )

        // DEVICE_CREDENTIAL replaces setNegativeButtonText — the system
        // sheet already offers the device PIN as the fallback.
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        Log.i(TAG, "showing BiometricPrompt")
        prompt.authenticate(info)
    }

    companion object {
        const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        private const val TAG = "Biometric"

        fun messageForReason(
            activity: FragmentActivity,
            reason: com.example.prog7314_part2.data.biometric.BiometricBlockReason
        ): String {
            val id = when (reason) {
                com.example.prog7314_part2.data.biometric.BiometricBlockReason.NO_HARDWARE ->
                    com.example.prog7314_part2.R.string.biometric_reason_no_hardware
                com.example.prog7314_part2.data.biometric.BiometricBlockReason.NONE_ENROLLED ->
                    com.example.prog7314_part2.R.string.biometric_reason_none_enrolled
                com.example.prog7314_part2.data.biometric.BiometricBlockReason.HW_UNAVAILABLE ->
                    com.example.prog7314_part2.R.string.biometric_reason_hw_unavailable
                com.example.prog7314_part2.data.biometric.BiometricBlockReason.LOCKOUT ->
                    com.example.prog7314_part2.R.string.biometric_reason_lockout
                else ->
                    com.example.prog7314_part2.R.string.biometric_reason_unavailable
            }
            return activity.getString(id)
        }
    }
}

sealed class BiometricPromptOutcome {
    data object Success : BiometricPromptOutcome()
    data class Unavailable(val status: Int) : BiometricPromptOutcome()
    data class Error(val errorCode: Int, val message: String) : BiometricPromptOutcome()
}

fun BiometricPromptOutcome.blockReason() = when (this) {
    BiometricPromptOutcome.Success ->
        com.example.prog7314_part2.data.biometric.BiometricBlockReason.READY
    is BiometricPromptOutcome.Unavailable ->
        BiometricDecision.reasonForStatus(status)
    is BiometricPromptOutcome.Error ->
        BiometricDecision.reasonForError(errorCode)
}
