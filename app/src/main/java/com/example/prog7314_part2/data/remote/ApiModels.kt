package com.example.prog7314_part2.data.remote

/**
 * Data-transfer objects (DTOs) that mirror the JSON returned by the
 * SportSphere REST API.
 *
 * These types are read/written by Gson through Retrofit, so:
 *  - Property names must match the API field names exactly.
 *  - Nullable properties correspond to fields the server may omit.
 *  - Nothing here should reference Android APIs — the DTOs live in the
 *    data layer and are mapped to domain models in
 *    [com.example.prog7314_part2.data.remote.Mappers].
 *
 * If you rename a property, keep the shape of the API responses in
 * mind (see `api/src/routes/`) or add a `@SerializedName` annotation.
 */

/** Response of `GET /api/me` and `PATCH /api/me`. */
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

/**
 * Partial-update body for `PATCH /api/me`.
 *
 * Every field is nullable so a caller can send just the property it
 * wants to change (e.g. only `darkMode` from the Settings toggle).
 */
data class UpdateProfileRequest(
    val displayName: String? = null,
    val sportId: String? = null,
    val sportName: String? = null,
    val darkMode: Boolean? = null
)

/** A single metric column returned inside a [SportDto]. */
data class MetricDto(
    val key: String,
    val label: String
)

/**
 * Sport catalog entry from `GET /api/sports` and `GET /api/sports/{id}`.
 *
 * `metrics` is empty on `GET /api/sports` in older API builds; the
 * per-sport endpoint always populates it.
 */
data class SportDto(
    val id: String,
    val sportId: String,
    val name: String,
    val metrics: List<MetricDto> = emptyList()
)

/** One saved training/match session for the logged-in user. */
data class PerformanceSessionDto(
    val id: String,
    val userId: String,
    val sportId: String,
    val recordedAt: Long,
    val notes: String,
    val metrics: Map<String, Double>,
    /** Server's choice of the "headline" metric for graphs and tiles. */
    val primaryMetric: String,
    val primaryValue: Double
)

/** Body for `POST /api/performance`. */
data class CreatePerformanceRequest(
    val sportId: String,
    val recordedAt: Long,
    val notes: String,
    val metrics: Map<String, Double>
)

/** A single dot on the monthly graph (one session, one metric value). */
data class MonthlyPointDto(
    /** ISO date `YYYY-MM-DD` for the day this point represents. */
    val date: String,
    /** Day of month, mirrors [date] but pre-parsed for the X axis. */
    val day: Int,
    val recordedAt: Long,
    val sessionId: String,
    val value: Double
)

/** Response of `GET /api/performance/monthly?...`. */
data class MonthlyPerformanceDto(
    val sportId: String,
    val year: Int,
    val month: Int,
    val metric: String,
    val metricLabel: String,
    val points: List<MonthlyPointDto>
)

/** One Learn card. */
data class LearnGuideDto(
    val id: String,
    val sportId: String,
    val category: String,
    val title: String,
    val body: String,
    /** Optional. Image URL for a still, or video URL/link for a clip. */
    val mediaUrl: String? = null
)

/** Calendar entry. `type` is one of `PRACTICE`, `SOCIAL_EVENT`, `ANNOUNCEMENT`. */
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

/** Shape of the API's uniform error body: `{ "error": "..." }`. */
data class ApiErrorDto(
    val error: String
)

/** Response of `POST /api/auth/send-verification`. */
data class SendVerificationResponse(
    val ok: Boolean = false,
    val email: String? = null,
    val error: String? = null
)
