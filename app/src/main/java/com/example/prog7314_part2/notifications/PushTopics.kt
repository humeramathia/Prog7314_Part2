package com.example.prog7314_part2.notifications

/**
 * FCM topic naming shared with the API (`topicForSport` in `api/src/notify.js`).
 *
 * Each sport has one topic per fixture type. The API pushes to that topic
 * and Firebase fans it out only to phones that subscribed to the type,
 * so a practice reminder never reaches a user who only wants announcements.
 *
 * Topic names may only contain `[a-zA-Z0-9-_.~%]`, so anything else is
 * replaced with an underscore. Keep this in step with the API.
 *
 * Reference: Google (2025) *Topic messaging on Android*, Firebase Cloud
 * Messaging documentation.
 * Available at: https://firebase.google.com/docs/cloud-messaging/android/topic-messaging
 */
object PushTopics {

    const val PRACTICE = "PRACTICE"
    const val SOCIAL_EVENT = "SOCIAL_EVENT"
    const val ANNOUNCEMENT = "ANNOUNCEMENT"

    private const val PREFIX = "sport_"
    private val NOT_ALLOWED = Regex("[^a-z0-9_.~-]")

    /** `football` + `PRACTICE` → `sport_football_practice`. Blank sport → empty. */
    fun forType(sportId: String?, type: String?): String {
        val safe = sportId.orEmpty().trim().lowercase().replace(NOT_ALLOWED, "_")
        if (safe.isEmpty()) return ""
        val suffix = when (type.orEmpty().uppercase()) {
            PRACTICE -> "practice"
            ANNOUNCEMENT -> "announcement"
            else -> "event"
        }
        return PREFIX + safe + "_" + suffix
    }

    /** Topics this phone should be on, given the Settings switches. */
    fun wanted(
        sportId: String?,
        enabled: Boolean,
        practice: Boolean,
        events: Boolean,
        announcements: Boolean
    ): Set<String> {
        if (!enabled) return emptySet()
        return buildSet {
            if (practice) forType(sportId, PRACTICE).takeIf { it.isNotEmpty() }?.let(::add)
            if (events) forType(sportId, SOCIAL_EVENT).takeIf { it.isNotEmpty() }?.let(::add)
            if (announcements) forType(sportId, ANNOUNCEMENT).takeIf { it.isNotEmpty() }?.let(::add)
        }
    }

    /**
     * Works out which topic subscriptions need to change.
     *
     * @return pair of (topics to leave, topics to join)
     */
    fun plan(current: Set<String>, wanted: Set<String>): Pair<Set<String>, Set<String>> =
        (current - wanted) to (wanted - current)
}
