package com.example.prog7314_part2.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class SessionPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() {
            val user = FirebaseAuth.getInstance().currentUser

            return prefs.getBoolean(KEY_LOGGED_IN, false) &&
                    user != null &&
                    user.isEmailVerified &&
                    user.email == email
        }
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var email: String
        get() = prefs.getString(KEY_EMAIL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var displayName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var sportId: String
        get() = prefs.getString(KEY_SPORT_ID, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SPORT_ID, value).apply()

    var sportName: String
        get() = prefs.getString(KEY_SPORT_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SPORT_NAME, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    fun signIn(email: String, displayName: String) {
        this.email = email
        this.displayName = displayName.ifBlank { email.substringBefore("@") }
        isLoggedIn = true
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        prefs.edit()
            .remove(KEY_LOGGED_IN)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_SPORT_ID)
            .remove(KEY_SPORT_NAME)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "sportsphere_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "display_name"
        private const val KEY_SPORT_ID = "sport_id"
        private const val KEY_SPORT_NAME = "sport_name"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
