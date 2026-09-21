package com.example.prog7314_part2.data

/**
 * Plain-Kotlin domain models used by the UI layer.
 *
 * These are intentionally decoupled from the wire-format DTOs in
 * [com.example.prog7314_part2.data.remote.ApiModels]. Mapping happens in
 * [com.example.prog7314_part2.data.remote.Mappers] so the UI never has to
 * reason about nullability quirks of the API payload.
 */

/** A single sport the user can pick during onboarding. */
data class Sport(
    /** Stable identifier used everywhere the API expects a `sportId`. */
    val id: String,
    /** Human-readable name shown in the picker (e.g. "Basketball"). */
    val name: String
)

/**
 * An entry on the Calendar tab: a practice, social fixture, or announcement.
 *
 * All timestamps are epoch milliseconds in the device's default time zone.
 */
data class CalendarEvent(
    val id: String,
    val sportId: String,
    val title: String,
    val type: EventType,
    val startsAt: Long,
    /** `null` for events with no explicit end time (e.g. announcements). */
    val endsAt: Long? = null,
    val location: String,
    /** Long-form description. Optional; may be blank. */
    val description: String = "",
    /** Coach/organiser notes shown beneath the description. */
    val notes: String
)

/** Types of calendar entries the API produces. Ordered by frequency in the UI. */
enum class EventType { PRACTICE, SOCIAL_EVENT, ANNOUNCEMENT }

/**
 * One recorded training/match session for a sport.
 *
 * [metrics] is a sparse map keyed by the metric identifier defined in
 * [SportMetrics.fields] (e.g. `points`, `rebounds`, `distance`).
 */
data class PerformanceSession(
    val id: String,
    val sportId: String,
    val recordedAt: Long,
    val metrics: Map<String, Double>,
    val notes: String
)

/** A single beginner-friendly Learn card (Rules / Techniques / Training / Safety). */
data class LearnGuide(
    val id: String,
    val sportId: String,
    val category: LearnCategory,
    val title: String,
    val body: String
)

/** Chip categories on the Learn tab. Must stay in sync with the API's CATEGORIES. */
enum class LearnCategory { RULES, TECHNIQUES, TRAINING, SAFETY }
