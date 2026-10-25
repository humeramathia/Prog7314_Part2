package com.example.prog7314_part2.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.biometric.BiometricDecision
import com.example.prog7314_part2.data.biometric.BiometricSession
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.UpdateProfileRequest
import com.example.prog7314_part2.ui.biometrics
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * After SSO / email sign-in, ask whether the user wants fingerprint
 * unlock next time. The answer is stored in DataStore and on
 * `PATCH /api/me` — never as a password (PROG7314 POE tips, s. 3).
 */
object BiometricEnrollment {

    fun offerAfterSignIn(fragment: Fragment, onContinue: () -> Unit) {
        BiometricSession.markUnlocked()
        val activity = fragment.activity ?: return onContinue()
        val host = BiometricPromptHost(activity)
        val status = host.canAuthenticateStatus()
        val alreadyOn = fragment.biometrics().enabledFor(ProfileSync.currentUserId())
        if (!BiometricDecision.shouldOfferEnrollment(alreadyOn, status)) {
            Log.i(TAG, "skip offer alreadyOn=$alreadyOn status=$status")
            onContinue()
            return
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.biometric_offer_title)
            .setMessage(R.string.biometric_offer_body)
            .setPositiveButton(R.string.biometric_offer_yes) { _, _ ->
                confirmOnDevice(fragment, host, onContinue)
            }
            .setNegativeButton(R.string.biometric_offer_no) { _, _ ->
                Log.i(TAG, "user declined enrollment")
                onContinue()
            }
            .setOnCancelListener { onContinue() }
            .show()
    }

    fun persistEnabled(fragment: Fragment, enabled: Boolean, onDone: () -> Unit = {}) {
        val uid = ProfileSync.currentUserId()
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.biometrics().setEnabled(uid, enabled)
            ApiClient.service.updateMyProfile(UpdateProfileRequest(biometricEnabled = enabled))
                .enqueue(IgnoreResponse)
            onDone()
        }
    }

    private fun confirmOnDevice(
        fragment: Fragment,
        host: BiometricPromptHost,
        onContinue: () -> Unit
    ) {
        host.authenticate(
            title = fragment.getString(R.string.biometric_prompt_enable_title),
            subtitle = fragment.getString(R.string.biometric_prompt_enable_subtitle)
        ) { outcome ->
            if (fragment.view == null) {
                onContinue()
                return@authenticate
            }
            when (outcome) {
                BiometricPromptOutcome.Success -> {
                    persistEnabled(fragment, true) {
                        Toast.makeText(
                            fragment.requireContext(),
                            R.string.biometric_enabled,
                            Toast.LENGTH_SHORT
                        ).show()
                        onContinue()
                    }
                }
                else -> {
                    Log.w(TAG, "enrollment prompt did not succeed")
                    onContinue()
                }
            }
        }
    }

    private val IgnoreResponse =
        object : retrofit2.Callback<com.example.prog7314_part2.data.remote.UserProfileDto> {
            override fun onResponse(
                call: retrofit2.Call<com.example.prog7314_part2.data.remote.UserProfileDto>,
                response: retrofit2.Response<com.example.prog7314_part2.data.remote.UserProfileDto>
            ) {
                Log.i(TAG, "profile biometricEnabled sync http=${response.code()}")
            }

            override fun onFailure(
                call: retrofit2.Call<com.example.prog7314_part2.data.remote.UserProfileDto>,
                error: Throwable
            ) {
                Log.w(TAG, "profile biometricEnabled sync failed: ${error.message}")
            }
        }

    private const val TAG = "Biometric"
}
