package com.example.prog7314_part2.data

import com.example.prog7314_part2.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SportMetricsTest {

    @Test
    fun basketballFields() {
        val keys = SportMetrics.fields("basketball").map { it.key }
        assertEquals(listOf("points", "rebounds"), keys)
        assertEquals("points", SportMetrics.primaryKey("basketball"))
    }

    @Test
    fun swimmingFieldsDifferFromBasketball() {
        val swimming = SportMetrics.fields("swimming").map { it.key }
        val basketball = SportMetrics.fields("basketball").map { it.key }
        assertEquals(listOf("distance", "time"), swimming)
        assertNotEquals(swimming, basketball)
        assertFalse(swimming.contains("score"))
        assertFalse(basketball.contains("score"))
    }

    @Test
    fun footballAndAthleticsKeepApiKeys() {
        assertEquals(listOf("goals", "assists"), SportMetrics.fields("football").map { it.key })
        assertEquals(listOf("distance", "time"), SportMetrics.fields("athletics").map { it.key })
        assertEquals("distance", SportMetrics.primaryKey("swimming"))
    }

    @Test
    fun cricketAndTennisStaySportAware() {
        assertEquals(listOf("runs", "wickets"), SportMetrics.fields("cricket").map { it.key })
        assertEquals(listOf("aces", "winners"), SportMetrics.fields("tennis").map { it.key })
        assertEquals("runs", SportMetrics.primaryKey("cricket"))
        assertEquals("aces", SportMetrics.primaryKey("tennis"))
    }

    @Test
    fun summaryUsesLabels() {
        val line = SportMetrics.summary("swimming", mapOf("distance" to 1500.0, "time" to 1260.0))
        assertEquals("1500 distance (m)  ·  1260 time (s)", line)
    }

    @Test
    fun labelResMatchesStringsXml() {
        assertEquals(R.string.metric_points, SportMetrics.labelRes("points"))
        assertEquals(R.string.metric_distance, SportMetrics.labelRes("distance"))
        assertEquals(R.string.metric_goals, SportMetrics.fields("football").first().labelRes)
    }
}
