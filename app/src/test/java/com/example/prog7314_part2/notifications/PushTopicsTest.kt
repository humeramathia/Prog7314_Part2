package com.example.prog7314_part2.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Android topic names must match `topicForSport` in `api/src/notify.js`,
 * otherwise the API would push to a topic no phone is subscribed to.
 */
class PushTopicsTest {

    @Test
    fun forType_matchesApiSuffixes() {
        assertEquals("sport_football_practice", PushTopics.forType("football", "PRACTICE"))
        assertEquals("sport_football_event", PushTopics.forType("football", "SOCIAL_EVENT"))
        assertEquals("sport_football_announcement", PushTopics.forType("football", "ANNOUNCEMENT"))
    }

    @Test
    fun forType_matchesApiSanitising() {
        assertEquals(
            "sport_ultimate_frisbee__practice",
            PushTopics.forType(" Ultimate Frisbee! ", "PRACTICE")
        )
    }

    @Test
    fun forType_blankSportHasNoTopic() {
        assertEquals("", PushTopics.forType("", "PRACTICE"))
        assertEquals("", PushTopics.forType(null, "PRACTICE"))
    }

    @Test
    fun wanted_offMeansNoTopics() {
        assertTrue(
            PushTopics.wanted("football", enabled = false, practice = true, events = true, announcements = true)
                .isEmpty()
        )
    }

    @Test
    fun wanted_onlyEnabledTypes() {
        val topics = PushTopics.wanted(
            "football",
            enabled = true,
            practice = true,
            events = false,
            announcements = true
        )
        assertEquals(
            setOf("sport_football_practice", "sport_football_announcement"),
            topics
        )
    }

    @Test
    fun plan_leavesOldAndJoinsNew() {
        val (leave, join) = PushTopics.plan(
            setOf("sport_football_practice", "sport_football_event"),
            setOf("sport_football_event", "sport_football_announcement")
        )
        assertEquals(setOf("sport_football_practice"), leave)
        assertEquals(setOf("sport_football_announcement"), join)
    }
}
