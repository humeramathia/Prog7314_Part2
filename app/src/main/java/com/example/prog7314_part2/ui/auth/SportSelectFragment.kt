package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.Sport
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.SportDto
import com.example.prog7314_part2.data.remote.UpdateProfileRequest
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.example.prog7314_part2.databinding.FragmentSportSelectBinding
import com.example.prog7314_part2.ui.session
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Onboarding screen where the user picks the one sport the whole app
 * will be scoped to. Also reachable from Settings for changing the
 * selection later.
 *
 * If `GET /api/sports` fails we degrade to the built-in
 * [FakeRepository.sports] catalogue so the screen is never empty. The
 * selection is persisted through `PATCH /api/me`; when that also fails
 * we save the choice on device only and continue with a warning toast.
 */
class SportSelectFragment : Fragment() {

    private var _binding: FragmentSportSelectBinding? = null
    private val binding get() = _binding!!

    /** Currently highlighted sport in the list. */
    private var selected: Sport? = null

    private var sportsCall: Call<List<SportDto>>? = null
    private var updateCall: Call<UserProfileDto>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.sportList.layoutManager = LinearLayoutManager(requireContext())
        binding.btnContinue.isEnabled = false
        binding.btnContinue.setOnClickListener { saveSelectedSport() }
        binding.btnRetrySports.setOnClickListener { loadSports() }
        loadSports()
    }

    /**
     * Pulls the sport catalogue from the API. On any failure — non-200,
     * empty list, or thrown exception — we fall back to [FakeRepository]
     * so the picker still works offline.
     */
    private fun loadSports() {
        val screen = _binding ?: return
        selected = null
        screen.btnContinue.isEnabled = false
        screen.sportsErrorGroup.isVisible = false
        screen.sportsLoading.isVisible = true
        screen.sportList.adapter = SportAdapter(emptyList(), "") {}

        sportsCall?.cancel()
        val call = ApiClient.service.getSports()
        sportsCall = call
        call.enqueue(object : Callback<List<SportDto>> {
            override fun onResponse(call: Call<List<SportDto>>, response: Response<List<SportDto>>) {
                if (_binding !== screen) return
                screen.sportsLoading.isVisible = false
                val sports = response.body()
                    // Prefer `sportId` (canonical) but fall back to `id`
                    // for older seed data that lacked the alias.
                    ?.map { Sport(it.sportId.ifBlank { it.id }, it.name) }
                    ?.filter { it.id.isNotBlank() && it.name.isNotBlank() }
                    .orEmpty()
                if (!response.isSuccessful || sports.isEmpty()) {
                    showFallback(getString(R.string.error_load_sports))
                    return
                }
                bindSports(sports)
            }

            override fun onFailure(call: Call<List<SportDto>>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                screen.sportsLoading.isVisible = false
                showFallback(getString(R.string.error_load_sports))
            }
        })
    }

    /** Shows a small error banner and switches the list to the local catalogue. */
    private fun showFallback(message: String) {
        val screen = _binding ?: return
        screen.sportsErrorGroup.isVisible = true
        screen.sportsErrorText.text = message
        bindSports(FakeRepository.sports)
    }

    /** Renders the list and pre-selects whatever the user picked last time. */
    private fun bindSports(sports: List<Sport>) {
        val screen = _binding ?: return
        selected = sports.find { it.id == session().sportId }
        screen.sportList.adapter = SportAdapter(sports, session().sportId) { sport ->
            selected = sport
            screen.btnContinue.isEnabled = true
        }
        screen.btnContinue.isEnabled = selected != null
    }

    /**
     * Persists the pick to the API and, on success, navigates to Home.
     * If the API call fails we still save locally so the user is not
     * stranded on this screen every time the network hiccups.
     */
    private fun saveSelectedSport() {
        val screen = binding
        val sport = selected ?: return
        setLoading(true)

        val request = UpdateProfileRequest(sportId = sport.id, sportName = sport.name)
        val call = ApiClient.service.updateMyProfile(request)
        updateCall = call
        call.enqueue(object : Callback<UserProfileDto> {
            override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                if (_binding !== screen) return
                val profile = response.body()
                if (!response.isSuccessful || profile == null) {
                    finishWithLocalSport(sport, "Sport saved on this device. Could not reach the API profile.")
                    return
                }
                session().sportId = profile.sportId
                session().sportName = profile.sportName
                setLoading(false)
                findNavController().navigate(R.id.action_sport_to_home)
            }

            override fun onFailure(call: Call<UserProfileDto>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                finishWithLocalSport(sport, "Sport saved on this device. Start the API on port 3000 to sync.")
            }
        })
    }

    /** Falls back to a local-only save, warns the user, then continues. */
    private fun finishWithLocalSport(sport: Sport, message: String) {
        session().sportId = sport.id
        session().sportName = sport.name
        setLoading(false)
        showMessage(message)
        findNavController().navigate(R.id.action_sport_to_home)
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            sportList.isEnabled = !loading
            btnRetrySports.isEnabled = !loading
            btnContinue.isEnabled = !loading && selected != null
        }
    }

    private fun showMessage(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroyView() {
        sportsCall?.cancel()
        updateCall?.cancel()
        sportsCall = null
        updateCall = null
        super.onDestroyView()
        _binding = null
    }
}
