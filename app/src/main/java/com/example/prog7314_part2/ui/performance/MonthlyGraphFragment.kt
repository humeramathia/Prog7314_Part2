package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.PerformanceMetrics
import com.example.prog7314_part2.databinding.FragmentMonthlyGraphBinding
import com.example.prog7314_part2.ui.session
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyGraphFragment : Fragment() {

    private var _binding: FragmentMonthlyGraphBinding? = null
    private val binding get() = _binding!!

    private val selectedMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            selectedMonth.timeInMillis =
                savedInstanceState.getLong("selectedMonth")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyGraphBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnPreviousMonth.setOnClickListener {
            selectedMonth.add(Calendar.MONTH, -1)
            renderGraph()
        }

        binding.btnNextMonth.setOnClickListener {
            selectedMonth.add(Calendar.MONTH, 1)
            renderGraph()
        }
    }

    override fun onResume() {
        super.onResume()
        renderGraph()
    }

    private fun renderGraph() {
        val screen = _binding ?: return
        val sportId = session().sportId
        val metric = PerformanceMetrics.forSport(sportId).firstOrNull()

        val monthText = SimpleDateFormat(
            "MMMM yyyy", Locale.getDefault()
        ).format(selectedMonth.time)

        screen.monthLabel.text = monthText

        if (metric == null) {
            screen.metricLabel.text = "Select a supported sport."
            screen.chartScroll.isVisible = false
            screen.emptyGraph.isVisible = true
            screen.graphCaption.text = ""
            return
        }

        screen.metricLabel.text =
            "${session().sportName} · ${metric.label} · daily total"

        val start = selectedMonth.timeInMillis
        val end = (selectedMonth.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }.timeInMillis

        // Ignore old demo entries without sport-specific measurements.
        val entries = FakeRepository.sessionsForSport(sportId)
            .filter { it.recordedAt >= start && it.recordedAt < end }
            .mapNotNull { entry ->
                val value = entry.metrics[metric.key]
                    ?: return@mapNotNull null

                if (!value.isFinite() || value < 0.0) {
                    return@mapNotNull null
                }

                val day = Calendar.getInstance().apply {
                    timeInMillis = entry.recordedAt
                }.get(Calendar.DAY_OF_MONTH)

                day to value
            }

        val dailyTotals = entries
            .groupBy { it.first }
            .toSortedMap()
            .map { (day, values) ->
                day.toString() to values.sumOf { it.second }
            }

        screen.barChart.setData(dailyTotals)
        screen.chartScroll.isVisible = dailyTotals.isNotEmpty()
        screen.emptyGraph.isVisible = dailyTotals.isEmpty()
        screen.chartScroll.scrollTo(0, 0)

        if (dailyTotals.isEmpty()) {
            screen.graphCaption.text =
                "Add a session to see ${metric.label.lowercase()} here."
            return
        }

        val number = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 2
        }

        val total = dailyTotals.sumOf { it.second }

        screen.graphCaption.text =
            "${entries.size} session(s) recorded.\n" +
                    "Month total — ${metric.label}: ${number.format(total)}\n" +
                    "Each bar totals one recorded day. " +
                    "Days without entries are omitted. Swipe sideways if needed."

        screen.barChart.contentDescription =
            "$monthText. ${metric.label}, daily totals. " +
                    dailyTotals.joinToString(". ") { (day, value) ->
                        "Day $day: ${number.format(value)}"
                    }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("selectedMonth", selectedMonth.timeInMillis)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}