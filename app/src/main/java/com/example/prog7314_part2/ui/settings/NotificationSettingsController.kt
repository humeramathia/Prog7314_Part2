package com.example.prog7314_part2.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.TestNotificationResponse
import com.example.prog7314_part2.databinding.FragmentSettingsBinding
import com.example.prog7314_part2.logging.AppLog
import com.example.prog7314_part2.notifications.NotificationChannels
import com.example.prog7314_part2.notifications.NotificationPrefs
import com.example.prog7314_part2.notifications.PushRegistrar
import com.example.prog7314_part2.ui.auth.ProfileSync
import com.example.prog7314_part2.ui.session
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Drives the Notifications card on the Settings screen:
 *  - master switch plus Practice / Events / Announcements (tips: one switch per type),
 *  - a status line that explains the current state,
 *  - a "Send test notification" button that asks the API to push to this phone.
 */
class NotificationSettingsController(
    private val fragment: Fragment,
    private val binding: FragmentSettingsBinding,
    private val permissionLauncher: ActivityResultLauncher<String>
) {

    private val context get() = fragment.requireContext()
    private val prefs by lazy { NotificationPrefs(context) }
    private var testCall: Call<TestNotificationResponse>? = null

    fun bind() {
        binding.notificationsSwitch.isChecked = prefs.enabled
        binding.notifyPracticeSwitch.isChecked = prefs.notifyPractice
        binding.notifyEventsSwitch.isChecked = prefs.notifyEvents
        binding.notifyAnnouncementsSwitch.isChecked = prefs.notifyAnnouncements

        binding.notificationsSwitch.setOnCheckedChangeListener { _, checked -> onMasterChanged(checked) }
        binding.notifyPracticeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.notifyPractice = checked
            PushRegistrar.sync(context)
            refresh()
        }
        binding.notifyEventsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.notifyEvents = checked
            PushRegistrar.sync(context)
            refresh()
        }
        binding.notifyAnnouncementsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.notifyAnnouncements = checked
            PushRegistrar.sync(context)
            refresh()
        }
        binding.notificationsStatus.setOnClickListener {
            if (prefs.enabled && !NotificationChannels.canNotify(context)) openSystemSettings()
        }
        binding.btnTestNotification.setOnClickListener { sendTest() }
        refresh()
    }

    fun refresh() {
        val enabled = prefs.enabled
        val allowed = NotificationChannels.canNotify(context)
        val sportName = fragment.session().sportName
        binding.notifyPracticeSwitch.isEnabled = enabled
        binding.notifyEventsSwitch.isEnabled = enabled
        binding.notifyAnnouncementsSwitch.isEnabled = enabled
        binding.notificationsStatus.text = when {
            !enabled -> context.getString(R.string.notifications_status_off)
            !allowed -> context.getString(R.string.notifications_status_blocked)
            sportName.isBlank() -> context.getString(R.string.notifications_status_on_no_sport)
            else -> context.getString(R.string.notifications_status_on, sportName)
        }
        binding.btnTestNotification.isEnabled = enabled && allowed
    }

    fun onPermissionResult(granted: Boolean) {
        AppLog.i(AREA, "Settings permission request: ${if (granted) "granted" else "denied"}")
        refresh()
        if (granted) PushRegistrar.sync(context)
    }

    fun onDestroyView() {
        testCall?.cancel()
        testCall = null
    }

    private fun onMasterChanged(checked: Boolean) {
        AppLog.i(AREA, "Notifications switched ${if (checked) "ON" else "OFF"}")
        prefs.enabled = checked
        if (checked && !NotificationChannels.hasPermission(context)) {
            prefs.permissionAsked = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PushRegistrar.sync(context)
        refresh()
    }

    private fun sendTest() {
        binding.btnTestNotification.isEnabled = false
        AppLog.i(AREA, "Requesting a test push from the API")
        val call = ApiClient.service.sendTestNotification()
        testCall = call
        call.enqueue(object : Callback<TestNotificationResponse> {
            override fun onResponse(
                call: Call<TestNotificationResponse>,
                response: Response<TestNotificationResponse>
            ) {
                if (!fragment.isAdded || fragment.view == null) return
                refresh()
                AppLog.i(AREA, "Test push response: HTTP ${response.code()}")
                val message = when {
                    response.isSuccessful -> R.string.notifications_test_sent
                    response.code() == 409 || response.code() == 410 -> {
                        NotificationPrefs(context).uploadedToken = ""
                        PushRegistrar.sync(context)
                        R.string.notifications_test_registering
                    }
                    response.code() == 503 -> R.string.notifications_test_server_off
                    else -> R.string.notifications_test_failed
                }
                toast(context.getString(message))
            }

            override fun onFailure(call: Call<TestNotificationResponse>, error: Throwable) {
                if (call.isCanceled || !fragment.isAdded || fragment.view == null) return
                AppLog.w(AREA, "Test push request failed", error)
                refresh()
                toast(ProfileSync.UNAVAILABLE)
            }
        })
    }

    private fun openSystemSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
        try {
            fragment.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            AppLog.w(AREA, "No settings screen available on this phone", error)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val AREA = "Push"
    }
}
