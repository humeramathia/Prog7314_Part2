package com.example.prog7314_part2

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.prog7314_part2.data.SessionPrefs
import androidx.room.Room
import com.example.prog7314_part2.data.PerformanceRepository
import com.example.prog7314_part2.data.SyncWorker
import com.example.prog7314_part2.data.local.AppDatabase
import com.example.prog7314_part2.data.remote.ApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    override fun onCreate() {
        super.onCreate()
        session = SessionPrefs(this)
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
}
