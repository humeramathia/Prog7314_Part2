package com.example.prog7314_part2.data

import java.util.Calendar
import java.util.UUID

/**
 * In-memory fallback dataset used when the hosted API is unreachable.
 *
 * The Sport Select screen calls into [sports] when `GET /api/sports`
 * fails so the picker is never a blank screen. The rest of the maps
 * (events, sessions, guides) were kept from an earlier prototype and
 * are only exercised by the unit tests in `FakeRepositoryTest`.
 *
 * The IDs here must match `api/src/data/catalog.js` so any local
 * `sportId` written to preferences is still valid when the API comes
 * back online.
 */
object FakeRepository {

    val sports = listOf(
        Sport("football", "Football"),
        Sport("cricket", "Cricket"),
        Sport("tennis", "Tennis"),
        Sport("basketball", "Basketball"),
        Sport("swimming", "Swimming"),
        Sport("athletics", "Athletics")
    )

    private val events = mutableListOf<CalendarEvent>()
    private val sessions = mutableListOf<PerformanceSession>()
    private val guides = mutableListOf<LearnGuide>()

    init {
        seed()
    }

    /** Every event for [sportId] in ascending start-time order. */
    fun eventsForSport(sportId: String): List<CalendarEvent> =
        events.filter { it.sportId == sportId }.sortedBy { it.startsAt }

    fun eventById(id: String): CalendarEvent? = events.find { it.id == id }

    /**
     * The next upcoming event, or — if all seeded events are in the past —
     * the most recent one. Keeps the Home tile populated during demos.
     */
    fun nextEvent(sportId: String): CalendarEvent? {
        val now = System.currentTimeMillis()
        return eventsForSport(sportId).firstOrNull { it.startsAt >= now }
            ?: eventsForSport(sportId).lastOrNull()
    }

    /** Sessions for a sport, newest first. */
    fun sessionsForSport(sportId: String): List<PerformanceSession> =
        sessions.filter { it.sportId == sportId }
            .sortedByDescending { it.recordedAt }

    fun latestSession(sportId: String): PerformanceSession? =
        sessionsForSport(sportId).firstOrNull()

    /**
     * Appends a session to the in-memory list. Used by exploratory tests
     * — no production code path writes into this object.
     */
    fun addSession(
        sportId: String,
        metrics: Map<String, Double>,
        notes: String,
        recordedAt: Long = System.currentTimeMillis()
    ) {
        sessions.add(
            0,
            PerformanceSession(
                id = UUID.randomUUID().toString(),
                sportId = sportId,
                recordedAt = recordedAt,
                metrics = metrics,
                notes = notes
            )
        )
    }

    /**
     * Points for the monthly graph fallback: `(dayLabel, value)` pairs for
     * every session in [year]/[month] that has a [metricKey] value.
     */
    fun monthSeries(
        sportId: String,
        year: Int,
        month: Int,
        metricKey: String = SportMetrics.primaryKey(sportId)
    ): List<Pair<String, Float>> {
        return sessionsForSport(sportId)
            .filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.recordedAt }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }
            .sortedBy { it.recordedAt }
            .mapNotNull { session ->
                val value = session.metrics[metricKey] ?: return@mapNotNull null
                val day = Calendar.getInstance()
                    .apply { timeInMillis = session.recordedAt }
                    .get(Calendar.DAY_OF_MONTH)
                day.toString() to value.toFloat()
            }
    }

    /**
     * Learn guides for [sportId], optionally narrowed to a single
     * [category]. Returns an empty list when nothing matches.
     */
    fun guidesFor(sportId: String, category: LearnCategory?): List<LearnGuide> {
        val filtered = guides.filter { it.sportId == sportId }
        return if (category == null) filtered else filtered.filter { it.category == category }
    }

    fun guideById(id: String): LearnGuide? = guides.find { it.id == id }

    /**
     * Populates one practice, one social event, one announcement, two
     * sessions, and four learn guides per sport. Times are offset from
     * "now" so demos always show a mix of past and future dates.
     */
    private fun seed() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        sports.forEachIndexed { index, sport ->
            events += CalendarEvent(
                id = "${sport.id}-practice",
                sportId = sport.id,
                title = "${sport.name} squad practice",
                type = EventType.PRACTICE,
                startsAt = now + day * (2 + index),
                location = "Campus courts",
                notes = "Bring kit and water. Warm-up starts 15 minutes early."
            )
            events += CalendarEvent(
                id = "${sport.id}-event",
                sportId = sport.id,
                title = "${sport.name} inter-campus fixture",
                type = EventType.SOCIAL_EVENT,
                startsAt = now + day * (9 + index),
                endsAt = now + day * (9 + index) + 10_800_000L,
                location = "Main stadium",
                description = "Friendly fixture against another campus side.",
                notes = "Arrive 45 minutes before start. Team photo at the gate."
            )
            events += CalendarEvent(
                id = "${sport.id}-announcement",
                sportId = sport.id,
                title = "${sport.name} kit collection",
                type = EventType.ANNOUNCEMENT,
                startsAt = now + day * (4 + index),
                location = "Clubhouse",
                description = "Collect numbered kit and confirm your availability.",
                notes = "Bring student card."
            )
            sessions += PerformanceSession(
                id = "${sport.id}-s1",
                sportId = sport.id,
                recordedAt = now - day * 3,
                metrics = SportMetrics.sample(sport.id, 72.0 + index),
                notes = "Solid session."
            )
            sessions += PerformanceSession(
                id = "${sport.id}-s2",
                sportId = sport.id,
                recordedAt = now - day * 18,
                metrics = SportMetrics.sample(sport.id, 64.0 + index),
                notes = "Need more recovery work."
            )
            LearnCategory.entries.forEach { category ->
                guides += LearnGuide(
                    id = "${sport.id}-${category.name.lowercase()}",
                    sportId = sport.id,
                    category = category,
                    title = "${sport.name} ${category.label()}",
                    body = beginnerBody(sport.name, category)
                )
            }
        }
    }

    /** Boiler-plate beginner tips per category. Kept short for the tile UI. */
    private fun beginnerBody(sport: String, category: LearnCategory): String = when (category) {
        LearnCategory.RULES ->
            "$sport beginners should learn the scoring system, playing area, and basic fouls first. " +
                "Know when play starts and stops, and what counts as a legal action."
        LearnCategory.TECHNIQUES ->
            "Start with stance, grip, and a repeatable movement. " +
                "Practice slowly, then add speed once the pattern feels consistent."
        LearnCategory.TRAINING ->
            "Train twice a week: one skill session and one fitness session. " +
                "Keep a short log of what you practised and how it felt."
        LearnCategory.SAFETY ->
            "Warm up, wear the right kit, and stop if you feel sharp pain. " +
                "Hydrate, check the surface, and tell a coach about any injury."
    }
}

/** Lower-case display label for a category (used in seeded titles). */
fun LearnCategory.label(): String = when (this) {
    LearnCategory.RULES -> "rules"
    LearnCategory.TECHNIQUES -> "techniques"
    LearnCategory.TRAINING -> "training"
    LearnCategory.SAFETY -> "safety"
}
