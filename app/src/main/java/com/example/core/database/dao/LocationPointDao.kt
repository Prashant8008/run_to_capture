package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: LocationPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<LocationPointEntity>)

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPointsForSession(sessionId: String): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsListForSession(sessionId: String): List<LocationPointEntity>

    @Query("SELECT COUNT(*) FROM location_points WHERE sessionId = :sessionId")
    fun getPointCountForSession(sessionId: String): Flow<Int>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPointForSession(sessionId: String): LocationPointEntity?

    @Query("DELETE FROM location_points WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: String)

    @Query("DELETE FROM location_points")
    suspend fun deleteAllPoints()
}
