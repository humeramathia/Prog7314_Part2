package com.example.prog7314_part2.logging

import android.util.Log

/**
 * Small wrapper around [android.util.Log] so every SportSphere log line
 * shares one tag prefix. In Logcat, filter on `tag:SS.` to see only the
 * app's own output (lifecycle, notifications, language changes).
 *
 * Tags are kept short because Android older than API 26 rejects tags
 * longer than 23 characters in [Log.isLoggable].
 *
 * Reference: Android Developers (2025) *Write and view logs with Logcat*.
 * Available at: https://developer.android.com/studio/debug/logcat
 */
object AppLog {

    /** Prefix shared by every tag the app writes. */
    const val PREFIX = "SS."

    /** Builds a tag such as `SS.Lifecycle` from a short area name. */
    fun tag(area: String): String = (PREFIX + area).take(MAX_TAG_LENGTH)

    fun d(area: String, message: String) {
        Log.d(tag(area), message)
    }

    fun i(area: String, message: String) {
        Log.i(tag(area), message)
    }

    fun w(area: String, message: String, error: Throwable? = null) {
        Log.w(tag(area), message, error)
    }

    fun e(area: String, message: String, error: Throwable? = null) {
        Log.e(tag(area), message, error)
    }

    /** Longest tag older Android versions accept. */
    const val MAX_TAG_LENGTH = 23
}
