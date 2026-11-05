package com.example.prog7314_part2.notifications

import android.content.Context

/**
 * Notification settings kept on the phone, in their own preferences file
 * so they do not collide with the account details in
 * [com.example.prog7314_part2.data.SessionPrefs].
 */
class NotificationPrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The Settings switch. On by default; Android 13+ still needs the runtime permission. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Sport topic this phone is currently subscribed to, or "" for none. */
    var subscribedTopic: String
        get() = prefs.getString(KEY_TOPIC, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOPIC, value).apply()

    /** Last FCM token the API accepted, so we only upload when it changes. */
    var uploadedToken: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    /** True once we have asked for POST_NOTIFICATIONS, so we never nag twice. */
    var permissionAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    /** Forget the per-account state on sign-out. The switch itself is a device preference. */
    fun clearAccountState() {
        prefs.edit().remove(KEY_TOPIC).remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val PREFS_NAME = "sportsphere_notifications"
        const val KEY_ENABLED = "enabled"
        const val KEY_TOPIC = "subscribed_topic"
        const val KEY_TOKEN = "uploaded_token"
        const val KEY_ASKED = "permission_asked"
    }
}
