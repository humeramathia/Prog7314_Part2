package com.example.prog7314_part2.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** The language picker must map every tag Android may hand back to a shipped language. */
class AppLanguageTest {

    @Test
    fun fromTag_readsPlainCodes() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.AFRIKAANS, AppLanguage.fromTag("af"))
        assertEquals(AppLanguage.XHOSA, AppLanguage.fromTag("xh"))
    }

    @Test
    fun fromTag_ignoresRegionAndCase() {
        assertEquals(AppLanguage.AFRIKAANS, AppLanguage.fromTag("af-ZA"))
        assertEquals(AppLanguage.XHOSA, AppLanguage.fromTag("XH_za"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
    }

    @Test
    fun fromTag_unsupportedOrBlankFallsBackToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(null))
    }

    @Test
    fun codes_matchApiAllowList() {
        // api/src/routes/deviceSettings.js accepts exactly en, af, xh.
        assertEquals(listOf("en", "af", "xh"), AppLanguage.entries.map { it.code })
    }
}
