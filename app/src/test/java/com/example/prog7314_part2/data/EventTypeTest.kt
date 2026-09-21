package com.example.prog7314_part2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTypeTest {

    @Test
    fun calendarTypesMatchApi() {
        assertEquals(
            listOf("PRACTICE", "SOCIAL_EVENT", "ANNOUNCEMENT"),
            EventType.entries.map { it.name }
        )
    }
}
