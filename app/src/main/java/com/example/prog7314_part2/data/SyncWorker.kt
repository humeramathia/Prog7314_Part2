package com.example.prog7314_part2.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.prog7314_part2.SportSphereApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(USER_ID) ?: return Result.failure()
        // Queues are account-scoped. Signing into B must never upload A's sessions as B.
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()
        return try {
            val repository = (applicationContext as SportSphereApp).performance
            if (repository.sync(uid)) Result.success() else Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.i("PerformanceSync", "Sync deferred; will retry", error)
            Result.retry()
        }
    }

    companion object {
        private const val USER_ID = "userId"
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        private fun tag(uid: String) = "performance-sync-$uid"

        fun enqueue(context: Context, uid: String) {
            val work = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(USER_ID to uid))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(tag(uid)).build()
            // Append also covers saves arriving while a worker has already read its queue.
            WorkManager.getInstance(context).enqueueUniqueWork(tag(uid), ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }

        fun start(context: Context, uid: String) {
            enqueue(context, uid)
            val periodic = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf(USER_ID to uid)).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(tag(uid)).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${tag(uid)}-periodic", ExistingPeriodicWorkPolicy.KEEP, periodic
            )
        }

        fun stop(context: Context, uid: String) {
            WorkManager.getInstance(context).cancelAllWorkByTag(tag(uid))
        }
    }
}
