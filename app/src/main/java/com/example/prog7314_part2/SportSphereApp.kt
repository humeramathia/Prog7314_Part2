package com.example.prog7314_part2

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.prog7314_part2.data.SessionPrefs

/**
 * Application entry point.
 *
 * Owns the single [SessionPrefs] instance for the whole process and
 * applies the persisted dark-mode preference before the first activity
 * is created so the correct theme is used from the very first frame.
 *
 * The class is registered as `android:name=".SportSphereApp"` in
 * [AndroidManifest.xml].
 */
class SportSphereApp : Application() {

    /** Shared preferences wrapper. Created once, read from every fragment. */
    lateinit var session: SessionPrefs
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionPrefs(this)

        // Apply the saved theme immediately so recreations after a system
        // theme change do not flash the wrong palette.
        AppCompatDelegate.setDefaultNightMode(
            if (session.darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}
