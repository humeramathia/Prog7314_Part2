package com.example.prog7314_part2.data.remote

import com.example.prog7314_part2.data.CalendarEvent
import com.example.prog7314_part2.data.EventType
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.data.SportMetrics

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

fun PerformanceSessionDto.toLocal(): PerformanceSession =
    PerformanceSession(
        id = id,
        sportId = sportId,
        recordedAt = recordedAt,
        metrics = metrics,
        notes = notes
    )

fun PerformanceSessionDto.summaryLine(): String {
    val metricsLine = SportMetrics.summary(sportId, metrics)
    return if (notes.isBlank()) metricsLine else "$metricsLine  ·  $notes"
}
