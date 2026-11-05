package com.example.prog7314_part2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphPointsTest {

    @Test
    fun emptyMonthIsEmptyList() {
        assertTrue(GraphPoints.forChart(emptyList()).isEmpty())
    }

    @Test
    fun dropsNonFiniteAndNegative() {
        val kept = GraphPoints.forChart(
            listOf(
                "1" to 3.0,
                "2" to Double.NaN,
                "3" to -1.0,
                "4" to Double.POSITIVE_INFINITY,
                "5" to 0.0
            )
        )
        assertEquals(listOf("1" to 3.0, "5" to 0.0), kept)
    }
}
