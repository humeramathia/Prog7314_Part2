package com.example.prog7314_part2.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface SportSphereApi {

    @GET("api/me")
    fun getMyProfile(): Call<UserProfileDto>

    @PATCH("api/me")
    fun updateMyProfile(
        @Body request: UpdateProfileRequest
    ): Call<UserProfileDto>

    @GET("api/sports")
    fun getSports(): Call<List<SportDto>>
}