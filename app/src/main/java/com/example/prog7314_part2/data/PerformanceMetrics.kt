package com.example.prog7314_part2.data

data class PerformanceMetric(
    val key: String,
    val label: String,
    val wholeNumber: Boolean = false,
    val allowZero: Boolean = true
)

object PerformanceMetrics {

    fun forSport(sportId: String): List<PerformanceMetric> =
        when (sportId) {
            "swimming" -> listOf(
                PerformanceMetric(
                    "distanceMetres", "Distance (metres)",
                    allowZero = false
                ),
                PerformanceMetric(
                    "timeSeconds", "Time (seconds)",
                    allowZero = false
                )
            )

            "basketball" -> listOf(
                PerformanceMetric("points", "Points", wholeNumber = true),
                PerformanceMetric("rebounds", "Rebounds", wholeNumber = true)
            )

            "football" -> listOf(
                PerformanceMetric("goals", "Goals", wholeNumber = true),
                PerformanceMetric("assists", "Assists", wholeNumber = true)
            )

            "cricket" -> listOf(
                PerformanceMetric("runs", "Runs", wholeNumber = true),
                PerformanceMetric("wickets", "Wickets", wholeNumber = true)
            )

            "tennis" -> listOf(
                PerformanceMetric(
                    "gamesWon", "Games won", wholeNumber = true
                ),
                PerformanceMetric(
                    "aces", "Aces", wholeNumber = true
                )
            )

            "athletics" -> listOf(
                PerformanceMetric(
                    "distanceMetres", "Distance (metres)",
                    allowZero = false
                ),
                PerformanceMetric(
                    "timeSeconds", "Time (seconds)",
                    allowZero = false
                )
            )

            else -> emptyList()
        }
}