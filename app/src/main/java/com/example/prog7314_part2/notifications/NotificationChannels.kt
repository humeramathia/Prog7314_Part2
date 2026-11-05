package com.example.prog7314_part2.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.prog7314_part2.R
import com.example.prog7314_part2.logging.AppLog

/**
 * Creates the Android notification channel used for fixture alerts and
 * answers "can this phone show our notifications right now?".
 *
 * Android 8+ drops notifications that do not name an existing channel,
 * and Android 13+ also needs the POST_NOTIFICATIONS runtime permission.
 *
 * Reference: Android Developers (2025) *Create and manage notification
 * channels* and *Notification runtime permission*.
 * Available at: https://developer.android.com/develop/ui/views/notifications/channels
 */
object NotificationChannels {

    /** Must match `FIXTURE_CHANNEL` in `api/src/notify.js` and the manifest meta-data. */
    const val FIXTURES = "fixtures"

    private const val AREA = "Push"

    /** Safe to call on every launch; Android ignores repeat creation. */
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            FIXTURES,
            context.getString(R.string.notification_channel_fixtures),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_fixtures_description)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        AppLog.d(AREA, "Notification channel '$FIXTURES' ready")
    }

    /** True on Android 13+ where the user has to grant POST_NOTIFICATIONS. */
    fun needsRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** True when the runtime permission is granted (always true below Android 13). */
    fun hasPermission(context: Context): Boolean {
        if (!needsRuntimePermission()) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** True when notifications can actually appear (permission + not blocked in system settings). */
    fun canNotify(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()
}
