package com.example.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import com.example.core.database.entity.SyncQueueEntity
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val syncQueueDao = database.syncQueueDao()
    
    // We assume payloads are JSON objects stringified
    // Idempotency: using actionType and a unique id in payload if needed

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingItems = syncQueueDao.getPendingItemsList()
            if (pendingItems.isEmpty()) {
                return@withContext Result.success()
            }

            var allSuccess = true
            var needsRetry = false

            for (item in pendingItems) {
                // Mark as UPLOADING
                syncQueueDao.update(item.copy(status = "UPLOADING"))
                
                try {
                    val success = processAction(item)
                    if (success) {
                        syncQueueDao.update(item.copy(status = "SUCCESS"))
                        // Cleanup can happen here or in another worker
                        syncQueueDao.delete(item.id)
                    } else {
                        allSuccess = false
                        handleFailure(item)
                        needsRetry = true
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    handleFailure(item)
                    needsRetry = true
                }
            }

            if (needsRetry) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun processAction(item: SyncQueueEntity): Boolean {
        // Here we'd map actionType to the actual API call
        // For simulation, we assume any API call works or is simulated
        
        return when (item.actionType) {
            "CAPTURE_TERRITORY" -> {
                // apiService.submitCapture(item.payloadJson)
                true
            }
            "COMPLETE_SESSION" -> {
                // apiService.submitRunSession(item.payloadJson)
                true
            }
            "BATTLE_RESULT" -> {
                // apiService.submitBattleResult(item.payloadJson)
                true
            }
            else -> {
                // Unknown action
                true // Just skip it / mark success to delete
            }
        }
    }

    private suspend fun handleFailure(item: SyncQueueEntity) {
        val nextRetryCount = item.retryCount + 1
        if (nextRetryCount > 5) { // Max retries
            syncQueueDao.update(item.copy(status = "FAILED", retryCount = nextRetryCount))
        } else {
            syncQueueDao.update(item.copy(status = "RETRYING", retryCount = nextRetryCount))
        }
    }

    companion object {
        fun buildConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
    }
}
