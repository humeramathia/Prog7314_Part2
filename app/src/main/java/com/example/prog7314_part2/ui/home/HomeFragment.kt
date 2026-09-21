package com.example.prog7314_part2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CalendarEventDto
import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import com.example.prog7314_part2.data.remote.summaryLine
import com.example.prog7314_part2.databinding.FragmentHomeBinding
import com.example.prog7314_part2.ui.session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * The dashboard the user lands on after login.
 *
 * Displays two tiles — "Next practice" and "Latest session" — and links
 * to each detailed tab. Both tiles pull their data from the API on
 * every entry; failures leave the placeholder text in place instead of
 * showing an error, because the four bottom-nav tabs already surface
 * connectivity problems in-context.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var nextCall: Call<CalendarEventDto>? = null
    private var latestCall: Call<List<PerformanceSessionDto>>? = null

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

        // Personalised greeting; falls back to "athlete" when no display
        // name is available (e.g. Google account with no profile name).
        val name = session.displayName.ifBlank { "athlete" }
        binding.greeting.text = "Hi, $name"
        binding.sportChip.text = session.sportName.ifBlank { getString(R.string.choose_sport) }

        // Placeholder copy until the API responses land.
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

        loadDashboard(session.sportId)
    }

    /**
     * Fires the two dashboard requests in parallel. Both silently
     * short-circuit on failure — the tiles keep their placeholder copy.
     */
    private fun loadDashboard(sportId: String) {
        val screen = binding
        if (sportId.isBlank()) return

        nextCall?.cancel()
        latestCall?.cancel()

        // Next practice tile.
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

        // Latest performance tile. `maxByOrNull` handles the "no sessions
        // yet" case gracefully by returning null.
        val history = ApiClient.service.getPerformance(sportId)
        latestCall = history
        history.enqueue(object : Callback<List<PerformanceSessionDto>> {
            override fun onResponse(
                call: Call<List<PerformanceSessionDto>>,
                response: Response<List<PerformanceSessionDto>>
            ) {
                if (_binding !== screen) return
                val latest = response.body().orEmpty().maxByOrNull { it.recordedAt } ?: return
                screen.latestScoreValue.text = latest.summaryLine()
            }

            override fun onFailure(call: Call<List<PerformanceSessionDto>>, t: Throwable) {
                if (_binding !== screen || call.isCanceled) return
            }
        })
    }

    override fun onDestroyView() {
        nextCall?.cancel()
        latestCall?.cancel()
        nextCall = null
        latestCall = null
        super.onDestroyView()
        _binding = null
    }
}
