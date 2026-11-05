package com.example.prog7314_part2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceHistoryTest {

    @Test
    fun pendingAppearsImmediatelyNewestFirst() {
        val remote = listOf(session("a", recordedAt = 10L, pending = false))
        val pending = listOf(session("p", recordedAt = 20L, pending = true))
        val merged = PerformanceHistory.merge(remote, pending)
        assertEquals(listOf("p", "a"), merged.map { it.id })
        assertTrue(merged.first().pending)
    }

    @Test
    fun syncedIdDropsOutOfPending() {
        val remote = listOf(session("p", recordedAt = 20L, pending = false))
        val pending = listOf(session("p", recordedAt = 20L, pending = true))
        val merged = PerformanceHistory.merge(remote, pending)
        assertEquals(1, merged.size)
        assertFalse(merged.first().pending)
    }

    @Test
    fun graphExcludesPending() {
        val remote = listOf(
            session("a", recordedAt = 1L, pending = false),
            session("p", recordedAt = 2L, pending = true)
        )
        assertEquals(listOf("a"), PerformanceHistory.graphSessions(remote).map { it.id })
    }

    @Test
    fun latestPrefersPendingWhenNewer() {
        val latest = PerformanceHistory.latest(
            listOf(session("a", recordedAt = 10L)),
            listOf(session("p", recordedAt = 99L, pending = true))
        )
        assertEquals("p", latest?.id)
    }

    private fun session(
        id: String,
        recordedAt: Long,
        pending: Boolean = false
    ) = PerformanceSession(
        id = id,
        sportId = "basketball",
        recordedAt = recordedAt,
        metrics = mapOf("points" to 12.0),
        notes = "",
        pending = pending
    )
}
