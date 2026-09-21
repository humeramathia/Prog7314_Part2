package com.example.prog7314_part2.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Thin wrapper around a private [android.content.SharedPreferences] file
 * that stores the last known account details so screens can render
 * greetings, chosen sport, and theme without waiting on the API.
 *
 * The class is deliberately not a singleton — instantiate it once in
 * [com.example.prog7314_part2.SportSphereApp] and reuse the instance via
 * [com.example.prog7314_part2.ui.session].
 */
class SessionPrefs(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * True only when **all three** conditions hold:
     *  1. A previous [signIn] call flipped the persisted flag on.
     *  2. Firebase still has a live user for the process.
     *  3. That live user's email is verified and matches the stored one.
     *
     * This guards against the "old preference file, new Firebase account"
     * mismatch that used to leak one user's sport into another's home
     * screen after a re-install or account switch.
     */
    var isLoggedIn: Boolean
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            return prefs.getBoolean(KEY_LOGGED_IN, false) &&
                    user != null &&
                    user.isEmailVerified &&
                    user.email == email
        }
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    /** Persisted email of the last confirmed sign-in. Empty when signed out. */
    var email: String
        get() = prefs.getString(KEY_EMAIL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    /** Friendly display name. Falls back to the part before '@' in [signIn]. */
    var displayName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** Currently selected sport id (matches the API's `sportId`). */
    var sportId: String
        get() = prefs.getString(KEY_SPORT_ID, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SPORT_ID, value).apply()

    /** Human-readable sport name, cached to avoid an extra API call on Home. */
    var sportName: String
        get() = prefs.getString(KEY_SPORT_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SPORT_NAME, value).apply()

    /** User's dark-mode preference; applied globally in `SportSphereApp.onCreate`. */
    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    /**
     * Records the profile bits we need to render the app once Firebase has
     * confirmed the account. Only the fields tied to identity are written
     * here; sport selection is stored separately after the user picks one.
     */
    fun signIn(email: String, displayName: String) {
        this.email = email
        // Never leave the greeting blank — derive a friendly name from
        // the email when the provider (e.g. email/password) has none.
        this.displayName = displayName.ifBlank { email.substringBefore("@") }
        isLoggedIn = true
    }

    /**
     * Clears both the Firebase session and every persisted preference key
     * we own. Called from Settings → Log out and defensively from the
     * Register screen before creating a brand new account.
     */
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        prefs.edit()
            .remove(KEY_LOGGED_IN)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_SPORT_ID)
            .remove(KEY_SPORT_NAME)
            .apply()
        // Dark mode intentionally survives sign-out; it's a device preference.
    }

    private companion object {
        const val PREFS_NAME = "sportsphere_session"
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "display_name"
        const val KEY_SPORT_ID = "sport_id"
        const val KEY_SPORT_NAME = "sport_name"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
