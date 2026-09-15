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
    val location: String,
    val notes: String
)

enum class EventType { PRACTICE, EVENT }

data class PerformanceSession(
    val id: String,
    val sportId: String,
    val recordedAt: Long,
    val score: Double,
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
