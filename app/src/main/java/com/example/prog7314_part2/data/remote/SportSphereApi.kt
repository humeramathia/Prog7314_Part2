package com.example.prog7314_part2.data.remote

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SportSphereApi {

    @GET("api/learn")
    fun getLearn(
        @Query("sportId") sportId: String,
        @Query("category") category: String? = null
    ): Call<List<LearnGuideDto>>

    @GET("api/learn/{guideId}")
    fun getLearnGuide(
        @Path("guideId") guideId: String
    ): Call<LearnGuideDto>
}
