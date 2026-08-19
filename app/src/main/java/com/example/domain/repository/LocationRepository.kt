package com.example.domain.repository

import com.example.core.database.entity.RunSessionEntity
import com.example.domain.model.GpsPoint
import com.example.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun saveLocationPoint(sessionId: String, location: UserLocation): Long
    suspend fun saveLocationPoints(points: List<GpsPoint>)
    fun getPointsForSession(sessionId: String): Flow<List<GpsPoint>>
    suspend fun getPointsListForSession(sessionId: String): List<GpsPoint>
    fun getPointCountForSession(sessionId: String): Flow<Int>
    suspend fun getLatestPointForSession(sessionId: String): GpsPoint?
    suspend fun clearSessionPoints(sessionId: String)
    suspend fun clearAll()

    // Session Management
    suspend fun startRunSession(sessionId: String, startTime: Long = System.currentTimeMillis()): RunSessionEntity
    suspend fun updateRunSession(session: RunSessionEntity)
    suspend fun endRunSession(
        sessionId: String,
        distanceMeters: Double,
        durationSeconds: Long,
        avgSpeedMps: Double,
        avgPaceMinPerKm: Double = 0.0,
        status: String = "COMPLETED",
        syncStatus: String = "PENDING_SYNC",
        isOffline: Boolean = false,
        validationPassed: Boolean = true
    )
    suspend fun getActiveSession(): RunSessionEntity?
    suspend fun getActiveOrPausedSession(): RunSessionEntity?
    fun observeActiveSession(): Flow<RunSessionEntity?>
    suspend fun getSessionById(sessionId: String): RunSessionEntity?
    suspend fun updateSessionStatus(sessionId: String, status: String)
    suspend fun updateSyncStatus(sessionId: String, syncStatus: String)
    suspend fun getPendingSyncSessions(): List<RunSessionEntity>
}

