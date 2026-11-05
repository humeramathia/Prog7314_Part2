package com.example.prog7314_part2.notifications

import android.content.Context
import com.example.prog7314_part2.SportSphereApp
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.DeviceSettingsRequest
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.example.prog7314_part2.logging.AppLog
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Keeps this phone's push registration in step with the signed-in user:
 *
 *  1. Subscribes to FCM topics for the fixture types the user opted into,
 *     and leaves the rest, so practice/event/announcement alerts match Settings.
 *  2. Uploads the FCM token and type switches to `PATCH /api/me`.
 *  3. Undoes both when notifications are switched off or the user logs out.
 *
 * [sync] is cheap and idempotent: it only talks to Firebase or the API
 * when something actually changed, so it is safe to call each time Home
 * opens and whenever a setting changes.
 *
 * Reference: Google (2025) *Set up a Firebase Cloud Messaging client app
 * on Android*. Available at:
 * https://firebase.google.com/docs/cloud-messaging/android/client
 */
object PushRegistrar {

    private const val AREA = "Push"

    /** Brings topic subscription and server token up to date. */
    fun sync(context: Context) {
        val app = context.applicationContext as SportSphereApp
        val prefs = NotificationPrefs(app)
        if (!app.session.isLoggedIn) {
            AppLog.d(AREA, "sync skipped: nobody signed in")
            return
        }

        val wanted = PushTopics.wanted(
            app.session.sportId,
            prefs.enabled,
            prefs.notifyPractice,
            prefs.notifyEvents,
            prefs.notifyAnnouncements
        )
        updateTopics(prefs, wanted)

        if (!prefs.enabled) {
            if (prefs.uploadedToken.isNotEmpty()) {
                AppLog.i(AREA, "Notifications off: removing this phone from the account")
                upload(
                    prefs,
                    DeviceSettingsRequest(
                        fcmToken = "",
                        notificationsEnabled = false,
                        notifyPractice = prefs.notifyPractice,
                        notifyEvents = prefs.notifyEvents,
                        notifyAnnouncements = prefs.notifyAnnouncements
                    ),
                    ""
                )
            }
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> uploadIfChanged(prefs, token) }
            .addOnFailureListener { error -> AppLog.w(AREA, "Could not read the FCM token", error) }
    }

    /** Called by [SportSphereMessagingService.onNewToken] when Firebase rotates the token. */
    fun onNewToken(context: Context, token: String) {
        val app = context.applicationContext as SportSphereApp
        val prefs = NotificationPrefs(app)
        if (!app.session.isLoggedIn || !prefs.enabled) {
            AppLog.d(AREA, "New token not uploaded (signed out or notifications off)")
            return
        }
        uploadIfChanged(prefs, token)
    }

    /**
     * Stops pushes for the account that is signing out. Deleting the token
     * means the API's stored copy stops working, so no messages for the
     * old account reach this phone. Call before clearing the session.
     */
    fun onSignOut(context: Context) {
        val prefs = NotificationPrefs(context)
        updateTopics(prefs, emptySet())
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { AppLog.i(AREA, "FCM token deleted on sign-out") }
        prefs.clearAccountState()
    }

    fun applyFromProfile(context: Context, profile: UserProfileDto) {
        val prefs = NotificationPrefs(context)
        prefs.enabled = profile.notificationsEnabled
        prefs.notifyPractice = profile.notifyPractice
        prefs.notifyEvents = profile.notifyEvents
        prefs.notifyAnnouncements = profile.notifyAnnouncements
        sync(context)
    }

    private fun uploadIfChanged(prefs: NotificationPrefs, token: String) {
        AppLog.i(AREA, "Uploading FCM token (…${token.takeLast(6)}) and type switches")
        upload(
            prefs,
            DeviceSettingsRequest(
                fcmToken = token,
                notificationsEnabled = true,
                notifyPractice = prefs.notifyPractice,
                notifyEvents = prefs.notifyEvents,
                notifyAnnouncements = prefs.notifyAnnouncements
            ),
            token
        )
    }

    private fun upload(prefs: NotificationPrefs, request: DeviceSettingsRequest, tokenToRemember: String) {
        ApiClient.service.updateDeviceSettings(request).enqueue(object : Callback<UserProfileDto> {
            override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                if (response.isSuccessful) {
                    prefs.uploadedToken = tokenToRemember
                    AppLog.i(AREA, "API saved device settings (HTTP ${response.code()})")
                } else {
                    AppLog.w(AREA, "API rejected device settings (HTTP ${response.code()})")
                }
            }

            override fun onFailure(call: Call<UserProfileDto>, error: Throwable) {
                AppLog.w(AREA, "Could not reach the API to save device settings", error)
            }
        })
    }

    private fun updateTopics(prefs: NotificationPrefs, wanted: Set<String>) {
        val (leave, join) = PushTopics.plan(prefs.subscribedTopics, wanted)
        val messaging = FirebaseMessaging.getInstance()
        leave.forEach { topic ->
            messaging.unsubscribeFromTopic(topic).addOnCompleteListener { task ->
                AppLog.i(AREA, "Unsubscribe from $topic: ${if (task.isSuccessful) "ok" else "failed"}")
            }
        }
        join.forEach { topic ->
            messaging.subscribeToTopic(topic).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AppLog.i(AREA, "Subscribed to $topic")
                } else {
                    AppLog.w(AREA, "Subscribe to $topic failed; will retry on next sync", task.exception)
                }
            }
        }
        if (leave.isNotEmpty() || join.isNotEmpty()) {
            prefs.subscribedTopics = wanted
        }
    }
}
