package com.example.prog7314_part2.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the SportSphere REST API.
 *
 * Every method returns Retrofit's synchronous [Call] type; callers use
 * `.enqueue(...)` from a fragment so results arrive on the main thread
 * and cancellation is straightforward in `onDestroyView`.
 *
 * The base URL is injected by [ApiClient] from `BuildConfig.API_BASE_URL`.
 * Authentication is handled by [FirebaseAuthInterceptor], so no method
 * here declares an `Authorization` header.
 */
interface SportSphereApi {

    // ─────────────────────────── Auth ────────────────────────────

    /**
     * Server-side fallback for `FirebaseUser.sendEmailVerification()`.
     *
     * Called by [com.example.prog7314_part2.ui.auth.EmailVerification]
     * when the client-side send is rate-limited on the current device.
     */
    @POST("api/auth/send-verification")
    fun sendVerificationEmail(): Call<SendVerificationResponse>

    // ────────────────────────── Profile ──────────────────────────

    /** Returns the current user's profile, creating one on first call. */
    @GET("api/me")
    fun getMyProfile(): Call<UserProfileDto>

    /** Applies a partial update; unspecified fields are left untouched. */
    @PATCH("api/me")
    fun updateMyProfile(
        @Body request: UpdateProfileRequest
    ): Call<UserProfileDto>

    // ─────────────────────────── Sports ──────────────────────────

    /** All sports in the catalog, in the order they were seeded. */
    @GET("api/sports")
    fun getSports(): Call<List<SportDto>>

    /**
     * A single sport with its metric schema. Used by the Add Performance
     * screen to build one input field per metric.
     */
    @GET("api/sports/{sportId}")
    fun getSport(
        @Path("sportId") sportId: String
    ): Call<SportDto>

    // ────────────────────────── Performance ──────────────────────

    /** Every session recorded by the current user for [sportId], newest first. */
    @GET("api/performance")
    fun getPerformance(
        @Query("sportId") sportId: String
    ): Call<List<PerformanceSessionDto>>

    /** Persists a new session and returns the server's authoritative copy. */
    @POST("api/performance")
    fun createPerformance(
        @Body request: CreatePerformanceRequest
    ): Call<PerformanceSessionDto>

    /**
     * Datapoints for the monthly graph. [metric] defaults to the sport's
     * primary metric (see `api/src/data/metrics.js`) when omitted.
     */
    @GET("api/performance/monthly")
    fun getMonthlyPerformance(
        @Query("sportId") sportId: String,
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("metric") metric: String? = null
    ): Call<MonthlyPerformanceDto>

    /** Removes one session owned by the current user. Returns HTTP 204. */
    @DELETE("api/performance/{sessionId}")
    fun deletePerformance(
        @Path("sessionId") sessionId: String
    ): Call<Void>

    // ─────────────────────────── Learn ───────────────────────────

    /** Learn cards for a sport, optionally filtered to a single category. */
    @GET("api/learn")
    fun getLearn(
        @Query("sportId") sportId: String,
        @Query("category") category: String? = null
    ): Call<List<LearnGuideDto>>

    /** A single Learn card (title, body, optional mediaUrl). */
    @GET("api/learn/{guideId}")
    fun getLearnGuide(
        @Path("guideId") guideId: String
    ): Call<LearnGuideDto>

    // ─────────────────────────── Events ──────────────────────────

    /**
     * Calendar events for [sportId], optionally bounded by a time window
     * in epoch milliseconds.
     */
    @GET("api/events")
    fun getEvents(
        @Query("sportId") sportId: String,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null
    ): Call<List<CalendarEventDto>>

    /**
     * The next event whose `startsAt` is in the future, or the most
     * recent past event if none are upcoming (used by the Home tile).
     */
    @GET("api/events/next")
    fun getNextEvent(
        @Query("sportId") sportId: String
    ): Call<CalendarEventDto>

    /** A single event by id. */
    @GET("api/events/{eventId}")
    fun getEvent(
        @Path("eventId") eventId: String
    ): Call<CalendarEventDto>
}
