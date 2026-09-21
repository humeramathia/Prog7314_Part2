package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import com.example.prog7314_part2.data.remote.toLocal
import com.example.prog7314_part2.databinding.FragmentPerformanceBinding
import com.example.prog7314_part2.ui.session
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * The Performance tab: a history list plus two action buttons for the
 * "add a session" and "monthly graph" screens.
 *
 * The list is reloaded on every `onResume` so a newly-added session
 * from [AddPerformanceFragment] shows up immediately once the user
 * pops back to this screen.
 */
class PerformanceFragment : Fragment() {

    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!
    private var historyCall: Call<List<PerformanceSessionDto>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.sessionList.layoutManager = LinearLayoutManager(requireContext())
        binding.retryButton.setOnClickListener { loadHistory() }
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_performance_to_add)
        }
        binding.btnGraph.setOnClickListener {
            findNavController().navigate(R.id.action_performance_to_graph)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh on every resume so a saved session from the Add
        // screen appears without a manual pull.
        loadHistory()
    }

    /**
     * Loads the history for the current sport. On failure we swap the
     * empty text for an error message and reveal a Retry button.
     */
    private fun loadHistory() {
        val screen = _binding ?: return
        val sportId = session().sportId
        screen.retryButton.isVisible = false
        screen.emptyHistory.text = getString(R.string.empty_history)

        if (sportId.isBlank()) {
            screen.sessionList.adapter = SessionAdapter(emptyList())
            screen.emptyHistory.isVisible = true
            return
        }

        historyCall?.cancel()
        screen.emptyHistory.isVisible = false

        val call = ApiClient.service.getPerformance(sportId)
        historyCall = call
        call.enqueue(object : Callback<List<PerformanceSessionDto>> {
            override fun onResponse(
                call: Call<List<PerformanceSessionDto>>,
                response: Response<List<PerformanceSessionDto>>
            ) {
                if (_binding !== screen) return
                if (!response.isSuccessful) {
                    showError()
                    return
                }
                // Newest first, mapped to the domain model.
                val sessions = response.body()
                    .orEmpty()
                    .sortedByDescending { it.recordedAt }
                    .map { it.toLocal() }
                screen.sessionList.adapter = SessionAdapter(sessions)
                screen.emptyHistory.isVisible = sessions.isEmpty()
            }

            override fun onFailure(
                call: Call<List<PerformanceSessionDto>>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return
                showError()
            }
        })
    }

    private fun showError() {
        val screen = _binding ?: return
        screen.sessionList.adapter = SessionAdapter(emptyList())
        screen.emptyHistory.text = getString(R.string.error_load_performance)
        screen.emptyHistory.isVisible = true
        screen.retryButton.isVisible = true
    }

    override fun onDestroyView() {
        historyCall?.cancel()
        historyCall = null
        super.onDestroyView()
        _binding = null
    }
}
