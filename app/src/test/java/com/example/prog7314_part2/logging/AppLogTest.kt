package com.example.prog7314_part2.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the tag rules that keep Logcat filtering predictable. */
class AppLogTest {

    @Test
    fun tag_addsSharedPrefix() {
        assertEquals("SS.Lifecycle", AppLog.tag("Lifecycle"))
    }

    @Test
    fun tag_isNeverLongerThanAndroidLimit() {
        val tag = AppLog.tag("AVeryLongAreaNameThatKeepsGoing")
        assertTrue(tag.length <= AppLog.MAX_TAG_LENGTH)
        assertTrue(tag.startsWith(AppLog.PREFIX))
    }
}
