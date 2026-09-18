package com.example.prog7314_part2.data.remote

data class LearnGuideDto(
    val id: String,
    val sportId: String,
    val category: String,
    val title: String,
    val body: String,
    val mediaUrl: String? = null
)
