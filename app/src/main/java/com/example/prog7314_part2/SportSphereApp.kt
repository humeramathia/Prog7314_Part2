package com.example.prog7314_part2

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.prog7314_part2.data.SessionPrefs
import com.example.prog7314_part2.data.biometric.BiometricStore
import kotlinx.coroutines.runBlocking

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

    /** DataStore cache for the biometric unlock switch. */
    lateinit var biometrics: BiometricStore
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionPrefs(this)
        biometrics = BiometricStore(this)
        runBlocking { biometrics.refresh() }
        Log.i(TAG, "app start biometricEnabled=${biometrics.enabled}")

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

    private companion object {
        const val TAG = "Biometric"
    }
}
