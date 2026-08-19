package com.example.data.repository

import com.example.core.database.dao.LocationPointDao
import com.example.core.database.dao.RunSessionDao
import com.example.core.database.entity.LocationPointEntity
import com.example.core.database.entity.RunSessionEntity
import com.example.domain.model.GpsPoint
import com.example.domain.model.UserLocation
import com.example.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.example.core.database.AppDatabase
import com.example.core.database.entity.SyncQueueEntity
import org.json.JSONObject

class LocationRepositoryImpl(
    private val locationPointDao: LocationPointDao,
    private val runSessionDao: RunSessionDao,
    private val appDatabase: AppDatabase? = null
) : LocationRepository {

    override suspend fun saveLocationPoint(sessionId: String, location: UserLocation): Long {
        val entity = LocationPointEntity(
            sessionId = sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitudeMeters,
            speed = location.speedMps,
            accuracy = location.accuracyMeters,
            heading = location.heading,
            timestamp = location.timestamp
        )
        return locationPointDao.insertPoint(entity)
    }

    override suspend fun saveLocationPoints(points: List<GpsPoint>) {
        val entities = points.map { point ->
            LocationPointEntity(
                id = point.id,
                sessionId = point.sessionId,
                latitude = point.latitude,
                longitude = point.longitude,
                altitude = point.altitude,
                speed = point.speed,
                accuracy = point.accuracy,
                heading = point.heading,
                timestamp = point.timestamp
            )
        }
        locationPointDao.insertPoints(entities)
    }

    override fun getPointsForSession(sessionId: String): Flow<List<GpsPoint>> {
        return locationPointDao.getPointsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPointsListForSession(sessionId: String): List<GpsPoint> {
        return locationPointDao.getPointsListForSession(sessionId).map { it.toDomain() }
    }

    override fun getPointCountForSession(sessionId: String): Flow<Int> {
        return locationPointDao.getPointCountForSession(sessionId)
    }

    override suspend fun getLatestPointForSession(sessionId: String): GpsPoint? {
        return locationPointDao.getLatestPointForSession(sessionId)?.toDomain()
    }

    override suspend fun clearSessionPoints(sessionId: String) {
        locationPointDao.deletePointsForSession(sessionId)
    }

    override suspend fun clearAll() {
        locationPointDao.deleteAllPoints()
        runSessionDao.deleteAllSessions()
    }

    override suspend fun startRunSession(sessionId: String, startTime: Long): RunSessionEntity {
        val session = RunSessionEntity(
            sessionId = sessionId,
            startTime = startTime,
            distanceMeters = 0.0,
            durationSeconds = 0,
            avgSpeedMps = 0.0,
            territoriesCapturedCount = 0,
            status = "ACTIVE"
        )
        runSessionDao.insertSession(session)
        return session
    }

    override suspend fun updateRunSession(session: RunSessionEntity) {
        runSessionDao.updateSession(session)
    }

    override suspend fun endRunSession(
        sessionId: String,
        distanceMeters: Double,
        durationSeconds: Long,
        avgSpeedMps: Double,
        avgPaceMinPerKm: Double,
        status: String,
        syncStatus: String,
        isOffline: Boolean,
        validationPassed: Boolean
    ) {
        val existing = runSessionDao.getSessionById(sessionId)
        if (existing != null) {
            val updated = existing.copy(
                endTime = System.currentTimeMillis(),
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                avgSpeedMps = avgSpeedMps,
                avgPaceMinPerKm = avgPaceMinPerKm,
                status = status,
                syncStatus = syncStatus,
                isOffline = isOffline,
                validationPassed = validationPassed
            )
            runSessionDao.updateSession(updated)
            
            // Queue for sync
            appDatabase?.syncQueueDao()?.enqueue(
                SyncQueueEntity(
                    actionType = "COMPLETE_SESSION",
                    payloadJson = JSONObject().apply {
                        put("sessionId", sessionId)
                        put("distanceMeters", distanceMeters)
                        put("durationSeconds", durationSeconds)
                        put("avgPaceMinPerKm", avgPaceMinPerKm)
                    }.toString(),
                    status = "PENDING"
                )
            )
        }
    }

    override suspend fun getActiveSession(): RunSessionEntity? {
        return runSessionDao.getActiveSession()
    }

    override suspend fun getActiveOrPausedSession(): RunSessionEntity? {
        return runSessionDao.getActiveOrPausedSession()
    }

    override fun observeActiveSession(): Flow<RunSessionEntity?> {
        return runSessionDao.observeActiveSession()
    }

    override suspend fun getSessionById(sessionId: String): RunSessionEntity? {
        return runSessionDao.getSessionById(sessionId)
    }

    override suspend fun updateSessionStatus(sessionId: String, status: String) {
        val existing = runSessionDao.getSessionById(sessionId)
        if (existing != null) {
            runSessionDao.updateSession(existing.copy(status = status))
        }
    }

    override suspend fun updateSyncStatus(sessionId: String, syncStatus: String) {
        runSessionDao.updateSyncStatus(sessionId, syncStatus)
    }

    override suspend fun getPendingSyncSessions(): List<RunSessionEntity> {
        return runSessionDao.getPendingSyncSessions()
    }

    private fun LocationPointEntity.toDomain(): GpsPoint {
        return GpsPoint(
            id = id,
            sessionId = sessionId,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speed = speed,
            accuracy = accuracy,
            heading = heading,
            timestamp = timestamp
        )
    }
}
