package com.example.prog7314_part2.data

import android.util.Log
import androidx.room.withTransaction
import com.example.prog7314_part2.data.local.AppDatabase
import com.example.prog7314_part2.data.local.SessionEntity
import com.example.prog7314_part2.data.local.SyncMetadata
import com.example.prog7314_part2.data.local.SyncPolicy
import com.example.prog7314_part2.data.remote.CreatePerformanceRequest
import com.example.prog7314_part2.data.remote.SportSphereApi
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Room is the source of truth. No screen has to wait for an upload to save. */
class PerformanceRepository(
    private val database: AppDatabase,
    private val api: SportSphereApi,
    private val schedule: (String) -> Unit
) {
    private val dao = database.sessions()
    // One uploader/refresh/delete at a time, including the periodic backup worker.
    private val syncMutex = Mutex()

    fun observe(uid: String, sportId: String) = dao.observe(uid, sportId).map { rows ->
        rows.map { it.toLocal() }
    }

    fun lastSync(uid: String) = database.syncMetadata().observe(uid)

    suspend fun save(uid: String, request: CreatePerformanceRequest): String = withContext(Dispatchers.IO) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        require(request.sportId.isNotBlank() && request.metrics.isNotEmpty())
        require(request.metrics.values.all { it.isFinite() && it >= 0 })
        val id = UUID.randomUUID().toString()
        dao.put(SessionEntity(
            userId = uid, localId = id, sportId = request.sportId,
            recordedAt = request.recordedAt, notes = request.notes,
            metricsJson = Gson().toJson(request.metrics), updatedAt = System.currentTimeMillis()
        ))
        // Enqueue only after the durable write. Startup also re-enqueues after a
        // process death between this insert and the WorkManager call.
        try {
            schedule(uid)
        } catch (error: Exception) {
            // The durable save succeeded. Startup/periodic work can recover
            // scheduling; reporting a failed save would invite duplicate input.
            Log.w(TAG, "Saved locally; scheduling will resume on next startup", error)
        }
        Log.i(TAG, "Session saved locally; upload enqueued")
        id
    }

    suspend fun refresh(uid: String, sportId: String) = withContext(Dispatchers.IO) {
        syncMutex.withLock { pull(uid, sportId, authorization(uid)) }
    }

    /** Returns false when temporary failures leave rows waiting for retry. */
    suspend fun sync(uid: String): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val auth = authorization(uid)
            // Do not upload to an older deployment that would create a second
            // document when WorkManager retries after a lost response.
            val health = api.health().execute()
            if (!health.isSuccessful || health.body()?.capabilities?.get("performanceClientId") != true) {
                throw IOException("API deployment does not support safe offline uploads yet")
            }
            var retry = false
            for (row in dao.pending(uid)) {
                checkOwner(uid)
                val response = api.createPerformance(row.toRequest(), auth).execute()
                val remote = response.body()
                when {
                    response.isSuccessful && remote != null && remote.userId == uid -> {
                        dao.setState(uid, row.localId, SyncPolicy.SYNCED, remote.id)
                        Log.i(TAG, "Session upload completed")
                    }
                    SyncPolicy.isPermanentFailure(response.code()) -> {
                        dao.setState(uid, row.localId, SyncPolicy.FAILED, null)
                        Log.w(TAG, "Session rejected (${response.code()}); retained for review")
                    }
                    else -> {
                        retry = true
                        Log.i(TAG, "Upload deferred (${response.code()})")
                        // Stop on auth failures, retaining every pending row.
                        if (response.code() == 401 || response.code() == 403) break
                    }
                }
            }
            if (!retry) {
                for (sportId in dao.sports(uid)) pull(uid, sportId, auth)
                database.syncMetadata().put(SyncMetadata(uid, System.currentTimeMillis()))
            }
            !retry
        }
    }

    /** Synced sessions require a successful server delete. Pending uploads stay intact. */
    suspend fun delete(uid: String, localId: String): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val row = dao.get(uid, localId) ?: return@withLock true
            checkOwner(uid)
            if (row.syncState == SyncPolicy.PENDING) return@withLock false
            if (row.serverId != null) {
                val response = api.deletePerformance(row.serverId, authorization(uid)).execute()
                if (!response.isSuccessful && response.code() != 404) throw IOException("Delete failed")
            }
            dao.delete(uid, localId)
            true
        }
    }

    private suspend fun pull(uid: String, sportId: String, auth: String) {
        checkOwner(uid)
        val response = api.getPerformance(sportId, auth).execute()
        val rows = response.body()
        if (!response.isSuccessful || rows == null || rows.any { it.userId != uid || it.sportId != sportId }) {
            throw IOException("History refresh failed")
        }
        database.withTransaction {
            // Only replace acknowledged data; never discard pending or rejected rows.
            // A lost upload response can leave a pending row that already exists on
            // the server. Skip that remote copy until the idempotent retry completes.
            val pendingIds = dao.pending(uid).map { it.localId }.toSet()
            dao.clearSynced(uid, sportId)
            rows.filter { it.clientId !in pendingIds }.forEach { dao.put(SessionEntity.fromRemote(it)) }
        }
    }

    private fun checkOwner(uid: String) {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) throw IOException("Account changed")
    }

    private fun authorization(uid: String): String {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.uid != uid) throw IOException("Sign in required")
        val token = try {
            Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS).token
        } catch (error: Exception) {
            throw IOException("Authentication temporarily unavailable", error)
        }
        if (token.isNullOrBlank()) throw IOException("Sign in required")
        return "Bearer $token"
    }

    private companion object { const val TAG = "PerformanceSync" }
}
