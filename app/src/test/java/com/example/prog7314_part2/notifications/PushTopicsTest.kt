package com.example.prog7314_part2.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Android topic names must match `topicForSport` in `api/src/notify.js`,
 * otherwise the API would push to a topic no phone is subscribed to.
 */
class PushTopicsTest {

    @Test
    fun forSport_prefixesTheSportId() {
        assertEquals("sport_football", PushTopics.forSport("football"))
    }

    @Test
    fun forSport_matchesApiSanitising() {
        // Same input and expected value as api/tests/notify.test.js.
        assertEquals("sport_ultimate_frisbee_", PushTopics.forSport(" Ultimate Frisbee! "))
    }

    @Test
    fun forSport_blankSportHasNoTopic() {
        assertEquals("", PushTopics.forSport(""))
        assertEquals("", PushTopics.forSport(null))
    }

    @Test
    fun plan_sameTopicDoesNothing() {
        val (leave, join) = PushTopics.plan("sport_football", "sport_football")
        assertNull(leave)
        assertNull(join)
    }

    @Test
    fun plan_changingSportLeavesOldAndJoinsNew() {
        val (leave, join) = PushTopics.plan("sport_football", "sport_archery")
        assertEquals("sport_football", leave)
        assertEquals("sport_archery", join)
    }

    @Test
    fun plan_firstSubscriptionOnlyJoins() {
        val (leave, join) = PushTopics.plan("", "sport_netball")
        assertNull(leave)
        assertEquals("sport_netball", join)
    }

    @Test
    fun plan_switchingOffOnlyLeaves() {
        val (leave, join) = PushTopics.plan("sport_netball", "")
        assertEquals("sport_netball", leave)
        assertNull(join)
    }
}
