package com.example.prog7314_part2

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import com.example.prog7314_part2.data.PerformanceRepository
import com.example.prog7314_part2.data.SessionPrefs
import com.example.prog7314_part2.data.SyncWorker
import com.example.prog7314_part2.data.biometric.BiometricStore
import com.example.prog7314_part2.data.local.AppDatabase
import com.example.prog7314_part2.data.biometric.BiometricSession
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.AuthExpiry
import com.example.prog7314_part2.logging.AppLog
import com.example.prog7314_part2.logging.LifecycleLogger
import com.example.prog7314_part2.notifications.NotificationChannels
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    val performanceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val performance: PerformanceRepository by lazy {
        PerformanceRepository(
            Room.databaseBuilder(this, AppDatabase::class.java, "performance.db").build(),
            ApiClient.service
        ) { uid -> SyncWorker.enqueue(this, uid) }
    }

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
        registerActivityLifecycleCallbacks(LifecycleLogger())
        AppLog.i("App", "SportSphere process started (dark mode = ${session.darkMode})")
        NotificationChannels.create(this)
        AuthExpiry.install {
            FirebaseAuth.getInstance().signOut()
            session.signOut()
            BiometricSession.lock()
        }
        // Wake a sleeping Render instance as soon as the process starts.
        performanceScope.launch {
            try {
                ApiClient.service.health().execute()
            } catch (error: Exception) {
                AppLog.w("App", "health ping failed: ${error.message}")
            }
        }
        var previousUid: String? = null
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != previousUid) {
                previousUid?.let { SyncWorker.stop(this, it) }
                previousUid = uid
                uid?.let { SyncWorker.start(this, it) }
            }
        }

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
