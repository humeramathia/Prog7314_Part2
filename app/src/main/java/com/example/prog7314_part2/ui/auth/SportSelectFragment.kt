package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
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

class SportSelectFragment : Fragment() {

    private var _binding: FragmentSportSelectBinding? = null
    private val binding get() = _binding!!

    private var selected: Sport? = null
    private var sportsCall: Call<List<SportDto>>? = null
    private var updateCall: Call<UserProfileDto>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportSelectBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        binding.sportList.layoutManager =
            LinearLayoutManager(requireContext())

        binding.btnContinue.isEnabled = false
        binding.btnContinue.setOnClickListener {
            saveSelectedSport()
        }

        loadSports()
    }

    private fun loadSports() {
        val screen = binding

        screen.btnContinue.isEnabled = false

        val call = ApiClient.service.getSports()
        sportsCall = call

        call.enqueue(object : Callback<List<SportDto>> {

            override fun onResponse(
                call: Call<List<SportDto>>,
                response: Response<List<SportDto>>
            ) {
                if (_binding !== screen) return

                val sports = response.body()
                    ?.map { Sport(it.sportId, it.name) }
                    .orEmpty()

                if (!response.isSuccessful || sports.isEmpty()) {
                    showMessage(
                        "Could not load sports from the API."
                    )
                    return
                }

                selected = sports.find {
                    it.id == session().sportId
                }

                screen.sportList.adapter = SportAdapter(
                    sports,
                    session().sportId
                ) { sport ->
                    selected = sport
                    screen.btnContinue.isEnabled = true
                }

                screen.btnContinue.isEnabled = selected != null
            }

            override fun onFailure(
                call: Call<List<SportDto>>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return

                showMessage(
                    "Could not connect to the API. " +
                            "Check that it is running on port 3000."
                )
            }
        })
    }

    private fun saveSelectedSport() {
        val screen = binding
        val sport = selected ?: return

        setLoading(true)

        val request = UpdateProfileRequest(
            sportId = sport.id,
            sportName = sport.name
        )

        val call = ApiClient.service.updateMyProfile(request)
        updateCall = call

        call.enqueue(object : Callback<UserProfileDto> {

            override fun onResponse(
                call: Call<UserProfileDto>,
                response: Response<UserProfileDto>
            ) {
                if (_binding !== screen) return

                val profile = response.body()

                if (!response.isSuccessful || profile == null) {
                    setLoading(false)
                    showMessage("Could not save your selected sport.")
                    return
                }

                session().sportId = profile.sportId
                session().sportName = profile.sportName

                setLoading(false)
                findNavController().navigate(
                    R.id.action_sport_to_home
                )
            }

            override fun onFailure(
                call: Call<UserProfileDto>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return

                setLoading(false)
                showMessage(
                    "Could not connect to the API. " +
                            "Your sport was not saved."
                )
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            sportList.isEnabled = !loading
            btnContinue.isEnabled = !loading && selected != null
        }
    }

    private fun showMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        sportsCall?.cancel()
        updateCall?.cancel()
        sportsCall = null
        updateCall = null
        _binding = null
        super.onDestroyView()
    }
}