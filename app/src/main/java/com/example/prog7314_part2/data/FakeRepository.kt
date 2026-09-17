package com.example.prog7314_part2.data

import java.util.Calendar
import java.util.UUID

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

    fun eventsForSport(sportId: String): List<CalendarEvent> =
        events.filter { it.sportId == sportId }.sortedBy { it.startsAt }

    fun eventById(id: String): CalendarEvent? = events.find { it.id == id }

    fun nextEvent(sportId: String): CalendarEvent? {
        val now = System.currentTimeMillis()
        return eventsForSport(sportId).firstOrNull { it.startsAt >= now }
            ?: eventsForSport(sportId).lastOrNull()
    }

    fun sessionsForSport(sportId: String): List<PerformanceSession> =
        sessions.filter { it.sportId == sportId }.sortedByDescending { it.recordedAt }

    fun latestSession(sportId: String): PerformanceSession? =
        sessionsForSport(sportId).firstOrNull()

    fun addSession(sportId: String, metrics: Map<String, Double>, notes: String, recordedAt: Long = System.currentTimeMillis()) {
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

    fun monthSeries(sportId: String, year: Int, month: Int, metricKey: String = SportMetrics.primaryKey(sportId)): List<Pair<String, Float>> {
        return sessionsForSport(sportId)
            .filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.recordedAt }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }
            .sortedBy { it.recordedAt }
            .mapNotNull { session ->
                val value = session.metrics[metricKey] ?: return@mapNotNull null
                val day = Calendar.getInstance().apply { timeInMillis = session.recordedAt }.get(Calendar.DAY_OF_MONTH)
                day.toString() to value.toFloat()
            }
    }

    fun guidesFor(sportId: String, category: LearnCategory?): List<LearnGuide> {
        val filtered = guides.filter { it.sportId == sportId }
        return if (category == null) filtered else filtered.filter { it.category == category }
    }

    fun guideById(id: String): LearnGuide? = guides.find { it.id == id }

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
                type = EventType.EVENT,
                startsAt = now + day * (9 + index),
                location = "Main stadium",
                notes = "Arrive 45 minutes before start. Team photo at the gate."
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

fun LearnCategory.label(): String = when (this) {
    LearnCategory.RULES -> "rules"
    LearnCategory.TECHNIQUES -> "techniques"
    LearnCategory.TRAINING -> "training"
    LearnCategory.SAFETY -> "safety"
}
