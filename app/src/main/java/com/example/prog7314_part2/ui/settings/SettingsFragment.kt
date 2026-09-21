package com.example.prog7314_part2.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.UpdateProfileRequest
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.example.prog7314_part2.databinding.FragmentSettingsBinding
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Small settings screen shown from the gear button on Home.
 *
 * Offers three actions: change the selected sport, toggle dark mode
 * (which is applied instantly and PATCHed back to the API), and log
 * out (which signs the user out of both Firebase and local prefs).
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    /** In-flight dark-mode PATCH; cancelled on rapid toggles to avoid races. */
    private var darkModeCall: Call<UserProfileDto>? = null

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
        binding.btnChangeSport.text = session.sportName.ifBlank { getString(R.string.choose_sport) }
        binding.darkModeSwitch.isChecked = session.darkMode

        binding.btnChangeSport.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_sport)
        }

        binding.darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            // Apply locally first so the theme flips instantly, even
            // when the API is slow or unreachable.
            session.darkMode = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )

            // Cancel any in-flight call so rapid toggles don't queue up
            // conflicting requests.
            darkModeCall?.cancel()
            val call = ApiClient.service.updateMyProfile(UpdateProfileRequest(darkMode = checked))
            darkModeCall = call
            call.enqueue(
                object : Callback<UserProfileDto> {
                    // Failures are silent — the local pref already reflects
                    // the user's choice, and the next successful profile
                    // fetch will re-sync.
                    override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) = Unit
                    override fun onFailure(call: Call<UserProfileDto>, t: Throwable) = Unit
                }
            )
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            session.signOut()
            findNavController().navigate(R.id.action_settings_to_welcome)
        }
    }

    override fun onDestroyView() {
        darkModeCall?.cancel()
        darkModeCall = null
        super.onDestroyView()
        _binding = null
    }
}
