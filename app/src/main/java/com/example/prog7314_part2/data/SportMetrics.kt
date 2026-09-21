package com.example.prog7314_part2.data

/**
 * A single input row on the "Add performance" screen.
 *
 * @property key   Server-side metric identifier (e.g. `points`, `distance`).
 * @property label Localised label shown as the field hint.
 */
data class MetricField(
    val key: String,
    val label: String
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
            MetricField("goals", "Goals"),
            MetricField("assists", "Assists")
        )
        "cricket" -> listOf(
            MetricField("runs", "Runs"),
            MetricField("wickets", "Wickets")
        )
        "tennis" -> listOf(
            MetricField("aces", "Aces"),
            MetricField("winners", "Winners")
        )
        "basketball" -> listOf(
            MetricField("points", "Points"),
            MetricField("rebounds", "Rebounds")
        )
        // Swimming and athletics share the same two metric shape.
        "swimming", "athletics" -> listOf(
            MetricField("distance", "Distance (m)"),
            MetricField("time", "Time (s)")
        )
        else -> listOf(MetricField("value", "Value"))
    }

    /** Key of the metric that drives the monthly graph by default. */
    fun primaryKey(sportId: String): String = fields(sportId).first().key

    /**
     * Builds a plausible fake metric map for the seeded sample sessions
     * used by [FakeRepository]. Never called on real user data.
     *
     * The primary metric gets [base] verbatim; secondary metrics get a
     * rounded third of it so the numbers look coherent (e.g. 72 points
     * with 24 rebounds).
     */
    fun sample(sportId: String, base: Double): Map<String, Double> {
        val keys = fields(sportId)
        return keys.mapIndexed { index, field ->
            field.key to if (index == 0) {
                base
            } else {
                (base / 3).toInt().toDouble()
            }
        }.toMap()
    }

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
