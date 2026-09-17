package com.example.prog7314_part2.data

data class MetricField(
    val key: String,
    val label: String
)

object SportMetrics {

    fun fields(sportId: String): List<MetricField> = when (sportId) {
        "football" -> listOf(MetricField("goals", "Goals"), MetricField("assists", "Assists"))
        "cricket" -> listOf(MetricField("runs", "Runs"), MetricField("wickets", "Wickets"))
        "tennis" -> listOf(MetricField("aces", "Aces"), MetricField("winners", "Winners"))
        "basketball" -> listOf(MetricField("points", "Points"), MetricField("rebounds", "Rebounds"))
        "swimming", "athletics" -> listOf(
            MetricField("distance", "Distance (m)"),
            MetricField("time", "Time (s)")
        )
        else -> listOf(MetricField("value", "Value"))
    }

    fun primaryKey(sportId: String): String = fields(sportId).first().key

    fun sample(sportId: String, base: Double): Map<String, Double> {
        val keys = fields(sportId)
        return keys.mapIndexed { index, field ->
            field.key to if (index == 0) base else (base / 3).toInt().toDouble()
        }.toMap()
    }

    fun summary(sportId: String, metrics: Map<String, Double>): String {
        return fields(sportId).joinToString("  ·  ") { field ->
            val value = metrics[field.key] ?: return@joinToString field.label
            val rendered = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            "$rendered ${field.label.lowercase()}"
        }
    }
}
