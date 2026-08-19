package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.TerritoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerritoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritory(territory: TerritoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritories(territories: List<TerritoryEntity>)

    @Update
    suspend fun updateTerritory(territory: TerritoryEntity)

    @Query("SELECT * FROM territories ORDER BY capturedAt DESC")
    fun getAllTerritories(): Flow<List<TerritoryEntity>>

    @Query("SELECT * FROM territories WHERE id = :territoryId LIMIT 1")
    suspend fun getTerritoryById(territoryId: String): TerritoryEntity?

    @Query("SELECT * FROM territories WHERE ownerUserId = :userId ORDER BY capturedAt DESC")
    suspend fun getTerritoriesForUser(userId: String): List<TerritoryEntity>

    @Query("SELECT * FROM territories WHERE ownerUserId = :userId ORDER BY capturedAt DESC")
    fun observeTerritoriesForUser(userId: String): Flow<List<TerritoryEntity>>

    @Query("SELECT COUNT(*) FROM territories WHERE ownerUserId = :userId")
    fun countTerritoriesForUser(userId: String): Flow<Int>

    @Query("DELETE FROM territories WHERE id = :territoryId")
    suspend fun deleteTerritory(territoryId: String)

    @Query("DELETE FROM territories")
    suspend fun deleteAllTerritories()
}
