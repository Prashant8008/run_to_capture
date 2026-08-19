package com.example.domain.repository

import com.example.domain.model.AttackValidationResult
import com.example.domain.model.BattleChallengeEvaluation
import com.example.domain.model.BattleSession
import com.example.domain.model.Faction
import com.example.domain.model.TerritoryDefenseRecord
import kotlinx.coroutines.flow.Flow

interface BattleRepository {
    fun observeActiveBattles(): Flow<List<BattleSession>>
    suspend fun getActiveBattleForTerritory(territoryId: String): BattleSession?
    suspend fun getBattleById(battleId: String): BattleSession?
    
    suspend fun validateAttackEligibility(
        territoryId: String,
        targetTerritoryOwnerId: String,
        targetTerritoryFaction: Faction,
        attackerUserId: String,
        attackerFaction: Faction
    ): AttackValidationResult

    suspend fun initiateBattle(
        territoryId: String,
        territoryName: String,
        territoryAreaSqMeters: Double,
        defenderUserId: String,
        defenderDisplayName: String,
        defenderFaction: Faction,
        defenderColorHex: String,
        attackerUserId: String,
        attackerDisplayName: String,
        attackerFaction: Faction,
        defenseRecord: TerritoryDefenseRecord = TerritoryDefenseRecord()
    ): BattleSession

    suspend fun evaluateBattleOutcome(
        battleId: String,
        distanceCompletedMeters: Double,
        paceAchievedMinPerKm: Double,
        elapsedSeconds: Long
    ): BattleChallengeEvaluation
    
    suspend fun saveBattle(battle: BattleSession)
}
