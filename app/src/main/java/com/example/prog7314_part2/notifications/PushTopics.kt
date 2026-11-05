package com.example.prog7314_part2.notifications

/**
 * FCM topic naming shared with the API (`topicForSport` in `api/src/notify.js`).
 *
 * Each sport has one topic. When a fixture is added or edited, the API
 * sends one message to that topic and Firebase fans it out to every phone
 * subscribed to it, so the server never has to look up individual users.
 *
 * Topic names may only contain `[a-zA-Z0-9-_.~%]`, so anything else is
 * replaced with an underscore. Keep this in step with the API.
 *
 * Reference: Google (2025) *Topic messaging on Android*, Firebase Cloud
 * Messaging documentation.
 * Available at: https://firebase.google.com/docs/cloud-messaging/android/topic-messaging
 */
object PushTopics {

    private const val PREFIX = "sport_"
    private val NOT_ALLOWED = Regex("[^a-z0-9_.~-]")

    /** `football` → `sport_football`. Blank sport → empty string (no topic). */
    fun forSport(sportId: String?): String {
        val safe = sportId.orEmpty().trim().lowercase().replace(NOT_ALLOWED, "_")
        return if (safe.isEmpty()) "" else PREFIX + safe
    }

    /**
     * Works out which topic subscriptions need to change.
     *
     * @param current topic the phone is subscribed to now ("" for none)
     * @param wanted topic it should be subscribed to ("" for none)
     * @return pair of (topic to unsubscribe from, topic to subscribe to);
     *         either side is null when nothing needs doing.
     */
    fun plan(current: String, wanted: String): Pair<String?, String?> {
        if (current == wanted) return null to null
        val leave = current.takeIf { it.isNotBlank() }
        val join = wanted.takeIf { it.isNotBlank() }
        return leave to join
    }
}
