package com.example.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val SYNC_WORK_NAME = "run2capture_sync_work"

    fun scheduleSync(context: Context) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(SyncWorker.buildConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10000L, // 10 seconds backoff
                    TimeUnit.MILLISECONDS
                )
                .build()
    
            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE, // Using REPLACE to ensure we trigger now if needed, or KEEP if we want to batch
                workRequest
            )
        } catch (e: IllegalStateException) {
            // Ignore in tests where WorkManager isn't initialized
        }
    }
}
