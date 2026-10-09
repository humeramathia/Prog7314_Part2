package com.example.prog7314_part2.ui.auth

import androidx.appcompat.app.AppCompatDelegate
import com.example.prog7314_part2.data.SessionPrefs
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.google.firebase.auth.FirebaseAuth

object ProfileSync {

    const val UNAVAILABLE =
        "Could not reach SportSphere. The club server may be waking up — wait a few seconds and try again."

    fun apply(session: SessionPrefs, profile: UserProfileDto) {
        if (profile.displayName.isNotBlank()) {
            session.displayName = profile.displayName
        }
        session.sportId = profile.sportId
        session.sportName = profile.sportName
        session.darkMode = profile.darkMode
        AppCompatDelegate.setDefaultNightMode(
            if (profile.darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    fun currentUserId(): String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun canManageEvent(createdBy: String): Boolean {
        val uid = currentUserId()
        return uid.isNotBlank() && createdBy.isNotBlank() && createdBy != "seed" && createdBy == uid
    }
}
