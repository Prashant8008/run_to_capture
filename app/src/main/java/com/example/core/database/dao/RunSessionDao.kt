package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RunSessionEntity)

    @Update
    suspend fun updateSession(session: RunSessionEntity)

    @Query("SELECT * FROM run_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RunSessionEntity>>

    @Query("SELECT * FROM run_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): RunSessionEntity?

    @Query("SELECT * FROM run_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): RunSessionEntity?

    @Query("SELECT * FROM run_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): Flow<RunSessionEntity?>

    @Query("SELECT * FROM run_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveOrPausedSession(): RunSessionEntity?

    @Query("SELECT * FROM run_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startTime DESC LIMIT 1")
    fun observeActiveOrPausedSession(): Flow<RunSessionEntity?>

    @Query("SELECT * FROM run_sessions WHERE syncStatus = 'PENDING_SYNC' OR syncStatus = 'OFFLINE_SAVED' ORDER BY startTime DESC")
    suspend fun getPendingSyncSessions(): List<RunSessionEntity>

    @Query("UPDATE run_sessions SET syncStatus = :syncStatus WHERE sessionId = :sessionId")
    suspend fun updateSyncStatus(sessionId: String, syncStatus: String)

    @Query("DELETE FROM run_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM run_sessions")
    suspend fun deleteAllSessions()
}
