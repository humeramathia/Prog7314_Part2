package com.example.prog7314_part2.ui.settings

import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.UpdateProfileRequest
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.example.prog7314_part2.databinding.FragmentSettingsBinding
import com.example.prog7314_part2.ui.auth.ProfileSync
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var darkModeCall: Call<UserProfileDto>? = null
    private var nameCall: Call<UserProfileDto>? = null

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

        binding.btnSaveName.setOnClickListener { saveDisplayName() }
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
        FirebaseAuth.getInstance().signOut()
        session().signOut()
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
        darkModeCall?.cancel()
        nameCall?.cancel()
        darkModeCall = null
        nameCall = null
        super.onDestroyView()
        _binding = null
    }
}
