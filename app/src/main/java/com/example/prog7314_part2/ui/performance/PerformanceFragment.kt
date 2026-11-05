package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.PerformanceRepository
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.databinding.FragmentPerformanceBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call

/**
 * The Performance tab: a history list plus two action buttons for the
 * "add a session" and "monthly graph" screens.
 *
 * The list is reloaded on every `onResume` so a newly-added session
 * from [AddPerformanceFragment] shows up immediately, including a
 * pending row saved in airplane mode.
 */
class PerformanceFragment : Fragment() {

    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!
    private var historyCall: Call<*>? = null
    private var deleteCall: Call<*>? = null

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
        loadHistory()
    }

    private fun loadHistory() {
        val screen = _binding ?: return
        val sportId = session().sportId
        screen.retryButton.isVisible = false
        screen.emptyHistory.text = getString(R.string.performance_loading)

        if (sportId.isBlank()) {
            bindSessions(emptyList())
            screen.emptyHistory.text = getString(R.string.empty_history)
            screen.emptyHistory.isVisible = true
            return
        }

        historyCall?.cancel()
        screen.emptyHistory.isVisible = false
        historyCall = PerformanceRepository.history(sportId) { sessions, remoteFailed ->
            if (_binding !== screen) return@history
            bindSessions(sessions)
            when {
                remoteFailed && sessions.isEmpty() -> {
                    Log.i("Performance", "load fail sport=$sportId")
                    showError()
                }
                remoteFailed -> {
                    Log.i("Performance", "load fail sport=$sportId showing ${sessions.size} pending")
                    screen.retryButton.isVisible = true
                    screen.emptyHistory.isVisible = false
                }
                sessions.isEmpty() -> {
                    screen.emptyHistory.text = getString(R.string.empty_history)
                    screen.emptyHistory.isVisible = true
                }
                else -> {
                    Log.i("Performance", "loaded ${sessions.size} sessions sport=$sportId")
                    screen.emptyHistory.isVisible = false
                }
            }
        }
    }

    private fun bindSessions(sessions: List<PerformanceSession>) {
        val screen = _binding ?: return
        screen.sessionList.adapter = SessionAdapter(sessions) { row ->
            confirmDelete(row)
        }
    }

    private fun showError() {
        val screen = _binding ?: return
        screen.sessionList.adapter = SessionAdapter(emptyList()) {}
        screen.emptyHistory.text = getString(R.string.error_load_performance)
        screen.emptyHistory.isVisible = true
        screen.retryButton.isVisible = true
    }

    private fun confirmDelete(session: PerformanceSession) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_session)
            .setMessage(R.string.delete_session_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteSession(session) }
            .show()
    }

    private fun deleteSession(session: PerformanceSession) {
        deleteCall?.cancel()
        deleteCall = PerformanceRepository.delete(
            session,
            onSuccess = {
                if (_binding == null) return@delete
                Log.i("Performance", "deleted session ${session.id} pending=${session.pending}")
                loadHistory()
            },
            onError = { _ ->
                if (_binding == null) return@delete
                Log.i("Performance", "delete fail session=${session.id}")
                showError()
            }
        )
    }

    override fun onDestroyView() {
        historyCall?.cancel()
        deleteCall?.cancel()
        historyCall = null
        deleteCall = null
        super.onDestroyView()
        _binding = null
    }
}
