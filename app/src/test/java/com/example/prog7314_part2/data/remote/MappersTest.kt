package com.example.prog7314_part2.data.remote

import com.example.prog7314_part2.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    private fun event(type: String) = CalendarEventDto(
        id = "e1",
        sportId = "football",
        title = "Session",
        type = type,
        startsAt = 1_000L,
        endsAt = 2_000L,
        location = "Pitch",
        description = "Warm-up",
        notes = "Boots"
    )

    @Test
    fun mapsAnnouncement() {
        assertEquals(EventType.ANNOUNCEMENT, event("ANNOUNCEMENT").toLocal().type)
    }

    @Test
    fun mapsLegacyEventAsSocial() {
        assertEquals(EventType.SOCIAL_EVENT, event("EVENT").toLocal().type)
        assertEquals(EventType.SOCIAL_EVENT, event("SOCIAL_EVENT").toLocal().type)
    }

    @Test
    fun mapsPracticeAndCopiesTimes() {
        val local = event("PRACTICE").toLocal()
        assertEquals(EventType.PRACTICE, local.type)
        assertEquals(1_000L, local.startsAt)
        assertEquals(2_000L, local.endsAt)
        assertEquals("Warm-up", local.description)
    }
}
