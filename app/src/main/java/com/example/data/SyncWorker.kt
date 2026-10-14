package com.example.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.eventDao()
        val syncManager = FirestoreSyncManager(dao)
        val queueItems = dao.getAllSyncQueueOnce()
        if (queueItems.isEmpty()) return Result.success()

        var allSuccessful = true
        for (item in queueItems) {
            try {
                syncManager.syncQueueItem(item)
                dao.deleteSyncQueueById(item.queueId)
            } catch (e: Exception) {
                allSuccessful = false
                val msg = e.message?.lowercase() ?: ""
                val detailedError = when {
                    msg.contains("permission_denied") || msg.contains("permission denied") ->
                        "PERMISSION_DENIED: Firestore Security Rules rejected write access"
                    msg.contains("unauthenticated") || msg.contains("unauthorized") ->
                        "UNAUTHENTICATED: User is not authenticated with Firebase Auth"
                    else -> e.message ?: "Unknown sync error"
                }
                val updatedItem = item.copy(retryCount = item.retryCount + 1, lastError = detailedError)
                dao.updateSyncQueue(updatedItem)
            }
        }
        return if (allSuccessful) Result.success() else Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "ElbeitSyncWork",
                ExistingWorkPolicy.APPEND,
                request
            )
        }
    }
}
