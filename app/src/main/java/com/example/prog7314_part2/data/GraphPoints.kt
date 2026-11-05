package com.example.prog7314_part2.data

/**
 * Sanitises monthly-graph points before [com.example.prog7314_part2.ui.performance.SimpleBarChartView]
 * draws them. An empty month must produce an empty list, never a crash.
 */
object GraphPoints {
    fun forChart(values: List<Pair<String, Double>>): List<Pair<String, Double>> =
        values.filter { it.second.isFinite() && it.second >= 0.0 }
}
