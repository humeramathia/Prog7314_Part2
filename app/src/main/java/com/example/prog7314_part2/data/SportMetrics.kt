package com.example.prog7314_part2.data

import com.example.prog7314_part2.R

/**
 * A single input row on the "Add performance" screen.
 *
 * @property key   Server-side metric identifier (e.g. `points`, `distance`).
 * @property label English fallback that must match `api/src/data/metrics.js`.
 * @property labelRes String resource Lilitha can translate.
 */
data class MetricField(
    val key: String,
    val label: String,
    val labelRes: Int
)

/**
 * Client-side mirror of the sport → metric schema declared in the API's
 * `api/src/data/metrics.js`. Keeping a local copy lets the app render
 * the correct input fields even when the API is asleep on Render, and
 * gives unit tests something deterministic to assert against.
 *
 * The keys and order **must** match the server; otherwise a POST to
 * `/api/performance` will fail with `metrics.<key> is required`.
 */
object SportMetrics {

    /**
     * Returns the ordered list of metric fields for a sport. The first
     * entry is treated as the "primary" metric by [primaryKey] and by the
     * monthly graph endpoint on the server.
     *
     * Unknown sports fall back to a single generic "Value" field so the
     * screen still functions in a demo build with an unseeded catalog.
     */
    fun fields(sportId: String): List<MetricField> = when (sportId) {
        "football" -> listOf(
            MetricField("goals", "Goals", R.string.metric_goals),
            MetricField("assists", "Assists", R.string.metric_assists)
        )
        "cricket" -> listOf(
            MetricField("runs", "Runs", R.string.metric_runs),
            MetricField("wickets", "Wickets", R.string.metric_wickets)
        )
        "tennis" -> listOf(
            MetricField("aces", "Aces", R.string.metric_aces),
            MetricField("winners", "Winners", R.string.metric_winners)
        )
        "basketball" -> listOf(
            MetricField("points", "Points", R.string.metric_points),
            MetricField("rebounds", "Rebounds", R.string.metric_rebounds)
        )
        // Swimming and athletics share the same two metric shape.
        "swimming", "athletics" -> listOf(
            MetricField("distance", "Distance (m)", R.string.metric_distance),
            MetricField("time", "Time (s)", R.string.metric_time)
        )
        else -> listOf(MetricField("value", "Value", R.string.metric_value))
    }

    /** String resource for a metric key so the UI never hardcodes English. */
    fun labelRes(key: String): Int = when (key) {
        "goals" -> R.string.metric_goals
        "assists" -> R.string.metric_assists
        "runs" -> R.string.metric_runs
        "wickets" -> R.string.metric_wickets
        "aces" -> R.string.metric_aces
        "winners" -> R.string.metric_winners
        "points" -> R.string.metric_points
        "rebounds" -> R.string.metric_rebounds
        "distance" -> R.string.metric_distance
        "time" -> R.string.metric_time
        else -> R.string.metric_value
    }

    /** Key of the metric that drives the monthly graph by default. */
    fun primaryKey(sportId: String): String = fields(sportId).first().key

    /**
     * Renders a compact one-line summary of a session's metrics, used by
     * the Home tile and the Performance history list.
     *
     * Example: `"22 points  ·  8 rebounds"`.
     * Integer values are formatted without a trailing `.0`.
     */
    fun summary(sportId: String, metrics: Map<String, Double>): String {
        return fields(sportId).joinToString("  ·  ") { field ->
            val value = metrics[field.key] ?: return@joinToString field.label
            val rendered = if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                value.toString()
            }
            "$rendered ${field.label.lowercase()}"
        }
    }
}
