package com.example.prog7314_part2.notifications

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.SportSphereApp
import com.example.prog7314_part2.logging.AppLog

/**
 * Asks for the Android 13+ notification permission once, the first time a
 * signed-in user reaches Home, and refreshes the push registration every
 * time Home opens (covers cold start, a new sign-in and a sport change).
 *
 * Asking on Home rather than at launch follows Android's guidance to
 * request a permission in context, after the user knows what the app does.
 * If they say no, Settings → Notifications explains how to turn it on.
 *
 * Must be attached from `onCreate`, because activity-result launchers
 * have to be registered before the activity is started.
 *
 * Reference: Android Developers (2025) *Notification runtime permission*.
 * Available at: https://developer.android.com/develop/ui/views/notifications/notification-permission
 */
object NotificationPermissionPrompt {

    private const val AREA = "Push"

    fun attach(activity: AppCompatActivity, navController: NavController) {
        val app = activity.application as SportSphereApp
        val prefs = NotificationPrefs(activity)

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            AppLog.i(AREA, "POST_NOTIFICATIONS ${if (granted) "granted" else "denied"} by the user")
            PushRegistrar.sync(activity)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.homeFragment || !app.session.isLoggedIn) return@addOnDestinationChangedListener
            PushRegistrar.sync(activity)

            val shouldAsk = prefs.enabled &&
                !prefs.permissionAsked &&
                NotificationChannels.needsRuntimePermission() &&
                !NotificationChannels.hasPermission(activity)
            if (shouldAsk) {
                prefs.permissionAsked = true
                AppLog.i(AREA, "Asking for POST_NOTIFICATIONS on first Home visit")
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
