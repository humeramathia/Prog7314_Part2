package com.example.prog7314_part2.data.remote

data class UserProfileDto(
    val id: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val sportId: String,
    val sportName: String,
    val darkMode: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class UpdateProfileRequest(
    val displayName: String? = null,
    val sportId: String? = null,
    val sportName: String? = null,
    val darkMode: Boolean? = null
)

data class MetricDto(
    val key: String,
    val label: String
)

data class SportDto(
    val id: String,
    val sportId: String,
    val name: String,
    val metrics: List<MetricDto> = emptyList()
)

data class PerformanceSessionDto(
    val id: String,
    val userId: String,
    val sportId: String,
    val recordedAt: Long,
    val notes: String,
    val metrics: Map<String, Double>,
    val primaryMetric: String,
    val primaryValue: Double
)

data class CreatePerformanceRequest(
    val sportId: String,
    val recordedAt: Long,
    val notes: String,
    val metrics: Map<String, Double>
)

data class MonthlyPointDto(
    val date: String,
    val day: Int,
    val recordedAt: Long,
    val sessionId: String,
    val value: Double
)

data class MonthlyPerformanceDto(
    val sportId: String,
    val year: Int,
    val month: Int,
    val metric: String,
    val metricLabel: String,
    val points: List<MonthlyPointDto>
)

data class LearnGuideDto(
    val id: String,
    val sportId: String,
    val category: String,
    val title: String,
    val body: String,
    val mediaUrl: String? = null
)

data class CalendarEventDto(
    val id: String,
    val sportId: String,
    val title: String,
    val type: String,
    val startsAt: Long,
    val endsAt: Long? = null,
    val location: String? = null,
    val description: String? = null,
    val notes: String? = null
)

data class ApiErrorDto(
    val error: String
)

data class SendVerificationResponse(
    val ok: Boolean = false,
    val email: String? = null,
    val error: String? = null
)
