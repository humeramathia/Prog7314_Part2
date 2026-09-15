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
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.databinding.FragmentPerformanceBinding
import com.example.prog7314_part2.ui.session

class PerformanceFragment : Fragment() {

    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.sessionList.layoutManager = LinearLayoutManager(requireContext())
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_performance_to_add)
        }
        binding.btnGraph.setOnClickListener {
            findNavController().navigate(R.id.action_performance_to_graph)
        }
    }

    override fun onResume() {
        super.onResume()
        val sessions = FakeRepository.sessionsForSport(session().sportId)
        binding.sessionList.adapter = SessionAdapter(sessions)
        binding.emptyHistory.isVisible = sessions.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
