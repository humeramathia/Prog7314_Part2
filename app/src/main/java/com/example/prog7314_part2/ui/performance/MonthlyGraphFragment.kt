package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.databinding.FragmentMonthlyGraphBinding
import com.example.prog7314_part2.ui.session

class MonthlyGraphFragment : Fragment() {

    private var _binding: FragmentMonthlyGraphBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonthlyGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val points = FakeRepository.monthlyAverages(session().sportId)
        binding.barChart.setData(points)
        val peak = points.maxByOrNull { it.second }
        binding.graphCaption.text = if (peak == null || peak.second == 0f) {
            "Add sessions to see monthly averages."
        } else {
            "Highest monthly average: ${peak.first} (${peak.second.toInt()})"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
