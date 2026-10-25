package com.example.prog7314_part2.data.biometric

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.biometricDataStore by preferencesDataStore(name = "sportsphere_biometrics")

/**
 * DataStore-backed biometric preference.
 *
 * The POE tips require this answer to live in DataStore, not a raw
 * SharedPreferences file, and never as a stored password
 * (PROG7314 POE tips, s. 3). The enabled flag is also mirrored on
 * `PATCH /api/me` so it follows the user to another phone; this class
 * is the on-device cache used at cold start before the API responds.
 */
class BiometricStore(context: Context) {

    private val dataStore = context.applicationContext.biometricDataStore

    @Volatile
    var enabled: Boolean = false
        private set

    @Volatile
    var userId: String = ""
        private set

    suspend fun refresh() {
        val prefs = dataStore.data.first()
        enabled = prefs[KEY_ENABLED] ?: false
        userId = prefs[KEY_USER_ID].orEmpty()
        Log.i(TAG, "refresh enabled=$enabled userIdBlank=${userId.isBlank()}")
    }

    fun enabledFor(userId: String): Boolean {
        return enabled && userId.isNotBlank() && this.userId == userId
    }

    suspend fun setEnabled(userId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
            prefs[KEY_USER_ID] = userId
        }
        this.enabled = enabled
        this.userId = userId
        Log.i(TAG, "setEnabled enabled=$enabled")
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
        enabled = false
        userId = ""
        Log.i(TAG, "cleared local biometric preference")
    }

    private companion object {
        const val TAG = "Biometric"
        val KEY_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_USER_ID = stringPreferencesKey("biometric_user_id")
    }
}
