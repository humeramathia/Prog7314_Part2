package com.example.prog7314_part2.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.prog7314_part2.MainActivity
import com.example.prog7314_part2.R
import com.example.prog7314_part2.logging.AppLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging events.
 *
 * - When the app is in the background, Android shows messages that carry
 *   a `notification` block by itself (on the channel named in the manifest).
 * - When the app is open, Android hands the message to [onMessageReceived]
 *   instead, so we build and show the notification ourselves. Without this,
 *   users would miss alerts while they are using the app.
 *
 * Registered in AndroidManifest.xml with the `MESSAGING_EVENT` action.
 *
 * Reference: Google (2025) *Receive messages in an Android app*, Firebase
 * Cloud Messaging documentation. Available at:
 * https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class SportSphereMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        AppLog.i(AREA, "Firebase issued a new FCM token")
        PushRegistrar.onNewToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val kind = message.data["kind"] ?: "unknown"
        AppLog.i(AREA, "Push received in foreground: kind=$kind id=${message.messageId}")

        if (!NotificationPrefs(this).enabled) {
            AppLog.d(AREA, "Notifications switched off in Settings; not showing it")
            return
        }
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        show(title, body, message.messageId.hashCode())
    }

    @SuppressLint("MissingPermission") // Checked by NotificationChannels.canNotify just above.
    private fun show(title: String, body: String, id: Int) {
        if (!NotificationChannels.canNotify(this)) {
            AppLog.w(AREA, "Notification permission not granted; message dropped")
            return
        }
        NotificationChannels.create(this)

        // Tapping the notification brings the existing app window forward.
        val openApp = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.FIXTURES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.teal))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        NotificationManagerCompat.from(this).notify(id, notification)
        AppLog.d(AREA, "Notification shown: \"$title\"")
    }

    private companion object {
        const val AREA = "Push"
    }
}
