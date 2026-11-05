package com.example.prog7314_part2.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.ClearCredentialException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.SportSphereApp
import com.example.prog7314_part2.data.SyncWorker
import com.example.prog7314_part2.data.biometric.BiometricDecision
import com.example.prog7314_part2.data.biometric.BiometricSession
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.text.DateFormat
import java.util.Date
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.UpdateProfileRequest
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.example.prog7314_part2.databinding.FragmentSettingsBinding
import com.example.prog7314_part2.notifications.PushRegistrar
import com.example.prog7314_part2.ui.auth.BiometricEnrollment
import com.example.prog7314_part2.ui.auth.BiometricPromptHost
import com.example.prog7314_part2.ui.auth.BiometricPromptOutcome
import com.example.prog7314_part2.ui.auth.ProfileSync
import com.example.prog7314_part2.ui.auth.blockReason
import com.example.prog7314_part2.ui.biometrics
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var darkModeCall: Call<UserProfileDto>? = null
    private var nameCall: Call<UserProfileDto>? = null

    /** Notifications card (switch, status line, test button). */
    private var notifications: NotificationSettingsController? = null

    /** Registered up front because launchers cannot be created after onStart. */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notifications?.onPermissionResult(granted)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = session()
        binding.userEmail.text = session.email
        binding.displayNameInput.setText(session.displayName)
        binding.btnChangeSport.text = session.sportName.ifBlank { getString(R.string.choose_sport) }
        binding.darkModeSwitch.isChecked = session.darkMode
        bindBiometricSwitch()

        binding.btnSaveName.setOnClickListener { saveDisplayName() }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val app = requireActivity().application as SportSphereApp
            binding.btnSyncNow.setOnClickListener { SyncWorker.enqueue(requireContext(), uid) }
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    app.performance.lastSync(uid).collect { time ->
                        _binding?.lastSync?.text = if (time == null) {
                            getString(R.string.performance_never_synced)
                        } else {
                            getString(
                                R.string.performance_last_sync,
                                DateFormat.getDateTimeInstance().format(Date(time))
                            )
                        }
                    }
                }
            }
        }
        binding.btnChangeSport.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_sport)
        }

        binding.darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            session.darkMode = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )

            darkModeCall?.cancel()
            val call = ApiClient.service.updateMyProfile(UpdateProfileRequest(darkMode = checked))
            darkModeCall = call
            call.enqueue(
                object : Callback<UserProfileDto> {
                    override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                        val profile = response.body() ?: return
                        if (_binding == null) return
                        ProfileSync.apply(session(), profile)
                    }
                    override fun onFailure(call: Call<UserProfileDto>, t: Throwable) = Unit
                }
            )
        }

        binding.btnLogout.setOnClickListener { signOut() }

        notifications = NotificationSettingsController(this, binding, notificationPermission).also { it.bind() }
        // English / Afrikaans / isiXhosa picker.
        LanguageSettingsController(binding).bind()
    }

    override fun onResume() {
        super.onResume()
        // Re-check in case the user allowed or blocked notifications in Android settings.
        notifications?.refresh()
    }

    private fun bindBiometricSwitch() {
        val host = BiometricPromptHost(requireActivity())
        val status = host.canAuthenticateStatus()
        val enabled = biometrics().enabledFor(ProfileSync.currentUserId())
        renderBiometricStatus(enabled, status)

        binding.biometricSwitch.setOnCheckedChangeListener(null)
        binding.biometricSwitch.isChecked = enabled
        if (!BiometricDecision.isReady(status) && !enabled) {
            binding.biometricSwitch.isEnabled = true
        }

        binding.biometricSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                enableBiometrics(host)
            } else {
                BiometricEnrollment.persistEnabled(this, false) {
                    if (_binding == null) return@persistEnabled
                    showMessage(getString(R.string.biometric_disabled))
                    renderBiometricStatus(false, host.canAuthenticateStatus())
                }
            }
        }
    }

    private fun enableBiometrics(host: BiometricPromptHost) {
        host.authenticate(
            title = getString(R.string.biometric_prompt_enable_title),
            subtitle = getString(R.string.biometric_prompt_enable_subtitle)
        ) { outcome ->
            if (_binding == null) return@authenticate
            when (outcome) {
                BiometricPromptOutcome.Success -> {
                    BiometricSession.markUnlocked()
                    BiometricEnrollment.persistEnabled(this, true) {
                        if (_binding == null) return@persistEnabled
                        showMessage(getString(R.string.biometric_enabled))
                        renderBiometricStatus(true, host.canAuthenticateStatus())
                    }
                }
                else -> {
                    Log.w(TAG, "settings enable failed $outcome")
                    binding.biometricSwitch.setOnCheckedChangeListener(null)
                    binding.biometricSwitch.isChecked = false
                    bindBiometricSwitch()
                    val reason = BiometricPromptHost.messageForReason(
                        requireActivity(),
                        outcome.blockReason()
                    )
                    if (outcome is BiometricPromptOutcome.Unavailable ||
                        (outcome is BiometricPromptOutcome.Error &&
                            !BiometricDecision.isUserCancel(outcome.errorCode))
                    ) {
                        showMessage(getString(R.string.biometric_status_unavailable, reason))
                    } else {
                        showMessage(getString(R.string.biometric_cancelled))
                    }
                }
            }
        }
    }

    private fun renderBiometricStatus(enabled: Boolean, status: Int) {
        binding.biometricStatus.text = when {
            enabled -> getString(R.string.biometric_status_on)
            !BiometricDecision.isReady(status) -> getString(
                R.string.biometric_status_unavailable,
                BiometricPromptHost.messageForReason(
                    requireActivity(),
                    BiometricDecision.reasonForStatus(status)
                )
            )
            else -> getString(R.string.biometric_status_off)
        }
    }

    private fun saveDisplayName() {
        val screen = binding
        val name = screen.displayNameInput.text?.toString().orEmpty().trim()
        screen.displayNameLayout.error = null
        if (name.isBlank()) {
            screen.displayNameLayout.error = getString(R.string.required_field)
            return
        }
        if (name.length > 60) {
            screen.displayNameLayout.error = getString(R.string.error_name_length)
            return
        }

        screen.btnSaveName.isEnabled = false
        val call = ApiClient.service.updateMyProfile(UpdateProfileRequest(displayName = name))
        nameCall = call
        call.enqueue(object : Callback<UserProfileDto> {
            override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                if (_binding !== screen) return
                screen.btnSaveName.isEnabled = true
                val profile = response.body()
                if (!response.isSuccessful || profile == null) {
                    showMessage(getString(R.string.error_save_name))
                    return
                }
                ProfileSync.apply(session(), profile)
                FirebaseAuth.getInstance().currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(profile.displayName).build()
                )
                showMessage(getString(R.string.name_saved))
            }

            override fun onFailure(call: Call<UserProfileDto>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                screen.btnSaveName.isEnabled = true
                showMessage(ProfileSync.UNAVAILABLE)
            }
        })
    }

    private fun signOut() {
        // Stop pushes for this account before the session is cleared.
        PushRegistrar.onSignOut(requireContext())
        FirebaseAuth.getInstance().signOut()
        session().signOut()
        BiometricSession.lock()
        viewLifecycleOwner.lifecycleScope.launch { biometrics().clear() }
        clearGoogleState()
        findNavController().navigate(R.id.action_settings_to_welcome)
    }

    private fun clearGoogleState() {
        val appContext = context?.applicationContext ?: return
        try {
            val callback: CredentialManagerCallback<Void?, ClearCredentialException> =
                object : CredentialManagerCallback<Void?, ClearCredentialException> {
                    override fun onResult(result: Void?) = Unit
                    override fun onError(e: ClearCredentialException) = Unit
                }
            CredentialManager.create(appContext).clearCredentialStateAsync(
                ClearCredentialStateRequest(),
                null,
                ContextCompat.getMainExecutor(appContext),
                callback
            )
        } catch (_: Exception) {
            // Older Play services builds can throw; Firebase sign-out already ran.
        }
    }

    private fun showMessage(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroyView() {
        notifications?.onDestroyView()
        notifications = null
        darkModeCall?.cancel()
        nameCall?.cancel()
        darkModeCall = null
        nameCall = null
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "Biometric"
    }
}
