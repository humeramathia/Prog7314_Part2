package com.example.prog7314_part2.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.SportMetrics
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CalendarEventDto
import com.example.prog7314_part2.databinding.FragmentHomeBinding
import com.example.prog7314_part2.ui.performance
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * The dashboard the user lands on after login.
 *
 * Displays two tiles — "Next practice" and "Latest session" — and links
 * to each detailed tab. The latest session observes the local Room queue;
 * the next event comes from the API.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var nextCall: Call<CalendarEventDto>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = session()

        val name = session.displayName.ifBlank { getString(R.string.athlete_fallback) }
        binding.greeting.text = getString(R.string.home_greeting, name)
        binding.sportChip.text = session.sportName.ifBlank { getString(R.string.choose_sport) }

        binding.nextPracticeTitle.text = getString(R.string.empty_events)
        binding.nextPracticeWhen.text = ""
        binding.latestScoreValue.text = getString(R.string.empty_history)

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.nextPracticeCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }
        binding.latestScoreCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_performance)
        }
        binding.btnLearn.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_learn)
        }

        observeLatestSession(session.sportId)
    }

    override fun onResume() {
        super.onResume()
        loadNextEvent(session().sportId)
    }

    private fun observeLatestSession(sportId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val repo = performance()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    try {
                        if (sportId.isNotBlank()) repo.refresh(uid, sportId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Keep the local latest-session tile available offline.
                    }
                }
                repo.observe(uid, sportId).collect { rows ->
                    val latest = rows.firstOrNull()
                    val screen = _binding ?: return@collect
                    if (latest == null) {
                        screen.latestScoreValue.text = getString(R.string.empty_history)
                        return@collect
                    }
                    val metricsLine = SportMetrics.summary(latest.sportId, latest.metrics)
                    screen.latestScoreValue.text = if (latest.notes.isBlank()) {
                        metricsLine
                    } else {
                        "$metricsLine  ·  ${latest.notes}"
                    }
                    Log.i("Performance", "home latest session=${latest.id} pending=${latest.pending}")
                }
            }
        }
    }

    private fun loadNextEvent(sportId: String) {
        val screen = binding
        if (sportId.isBlank()) return

        nextCall?.cancel()
        val upcoming = ApiClient.service.getNextEvent(sportId)
        nextCall = upcoming
        upcoming.enqueue(object : Callback<CalendarEventDto> {
            override fun onResponse(call: Call<CalendarEventDto>, response: Response<CalendarEventDto>) {
                if (_binding !== screen) return
                val event = response.body()
                if (!response.isSuccessful || event == null) return
                val format = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
                screen.nextPracticeTitle.text = event.title
                screen.nextPracticeWhen.text = format.format(Date(event.startsAt))
            }

            override fun onFailure(call: Call<CalendarEventDto>, t: Throwable) {
                if (_binding !== screen || call.isCanceled) return
            }
        })
    }

    override fun onDestroyView() {
        nextCall?.cancel()
        nextCall = null
        super.onDestroyView()
        _binding = null
    }
}
