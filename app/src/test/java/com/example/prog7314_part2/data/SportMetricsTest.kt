package com.example.prog7314_part2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SportMetricsTest {

    @Test
    fun basketballFields() {
        val keys = SportMetrics.fields("basketball").map { it.key }
        assertEquals(listOf("points", "rebounds"), keys)
        assertEquals("points", SportMetrics.primaryKey("basketball"))
    }

    @Test
    fun summaryUsesLabels() {
        val line = SportMetrics.summary("swimming", mapOf("distance" to 1500.0, "time" to 1260.0))
        assertEquals("1500 distance (m)  ·  1260 time (s)", line)
    }
}
