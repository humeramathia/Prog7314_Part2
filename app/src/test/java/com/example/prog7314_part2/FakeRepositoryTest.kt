package com.example.prog7314_part2

import com.example.prog7314_part2.data.FakeRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRepositoryTest {
    @Test
    fun sportsAreSeeded() {
        assertTrue(FakeRepository.sports.size >= 6)
    }

    @Test
    fun footballHasEventsAndGuides() {
        assertTrue(FakeRepository.eventsForSport("football").isNotEmpty())
        assertTrue(FakeRepository.guidesFor("football", null).isNotEmpty())
    }

    @Test
    fun footballSeedIncludesSocialAndAnnouncement() {
        val types = FakeRepository.eventsForSport("football").map { it.type }.toSet()
        assertTrue(types.contains(com.example.prog7314_part2.data.EventType.SOCIAL_EVENT))
        assertTrue(types.contains(com.example.prog7314_part2.data.EventType.ANNOUNCEMENT))
    }
}
