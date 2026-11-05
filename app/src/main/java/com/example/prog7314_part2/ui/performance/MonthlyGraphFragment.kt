package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.MetricField
import com.example.prog7314_part2.data.SportMetrics
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.MonthlyPerformanceDto
import com.example.prog7314_part2.databinding.FragmentMonthlyGraphBinding
import com.example.prog7314_part2.ui.session
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MonthlyGraphFragment : Fragment() {

    private var _binding: FragmentMonthlyGraphBinding? = null
    private val binding get() = _binding!!

    private val visibleMonth = Calendar.getInstance()
    private var graphCall: Call<*>? = null
    private var selectedMetric: String = ""
    private var metricFields: List<MetricField> = emptyList()
    private var spinnerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            visibleMonth.timeInMillis = savedInstanceState.getLong(
                KEY_VISIBLE_MONTH,
                System.currentTimeMillis()
            )
            selectedMetric = savedInstanceState.getString(KEY_METRIC).orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnPrevMonth.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, -1)
            loadGraph()
        }
        binding.btnNextMonth.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, 1)
            loadGraph()
        }
        bindMetricSpinner()
        loadGraph()
    }

    private fun bindMetricSpinner() {
        val sportId = session().sportId
        metricFields = SportMetrics.fields(sportId)
        if (selectedMetric.isBlank() || metricFields.none { it.key == selectedMetric }) {
            selectedMetric = SportMetrics.primaryKey(sportId)
        }
        val labels = metricFields.map { getString(it.labelRes) }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
        binding.metricSpinner.adapter = adapter
        val index = metricFields.indexOfFirst { it.key == selectedMetric }.coerceAtLeast(0)
        spinnerReady = false
        binding.metricSpinner.setSelection(index, false)
        binding.metricSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val key = metricFields.getOrNull(position)?.key ?: return
                if (!spinnerReady) {
                    spinnerReady = true
                    return
                }
                if (key == selectedMetric) return
                selectedMetric = key
                loadGraph()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        spinnerReady = true
    }

    private fun loadGraph() {
        val screen = _binding ?: return
        val sportId = session().sportId
        val year = visibleMonth.get(Calendar.YEAR)
        val month = visibleMonth.get(Calendar.MONTH) + 1

        screen.monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(visibleMonth.time)

        graphCall?.cancel()
        screen.barChart.setData(emptyList())
        screen.graphCaption.text = getString(R.string.graph_loading)
        setNavigationEnabled(false)

        val call = ApiClient.service.getMonthlyPerformance(
            sportId,
            year,
            month,
            selectedMetric.ifBlank { null }
        )
        graphCall = call
        call.enqueue(object : Callback<MonthlyPerformanceDto> {
            override fun onResponse(
                call: Call<MonthlyPerformanceDto>,
                response: Response<MonthlyPerformanceDto>
            ) {
                if (_binding !== screen) return
                setNavigationEnabled(true)
                val graph = response.body()
                if (!response.isSuccessful || graph == null) {
                    screen.graphCaption.text = getString(R.string.graph_load_error)
                    Log.i("Performance", "load fail graph sport=$sportId")
                    return
                }
                val points = graph.points.map { point ->
                    point.day.toString() to point.value
                }
                screen.barChart.setData(points)
                screen.graphCaption.text = if (points.isEmpty()) {
                    getString(R.string.empty_month_graph)
                } else {
                    getString(R.string.graph_metric_by_date, graph.metricLabel)
                }
                val pointSummary = graph.points.joinToString(". ") { point ->
                    getString(R.string.graph_day_point, point.day, point.value.toString())
                }
                screen.barChart.contentDescription = getString(
                    R.string.graph_day_content,
                    graph.metricLabel,
                    screen.monthLabel.text,
                    pointSummary
                )
                Log.i(
                    "Performance",
                    "graph sport=$sportId metric=${graph.metric} points=${points.size}"
                )
            }

            override fun onFailure(call: Call<MonthlyPerformanceDto>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                setNavigationEnabled(true)
                screen.barChart.setData(emptyList())
                screen.graphCaption.text = getString(R.string.graph_connect_error)
                Log.i("Performance", "load fail graph sport=$sportId")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.graph_check_api),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun setNavigationEnabled(enabled: Boolean) {
        _binding?.apply {
            btnPrevMonth.isEnabled = enabled
            btnNextMonth.isEnabled = enabled
            metricSpinner.isEnabled = enabled
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(KEY_VISIBLE_MONTH, visibleMonth.timeInMillis)
        outState.putString(KEY_METRIC, selectedMetric)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        graphCall?.cancel()
        graphCall = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val KEY_VISIBLE_MONTH = "visible_month"
        const val KEY_METRIC = "graph_metric"
    }
}
