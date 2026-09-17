package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.SportMetrics
import com.example.prog7314_part2.databinding.FragmentMonthlyGraphBinding
import com.example.prog7314_part2.ui.session
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyGraphFragment : Fragment() {

    private var _binding: FragmentMonthlyGraphBinding? = null
    private val binding get() = _binding!!
    private val visibleMonth = Calendar.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonthlyGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnPrevMonth.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, -1)
            render()
        }
        binding.btnNextMonth.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, 1)
            render()
        }
        render()
    }

    private fun render() {
        val sportId = session().sportId
        val year = visibleMonth.get(Calendar.YEAR)
        val month = visibleMonth.get(Calendar.MONTH)
        val metric = SportMetrics.fields(sportId).first()
        val points = FakeRepository.monthSeries(sportId, year, month, metric.key)
        binding.monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(visibleMonth.time)
        binding.barChart.setData(
            points.map { (label, value) ->
                label to value.toDouble()
            }
        )
        binding.graphCaption.text = if (points.isEmpty()) {
            getString(R.string.empty_month_graph)
        } else {
            "${metric.label} by date"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
