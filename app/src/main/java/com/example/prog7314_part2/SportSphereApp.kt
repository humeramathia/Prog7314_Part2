package com.example.prog7314_part2

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.prog7314_part2.data.SessionPrefs

class SportSphereApp : Application() {

    lateinit var session: SessionPrefs
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionPrefs(this)
        AppCompatDelegate.setDefaultNightMode(
            if (session.darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}
