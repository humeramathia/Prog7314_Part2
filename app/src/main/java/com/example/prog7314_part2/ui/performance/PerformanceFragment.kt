package com.example.prog7314_part2.ui.performance

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
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import com.example.prog7314_part2.databinding.FragmentPerformanceBinding
import com.example.prog7314_part2.ui.session
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerformanceFragment : Fragment() {

    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!

    private var historyCall: Call<List<PerformanceSessionDto>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerformanceBinding.inflate(
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
        binding.sessionList.layoutManager =
            LinearLayoutManager(requireContext())

        binding.btnAdd.setOnClickListener {
            findNavController().navigate(
                R.id.action_performance_to_add
            )
        }

        binding.btnGraph.setOnClickListener {
            findNavController().navigate(
                R.id.action_performance_to_graph
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val screen = _binding ?: return
        val sportId = session().sportId

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
                    showMessage("Could not load performance history.")
                    screen.emptyHistory.isVisible = true
                    return
                }

                val sessions = response.body()
                    .orEmpty()
                    .sortedByDescending { it.recordedAt }
                    .map { it.toLocalSession() }

                screen.sessionList.adapter =
                    SessionAdapter(sessions)

                screen.emptyHistory.isVisible =
                    sessions.isEmpty()
            }

            override fun onFailure(
                call: Call<List<PerformanceSessionDto>>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return

                screen.emptyHistory.isVisible = true
                showMessage(
                    "Could not connect to the API. " +
                            "Check that it is running."
                )
            }
        })
    }

    private fun PerformanceSessionDto.toLocalSession() =
        PerformanceSession(
            id = id,
            sportId = sportId,
            recordedAt = recordedAt,
            metrics = metrics,
            notes = notes
        )

    private fun showMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        historyCall?.cancel()
        historyCall = null
        _binding = null
        super.onDestroyView()
    }
}