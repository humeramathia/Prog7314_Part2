package com.example.prog7314_part2.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SportSphereApi {

    @GET("api/me")
    fun getMyProfile(): Call<UserProfileDto>

    @PATCH("api/me")
    fun updateMyProfile(
        @Body request: UpdateProfileRequest
    ): Call<UserProfileDto>

    @GET("api/sports")
    fun getSports(): Call<List<SportDto>>

    @GET("api/sports/{sportId}")
    fun getSport(
        @Path("sportId") sportId: String
    ): Call<SportDto>

    @GET("api/performance")
    fun getPerformance(
        @Query("sportId") sportId: String
    ): Call<List<PerformanceSessionDto>>

    @POST("api/performance")
    fun createPerformance(
        @Body request: CreatePerformanceRequest
    ): Call<PerformanceSessionDto>

    @GET("api/performance/monthly")
    fun getMonthlyPerformance(
        @Query("sportId") sportId: String,
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("metric") metric: String? = null
    ): Call<MonthlyPerformanceDto>

    @DELETE("api/performance/{sessionId}")
    fun deletePerformance(
        @Path("sessionId") sessionId: String
    ): Call<Void>
}