package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.SportSphereApp
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.data.SyncWorker
import com.example.prog7314_part2.data.local.SyncPolicy
import com.example.prog7314_part2.data.networkStatus
import com.example.prog7314_part2.databinding.FragmentPerformanceBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** History always observes Room; network failures never clear saved sessions. */
class PerformanceFragment : Fragment() {
    private var _binding: FragmentPerformanceBinding? = null
    private val app get() = requireActivity().application as SportSphereApp
    private var uid = ""
    private var sportId = ""
    private val shownFailures = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return FragmentPerformanceBinding.inflate(inflater, container, false).also { _binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val screen = _binding ?: return
        uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        sportId = session().sportId
        screen.sessionList.layoutManager = LinearLayoutManager(requireContext())
        screen.btnAdd.setOnClickListener { findNavController().navigate(R.id.action_performance_to_add) }
        screen.btnGraph.setOnClickListener { findNavController().navigate(R.id.action_performance_to_graph) }
        screen.retryButton.setOnClickListener {
            SyncWorker.enqueue(requireContext(), uid)
            refresh()
        }
        if (uid.isBlank() || sportId.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    app.performance.observe(uid, sportId).collect { rows ->
                        screen.sessionList.adapter = SessionAdapter(rows, ::confirmDelete)
                        screen.emptyHistory.isVisible = rows.isEmpty()
                        screen.syncWarning.isVisible = rows.any { it.syncState == SyncPolicy.FAILED }
                        val rejected = rows.filter { it.syncState == SyncPolicy.FAILED }.map { it.id }
                        if (rejected.any { it !in shownFailures }) {
                            shownFailures.addAll(rejected)
                            Snackbar.make(screen.root, R.string.performance_sync_warning, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    networkStatus(requireContext()).collect { online ->
                        screen.offlineBanner.isVisible = !online
                        if (online) {
                            SyncWorker.enqueue(requireContext(), uid)
                            refresh()
                        }
                    }
                }
            }
        }
    }

    private fun refresh() {
        val screen = _binding ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            screen.retryButton.isVisible = false
            screen.refreshStatus.isVisible = true
            screen.refreshStatus.setText(R.string.performance_refreshing)
            try {
                app.performance.refresh(uid, sportId)
                screen.refreshStatus.isVisible = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                screen.refreshStatus.setText(R.string.performance_cached)
                screen.retryButton.isVisible = true
            }
        }
    }

    private fun confirmDelete(row: PerformanceSession) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.performance_delete_title)
            .setMessage(R.string.performance_delete_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.performance_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (!app.performance.delete(uid, row.id)) {
                            Toast.makeText(context, R.string.performance_pending_delete, Toast.LENGTH_LONG).show()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        Toast.makeText(context, R.string.performance_delete_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
