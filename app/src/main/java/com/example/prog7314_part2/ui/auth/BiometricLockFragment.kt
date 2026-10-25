package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.biometric.BiometricBlockReason
import com.example.prog7314_part2.data.biometric.BiometricDecision
import com.example.prog7314_part2.data.biometric.BiometricSession
import com.example.prog7314_part2.databinding.FragmentBiometricLockBinding
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth

/**
 * Shown on a cold start **before** Home or any API-backed screen, so
 * club data is not visible until [BiometricPrompt] succeeds
 * (PROG7314 POE tips, s. 3).
 *
 * Cancel leaves the user on this screen with "Unlock" and
 * "Sign in again" — they can use the device PIN from the system sheet
 * or fall back to SSO.
 */
class BiometricLockFragment : Fragment() {

    private var _binding: FragmentBiometricLockBinding? = null
    private val binding get() = _binding!!

    private lateinit var prompt: BiometricPromptHost

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBiometricLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prompt = BiometricPromptHost(requireActivity())
        binding.lockBody.text = getString(R.string.biometric_lock_body, session().email)
        binding.btnUnlock.setOnClickListener { unlock() }
        binding.btnSignInAgain.setOnClickListener { returnToLogin() }
        Log.i(TAG, "lock screen created, showing prompt")
        unlock()
    }

    private fun unlock() {
        prompt.authenticate(
            title = getString(R.string.biometric_prompt_title),
            subtitle = getString(R.string.biometric_prompt_subtitle)
        ) { outcome ->
            if (_binding == null) return@authenticate
            when (outcome) {
                BiometricPromptOutcome.Success -> {
                    BiometricSession.markUnlocked()
                    Log.i(TAG, "unlocked, opening clubhouse")
                    openClubhouse()
                }
                is BiometricPromptOutcome.Unavailable -> showBlocked(outcome.blockReason())
                is BiometricPromptOutcome.Error -> {
                    val reason = outcome.blockReason()
                    if (reason == BiometricBlockReason.CANCELLED) {
                        binding.lockStatus.text = getString(R.string.biometric_cancelled)
                    } else {
                        showBlocked(reason)
                    }
                }
            }
        }
    }

    private fun showBlocked(reason: BiometricBlockReason) {
        val detail = BiometricPromptHost.messageForReason(requireActivity(), reason)
        binding.lockStatus.text = getString(R.string.biometric_status_unavailable, detail)
        if (!BiometricDecision.shouldGate(
                loggedIn = session().isLoggedIn,
                enabledForUser = true,
                unlockedThisProcess = false,
                canAuthenticateStatus = prompt.canAuthenticateStatus()
            )
        ) {
            Log.w(TAG, "device can no longer prompt; opening clubhouse so the user is not locked out")
            BiometricSession.markUnlocked()
            openClubhouse()
        }
    }

    private fun openClubhouse() {
        val destination = if (session().sportId.isBlank()) {
            R.id.action_lock_to_sport
        } else {
            R.id.action_lock_to_home
        }
        findNavController().navigate(destination)
    }

    private fun returnToLogin() {
        Log.i(TAG, "user chose sign-in again from lock screen")
        FirebaseAuth.getInstance().signOut()
        session().signOut()
        BiometricSession.lock()
        findNavController().navigate(R.id.action_lock_to_login)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "Biometric"
    }
}
