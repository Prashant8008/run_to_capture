package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.BattleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattle(battle: BattleEntity)

    @Update
    suspend fun updateBattle(battle: BattleEntity)

    @Query("SELECT * FROM battles WHERE battleId = :battleId LIMIT 1")
    suspend fun getBattleById(battleId: String): BattleEntity?

    @Query("SELECT * FROM battles WHERE status IN ('PENDING', 'ACTIVE', 'CHALLENGE_IN_PROGRESS')")
    suspend fun getActiveBattles(): List<BattleEntity>

    @Query("SELECT * FROM battles WHERE status IN ('PENDING', 'ACTIVE', 'CHALLENGE_IN_PROGRESS')")
    fun observeActiveBattles(): Flow<List<BattleEntity>>

    @Query("SELECT * FROM battles WHERE territoryId = :territoryId AND status IN ('PENDING', 'ACTIVE', 'CHALLENGE_IN_PROGRESS') LIMIT 1")
    suspend fun getActiveBattleForTerritory(territoryId: String): BattleEntity?

    @Query("DELETE FROM battles")
    suspend fun deleteAllBattles()
}
