package com.example.prog7314_part2.data.remote

/**
 * Request and response bodies for push notifications and language
 * settings. Kept apart from [UserProfileDto] / [UpdateProfileRequest] so
 * the Part 3 features do not change the existing profile contract.
 *
 * Gson leaves out null fields, so each request only sends what changed.
 * See `api/src/routes/deviceSettings.js` for the server-side rules.
 */

/** Body for `PATCH /api/me` when only device settings change. */
data class DeviceSettingsRequest(
    /** FCM registration token. Send "" to remove the phone's registration. */
    val fcmToken: String? = null,
    /** Settings → Notifications switch. */
    val notificationsEnabled: Boolean? = null,
    /** Settings → Language: `en`, `af` or `xh`. */
    val language: String? = null
)

/** Response of `POST /api/me/test-notification`. */
data class TestNotificationResponse(
    val ok: Boolean = false,
    val messageId: String? = null
)
