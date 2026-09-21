package com.example.prog7314_part2.data.remote

import com.example.prog7314_part2.data.CalendarEvent
import com.example.prog7314_part2.data.EventType
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.data.SportMetrics

/**
 * Converts the wire-format DTOs from [ApiModels] into the app-friendly
 * domain models declared in [com.example.prog7314_part2.data.Models].
 *
 * All mapping is done here so the UI can rely on non-null values and
 * strongly-typed enums instead of Gson's raw JSON strings.
 */

/**
 * Maps a raw event DTO to the domain [CalendarEvent] used by the
 * calendar and event-detail screens.
 *
 * Type mapping tolerates the legacy `"EVENT"` value emitted by older
 * seeds — it is remapped to [EventType.SOCIAL_EVENT]. Anything the app
 * does not recognise falls back to [EventType.PRACTICE] so a stray
 * value never crashes the list.
 */
fun CalendarEventDto.toLocal(): CalendarEvent {
    val mappedType = when (type.uppercase()) {
        "ANNOUNCEMENT" -> EventType.ANNOUNCEMENT
        "SOCIAL_EVENT", "EVENT" -> EventType.SOCIAL_EVENT
        else -> EventType.PRACTICE
    }
    return CalendarEvent(
        id = id,
        sportId = sportId,
        title = title,
        type = mappedType,
        startsAt = startsAt,
        endsAt = endsAt,
        location = location.orEmpty(),
        description = description.orEmpty(),
        notes = notes.orEmpty()
    )
}

/** Maps the wire DTO to the domain [PerformanceSession]. */
fun PerformanceSessionDto.toLocal(): PerformanceSession =
    PerformanceSession(
        id = id,
        sportId = sportId,
        recordedAt = recordedAt,
        metrics = metrics,
        notes = notes
    )

/**
 * One-line summary used by the Home tile and the Performance list.
 * Combines [SportMetrics.summary] with the free-text notes, e.g.
 * `"22 points  ·  8 rebounds  ·  Home fixture"`.
 */
fun PerformanceSessionDto.summaryLine(): String {
    val metricsLine = SportMetrics.summary(sportId, metrics)
    return if (notes.isBlank()) metricsLine else "$metricsLine  ·  $notes"
}
