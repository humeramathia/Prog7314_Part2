package com.example.prog7314_part2.data

data class Sport(
    val id: String,
    val name: String
)

data class CalendarEvent(
    val id: String,
    val sportId: String,
    val title: String,
    val type: EventType,
    val startsAt: Long,
    val endsAt: Long? = null,
    val location: String,
    val description: String = "",
    val notes: String
)

enum class EventType { PRACTICE, SOCIAL_EVENT, ANNOUNCEMENT }

data class PerformanceSession(
    val id: String,
    val sportId: String,
    val recordedAt: Long,
    val metrics: Map<String, Double>,
    val notes: String
)

data class LearnGuide(
    val id: String,
    val sportId: String,
    val category: LearnCategory,
    val title: String,
    val body: String
)

enum class LearnCategory { RULES, TECHNIQUES, TRAINING, SAFETY }
