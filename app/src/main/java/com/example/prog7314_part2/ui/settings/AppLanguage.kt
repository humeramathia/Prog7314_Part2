package com.example.prog7314_part2.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.prog7314_part2.logging.AppLog

/**
 * The languages SportSphere ships: English plus two official South African
 * languages. Each code matches a `values-<code>` resource folder and the
 * API's allowed `language` values (`api/src/routes/deviceSettings.js`).
 *
 * Switching uses AndroidX per-app language support:
 *  - Android 13+ stores the choice in the system (it also appears under
 *    Settings → Apps → SportSphere → Language).
 *  - Older versions store it through `AppLocalesMetadataHolderService`
 *    (declared in the manifest with `autoStoreLocales`).
 * In both cases AppCompat recreates the activity so every screen redraws
 * in the new language straight away, with no app restart.
 *
 * Reference: Android Developers (2025) *Per-app language preferences*.
 * Available at: https://developer.android.com/guide/topics/resources/app-languages
 */
enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    AFRIKAANS("af"),
    XHOSA("xh");

    companion object {
        private const val AREA = "Language"

        /**
         * Maps a BCP-47 tag (`af`, `af-ZA`, `xh_ZA`, `EN`) to a supported
         * language. Anything else, including blank, is English.
         */
        fun fromTag(tag: String?): AppLanguage {
            val primary = tag.orEmpty().trim().lowercase().substringBefore('-').substringBefore('_')
            return entries.firstOrNull { it.code == primary } ?: ENGLISH
        }

        /** Language the app is currently using. Empty app locale = follow the phone = English unless phone is af/xh. */
        fun current(): AppLanguage {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return fromTag(LocaleListCompat.getAdjustedDefault()[0]?.language)
            return fromTag(locales[0]?.toLanguageTag())
        }

        /** Switches the whole app to [language]. AppCompat recreates visible activities. */
        fun apply(language: AppLanguage) {
            AppLog.i(AREA, "Switching app language to ${language.code}")
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        }
    }
}
