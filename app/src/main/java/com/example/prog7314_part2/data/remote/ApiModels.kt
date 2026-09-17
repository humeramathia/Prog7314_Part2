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

data class SportDto(
    val id: String,
    val sportId: String,
    val name: String
)

data class ApiErrorDto(
    val error: String
)