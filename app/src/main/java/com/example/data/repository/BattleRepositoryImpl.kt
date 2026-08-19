package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.database.AppDatabase
import com.example.core.database.dao.BattleDao
import com.example.core.database.dao.TerritoryDao
import com.example.core.database.entity.BattleEntity
import com.example.core.territory.BattleEngine
import com.example.domain.model.AttackValidationResult
import com.example.domain.model.BattleChallengeEvaluation
import com.example.domain.model.BattleSession
import com.example.domain.model.BattleStatus
import com.example.domain.model.ChallengeRequirement
import com.example.domain.model.ChallengeType
import com.example.domain.model.Faction
import com.example.domain.model.TerritoryDefenseRecord
import com.example.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

import com.example.core.database.entity.SyncQueueEntity
import org.json.JSONObject

class BattleRepositoryImpl(
    private val battleDao: BattleDao,
    private val territoryDao: TerritoryDao,
    private val database: AppDatabase,
    private val authRepository: com.example.domain.repository.AuthRepository,
    private val competitiveRepository: com.example.domain.repository.CompetitiveRepository? = null,
    private val notificationRepository: com.example.domain.repository.NotificationRepository? = null,
    private val battleEngine: BattleEngine = BattleEngine()
) : BattleRepository {

    override fun observeActiveBattles(): Flow<List<BattleSession>> {
        return battleDao.observeActiveBattles().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getActiveBattleForTerritory(territoryId: String): BattleSession? {
        return battleDao.getActiveBattleForTerritory(territoryId)?.toDomainModel()
    }

    override suspend fun getBattleById(battleId: String): BattleSession? {
        return battleDao.getBattleById(battleId)?.toDomainModel()
    }

    override suspend fun validateAttackEligibility(
        territoryId: String,
        targetTerritoryOwnerId: String,
        targetTerritoryFaction: Faction,
        attackerUserId: String,
        attackerFaction: Faction
    ): AttackValidationResult {
        val existingBattle = battleDao.getActiveBattleForTerritory(territoryId)
        val isCurrentlyUnderAttack = existingBattle != null && existingBattle.status in listOf("PENDING", "ACTIVE", "CHALLENGE_IN_PROGRESS")
        
        return battleEngine.validateAttackEligibility(
            targetTerritoryOwnerId = targetTerritoryOwnerId,
            targetTerritoryFaction = targetTerritoryFaction,
            attackerUserId = attackerUserId,
            attackerFaction = attackerFaction,
            isTerritoryCurrentlyUnderAttack = isCurrentlyUnderAttack
        )
    }

    override suspend fun initiateBattle(
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
        defenseRecord: TerritoryDefenseRecord
    ): BattleSession {
        val battle = battleEngine.initiateBattle(
            territoryId = territoryId,
            territoryName = territoryName,
            territoryAreaSqMeters = territoryAreaSqMeters,
            defenderUserId = defenderUserId,
            defenderDisplayName = defenderDisplayName,
            defenderFaction = defenderFaction,
            defenderColorHex = defenderColorHex,
            attackerUserId = attackerUserId,
            attackerDisplayName = attackerDisplayName,
            attackerFaction = attackerFaction,
            defenseRecord = defenseRecord
        )
        
        battleDao.insertBattle(battle.toEntity())
        return battle
    }

    override suspend fun evaluateBattleOutcome(
        battleId: String,
        distanceCompletedMeters: Double,
        paceAchievedMinPerKm: Double,
        elapsedSeconds: Long
    ): BattleChallengeEvaluation {
        val entity = battleDao.getBattleById(battleId) ?: throw IllegalArgumentException("Battle not found")
        val battle = entity.toDomainModel()
        
        val evaluation = battleEngine.evaluateBattleOutcome(
            battle = battle,
            distanceCompletedMeters = distanceCompletedMeters,
            paceAchievedMinPerKm = paceAchievedMinPerKm,
            elapsedSeconds = elapsedSeconds
        )
        
        val newStatus = if (evaluation.isPassed) BattleStatus.VICTORY else BattleStatus.DEFEAT
        
        database.withTransaction {
            val updatedEntity = entity.copy(
                status = newStatus.name,
                completedAt = System.currentTimeMillis()
            )
            battleDao.updateBattle(updatedEntity)
            
            // Queue battle outcome for sync
            database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    actionType = "BATTLE_RESULT",
                    payloadJson = JSONObject().apply {
                        put("battleId", battleId)
                        put("status", newStatus.name)
                        put("distanceMeters", distanceCompletedMeters)
                        put("pace", paceAchievedMinPerKm)
                        put("elapsedSeconds", elapsedSeconds)
                    }.toString(),
                    status = "PENDING"
                )
            )
            
            if (evaluation.isPassed) {
                val territory = territoryDao.getTerritoryById(battle.territoryId)
                if (territory != null) {
                    val capturedTerritory = territory.copy(
                        ownerUserId = battle.attackerUserId,
                        ownerDisplayName = battle.attackerDisplayName,
                        faction = battle.attackerFaction.name,
                        capturedAt = System.currentTimeMillis(),
                        defenseLevel = 100 // Reset defense shield upon capture
                    )
                    territoryDao.updateTerritory(capturedTerritory)
                    
                    // Award XP and stats to the current user if they are the attacker
                    authRepository.awardProgression(
                        sources = listOf(
                            Pair(com.example.core.progression.XpSource.VALID_RUN, 1),
                            Pair(com.example.core.progression.XpSource.DISTANCE_KM, (distanceCompletedMeters / 1000).toInt()),
                            Pair(com.example.core.progression.XpSource.CAPTURE, 1),
                            Pair(com.example.core.progression.XpSource.CHALLENGE, 1)
                        ),
                        newAreaSqMeters = territory.areaSqMeters,
                        newDistanceMeters = distanceCompletedMeters,
                        territoriesCaptured = 1
                    )
                    
                    competitiveRepository?.updateChallengeProgress(
                        userId = battle.attackerUserId,
                        condition = com.example.domain.model.ChallengeCondition.CAPTURE_TERRITORY,
                        amount = 1.0
                    )
                    
                    competitiveRepository?.updateChallengeProgress(
                        userId = battle.attackerUserId,
                        condition = com.example.domain.model.ChallengeCondition.DISTANCE_KM,
                        amount = distanceCompletedMeters / 1000.0
                    )
                    
                    notificationRepository?.sendNotification(
                        type = com.example.domain.model.NotificationType.TERRITORY_CAPTURED,
                        title = "Territory Captured!",
                        message = "You have successfully captured ${battle.territoryName}.",
                        actionUrl = "run2capture://map?territoryId=${territory.id}"
                    )
                    
                    // Simulate enemy losing territory via system notification
                    if (battle.defenderUserId != "SERVER") {
                        notificationRepository?.sendNotification(
                            type = com.example.domain.model.NotificationType.TERRITORY_LOST,
                            title = "Territory Lost!",
                            message = "Your territory ${battle.territoryName} was captured by ${battle.attackerDisplayName}.",
                            actionUrl = "run2capture://map?territoryId=${territory.id}"
                        )
                    }
                }
            } else {
                // Defense successful - we can increase defense level or something, but the prompt says:
                // "If defense succeeds: territory remains owned."
                if (battle.defenderUserId != "SERVER") {
                    notificationRepository?.sendNotification(
                        type = com.example.domain.model.NotificationType.DEFENSE_SUCCESSFUL,
                        title = "Defense Successful!",
                        message = "Your territory defense held against an attack from ${battle.attackerDisplayName}.",
                        actionUrl = "run2capture://map?territoryId=${battle.territoryId}"
                    )
                }
            }
        }
        
        return evaluation
    }

    override suspend fun saveBattle(battle: BattleSession) {
        battleDao.insertBattle(battle.toEntity())
    }

    private fun BattleEntity.toDomainModel(): BattleSession {
        return BattleSession(
            battleId = battleId,
            territoryId = territoryId,
            territoryName = territoryName,
            defenderUserId = defenderUserId,
            defenderDisplayName = defenderDisplayName,
            defenderFaction = Faction.valueOf(defenderFaction),
            defenderColorHex = defenderColorHex,
            defenderFlagSvg = defenderFlagSvg,
            defenderRecord = TerritoryDefenseRecord(), // Load real if implemented
            attackerUserId = attackerUserId,
            attackerDisplayName = attackerDisplayName,
            attackerFaction = Faction.valueOf(attackerFaction),
            challenge = ChallengeRequirement(
                type = ChallengeType.valueOf(challengeType),
                targetDistanceMeters = targetDistanceMeters,
                minPaceMinPerKm = minPaceMinPerKm,
                timeLimitSeconds = timeLimitSeconds,
                description = challengeDescription
            ),
            status = BattleStatus.valueOf(status),
            createdAt = createdAt,
            expiresAt = expiresAt,
            completedAt = completedAt,
            serverSignature = serverSignature,
            territoryAreaSqMeters = territoryAreaSqMeters
        )
    }

    private fun BattleSession.toEntity(): BattleEntity {
        return BattleEntity(
            battleId = battleId,
            territoryId = territoryId,
            territoryName = territoryName,
            defenderUserId = defenderUserId,
            defenderDisplayName = defenderDisplayName,
            defenderFaction = defenderFaction.name,
            defenderColorHex = defenderColorHex,
            defenderFlagSvg = defenderFlagSvg,
            attackerUserId = attackerUserId,
            attackerDisplayName = attackerDisplayName,
            attackerFaction = attackerFaction.name,
            challengeType = challenge.type.name,
            targetDistanceMeters = challenge.targetDistanceMeters,
            minPaceMinPerKm = challenge.minPaceMinPerKm,
            timeLimitSeconds = challenge.timeLimitSeconds,
            challengeDescription = challenge.description,
            status = status.name,
            createdAt = createdAt,
            expiresAt = expiresAt,
            completedAt = completedAt,
            serverSignature = serverSignature,
            territoryAreaSqMeters = territoryAreaSqMeters
        )
    }
}
