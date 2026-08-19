package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val avgSpeedMps: Double = 0.0,
    val avgPaceMinPerKm: Double = 0.0,
    val territoriesCapturedCount: Int = 0,
    val pointsCount: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED, CANCELLED, FAILED
    val syncStatus: String = "PENDING_SYNC", // SYNCED, PENDING_SYNC, OFFLINE_SAVED, FAILED
    val isOffline: Boolean = false,
    val validationPassed: Boolean = true
)

