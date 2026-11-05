package com.example.prog7314_part2.ui.settings

import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.DeviceSettingsRequest
import com.example.prog7314_part2.data.remote.UserProfileDto
import com.example.prog7314_part2.databinding.FragmentSettingsBinding
import com.example.prog7314_part2.logging.AppLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Drives the Language card on the Settings screen.
 *
 * Tapping a language:
 *  1. switches the app immediately through [AppLanguage.apply]
 *     (AppCompat recreates the screen, which stays on Settings), and
 *  2. saves the choice on the user's profile with `PATCH /api/me`, so the
 *     preference is visible in the back-end database and used for
 *     server-sent messages such as the test notification.
 */
class LanguageSettingsController(private val binding: FragmentSettingsBinding) {

    fun bind() {
        val current = AppLanguage.current()
        AppLog.d(AREA, "Settings opened with language ${current.code}")
        // Check the current language before adding the listener so it does not fire.
        binding.languageToggle.check(buttonFor(current))
        binding.languageToggle.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val chosen = languageFor(buttonId)
            if (chosen == AppLanguage.current()) return@addOnButtonCheckedListener
            AppLog.i(AREA, "User picked ${chosen.code}")
            saveToProfile(chosen)
            AppLanguage.apply(chosen)
        }
    }

    /**
     * Saves on the profile. The call is not cancelled with the view because
     * changing language recreates the screen straight after this starts.
     */
    private fun saveToProfile(language: AppLanguage) {
        ApiClient.service.updateDeviceSettings(DeviceSettingsRequest(language = language.code))
            .enqueue(object : Callback<UserProfileDto> {
                override fun onResponse(call: Call<UserProfileDto>, response: Response<UserProfileDto>) {
                    AppLog.i(AREA, "Language ${language.code} saved on profile (HTTP ${response.code()})")
                }

                override fun onFailure(call: Call<UserProfileDto>, error: Throwable) {
                    // The phone still switches; only the cloud copy is missing.
                    AppLog.w(AREA, "Could not save language on profile", error)
                }
            })
    }

    private fun buttonFor(language: AppLanguage): Int = when (language) {
        AppLanguage.ENGLISH -> R.id.btnLanguageEnglish
        AppLanguage.AFRIKAANS -> R.id.btnLanguageAfrikaans
        AppLanguage.XHOSA -> R.id.btnLanguageXhosa
    }

    private fun languageFor(buttonId: Int): AppLanguage = when (buttonId) {
        R.id.btnLanguageAfrikaans -> AppLanguage.AFRIKAANS
        R.id.btnLanguageXhosa -> AppLanguage.XHOSA
        else -> AppLanguage.ENGLISH
    }

    private companion object {
        const val AREA = "Language"
    }
}
