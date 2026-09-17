package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.R
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
    private var graphCall: Call<MonthlyPerformanceDto>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            visibleMonth.timeInMillis =
                savedInstanceState.getLong(
                    KEY_VISIBLE_MONTH,
                    System.currentTimeMillis()
                )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyGraphBinding.inflate(
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
        binding.btnPrevMonth.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, -1)
            loadGraph()
        }

        binding.btnNextMonth.setOnClickListener {
            visibleMonth.add(Calendar.MONTH, 1)
            loadGraph()
        }

        loadGraph()
    }

    private fun loadGraph() {
        val screen = _binding ?: return
        val sportId = session().sportId

        val year = visibleMonth.get(Calendar.YEAR)

        // Calendar months are 0–11, but the API expects 1–12.
        val month = visibleMonth.get(Calendar.MONTH) + 1

        screen.monthLabel.text = SimpleDateFormat(
            "MMMM yyyy",
            Locale.getDefault()
        ).format(visibleMonth.time)

        graphCall?.cancel()
        screen.barChart.setData(emptyList())
        screen.graphCaption.text = "Loading graph…"
        setNavigationEnabled(false)

        val call = ApiClient.service.getMonthlyPerformance(
            sportId = sportId,
            year = year,
            month = month
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
                    screen.graphCaption.text =
                        "Could not load the monthly graph."
                    return
                }

                val points = graph.points.map { point ->
                    point.day.toString() to point.value
                }

                screen.barChart.setData(points)

                screen.graphCaption.text =
                    if (points.isEmpty()) {
                        getString(R.string.empty_month_graph)
                    } else {
                        "${graph.metricLabel} by date"
                    }

                screen.barChart.contentDescription =
                    "${graph.metricLabel} for " +
                            screen.monthLabel.text + ". " +
                            graph.points.joinToString(". ") { point ->
                                "Day ${point.day}: ${point.value}"
                            }
            }

            override fun onFailure(
                call: Call<MonthlyPerformanceDto>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return

                setNavigationEnabled(true)
                screen.graphCaption.text =
                    "Could not connect to the API."

                Toast.makeText(
                    requireContext(),
                    "Check that the API is running.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun setNavigationEnabled(enabled: Boolean) {
        _binding?.apply {
            btnPrevMonth.isEnabled = enabled
            btnNextMonth.isEnabled = enabled
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(
            KEY_VISIBLE_MONTH,
            visibleMonth.timeInMillis
        )
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
    }
}