package com.example.prog7314_part2.data.local

import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import org.junit.Assert.*
import org.junit.Test

class SessionEntityTest {
    private fun pending() = SessionEntity(
        userId = "athlete-a", localId = "fixed-uuid", sportId = "swimming",
        recordedAt = 123L, notes = "Practice", metricsJson = "{\"distance\":800.0,\"time\":900.0}",
        updatedAt = 124L
    )

    @Test fun `new sessions remain pending without a server id`() {
        val row = pending()
        assertNull(row.serverId)
        assertEquals(SyncPolicy.PENDING, row.toLocal().syncState)
    }

    @Test fun `upload retries keep the same client UUID and complete payload`() {
        val row = pending()
        val first = row.toRequest()
        assertEquals(first, row.toRequest())
        assertEquals(row.localId, first.clientId)
        assertEquals(mapOf("distance" to 800.0, "time" to 900.0), first.metrics)
        assertEquals(123L, first.recordedAt)
        assertEquals("Practice", first.notes)
    }

    @Test fun `downloaded sessions are acknowledged and retain their owner`() {
        val dto = PerformanceSessionDto("server-id", "athlete-b", "swimming", 123L,
            "Practice", mapOf("distance" to 800.0), "distance", 800.0)
        val row = SessionEntity.fromRemote(dto)
        assertEquals("athlete-b", row.userId)
        assertEquals("server-id", row.serverId)
        assertEquals(SyncPolicy.SYNCED, row.toLocal().syncState)
    }

    @Test fun `input rejection stops automatic retries`() {
        assertTrue(SyncPolicy.isPermanentFailure(400))
        assertTrue(SyncPolicy.isPermanentFailure(409))
        assertTrue(SyncPolicy.isPermanentFailure(422))
    }

    @Test fun `auth failures keep the queue for another sign in`() {
        assertFalse(SyncPolicy.isPermanentFailure(401))
        assertFalse(SyncPolicy.isPermanentFailure(403))
    }

    @Test fun `timeouts throttling and server outages must retry`() {
        listOf(408, 425, 429, 500, 502, 503, 504).forEach {
            assertFalse("HTTP $it must retain pending data", SyncPolicy.isPermanentFailure(it))
        }
    }
}
