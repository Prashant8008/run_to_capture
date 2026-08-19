package com.example.core.territory

import com.example.domain.model.AttackRejectionReason
import com.example.domain.model.AttackValidationResult
import com.example.domain.model.BattleChallengeEvaluation
import com.example.domain.model.BattleSession
import com.example.domain.model.BattleStatus
import com.example.domain.model.ChallengeRequirement
import com.example.domain.model.ChallengeType
import com.example.domain.model.Faction
import com.example.domain.model.TerritoryDefenseRecord
import java.security.MessageDigest
import java.util.UUID

/**
 * Phase 10 Battle Engine.
 * Evaluates attack eligibility, generates dynamic challenges based on territory defense, and handles outcome validation.
 */
class BattleEngine {

    /**
     * Validates if a player is eligible to attack a specific territory.
     */
    fun validateAttackEligibility(
        targetTerritoryOwnerId: String,
        targetTerritoryFaction: Faction,
        attackerUserId: String,
        attackerFaction: Faction,
        isTerritoryCurrentlyUnderAttack: Boolean
    ): AttackValidationResult {
        if (targetTerritoryOwnerId == attackerUserId) {
            return AttackValidationResult(false, AttackRejectionReason.OWN_TERRITORY)
        }
        
        if (targetTerritoryFaction == attackerFaction) {
            return AttackValidationResult(false, AttackRejectionReason.SAME_FACTION)
        }
        
        if (isTerritoryCurrentlyUnderAttack) {
            return AttackValidationResult(false, AttackRejectionReason.DUPLICATE_ACTIVE_BATTLE)
        }
        
        return AttackValidationResult(true)
    }

    /**
     * Generates a dynamic challenge based on the territory's area and defense record.
     */
    fun generateChallengeForTerritory(
        territoryAreaSqMeters: Double,
        defenseRecord: TerritoryDefenseRecord
    ): ChallengeRequirement {
        // Base distance ~1km for small territories, scaling up to 5km for massive ones
        val baseDistance = 1000.0 + (territoryAreaSqMeters / 10000.0) * 500.0
        val targetDistance = baseDistance.coerceIn(1000.0, 5000.0)
        
        // Defense multiplier makes the challenge harder (faster pace or less time)
        // Shield range: 0-100. 100 = hardest.
        val shieldFactor = (defenseRecord.fortificationShield / 100.0).coerceIn(0.1, 1.0)
        
        // Base pace: 7:00 min/km (easy) to 4:30 min/km (hard)
        val targetPaceMinPerKm = 7.0 - (shieldFactor * 2.5) 
        
        // Select random challenge type based on defense traits
        val challengeTypes = ChallengeType.values()
        val selectedType = challengeTypes[(Math.random() * challengeTypes.size).toInt()]
        
        val timeLimitSeconds = ((targetDistance / 1000.0) * targetPaceMinPerKm * 60).toLong()

        val desc = when (selectedType) {
            ChallengeType.PACE_SPRINT -> "Maintain a pace strictly under %.2f min/km for %.1f km to break the sector's shield.".format(targetPaceMinPerKm, targetDistance / 1000.0)
            ChallengeType.ENDURANCE_DISTANCE -> "Cover %.1f km within %d minutes to overload the territory defenses.".format(targetDistance / 1000.0, timeLimitSeconds / 60)
            ChallengeType.TIMED_ASSAULT -> "Assault protocol active. Complete %.1f km before the %d minute countdown expires.".format(targetDistance / 1000.0, timeLimitSeconds / 60)
            ChallengeType.HEX_DOMINATION -> "Traverse %.1f km across the sector grid within %d minutes to establish dominance.".format(targetDistance / 1000.0, timeLimitSeconds / 60)
        }

        return ChallengeRequirement(
            type = selectedType,
            targetDistanceMeters = targetDistance,
            minPaceMinPerKm = targetPaceMinPerKm,
            timeLimitSeconds = timeLimitSeconds,
            description = desc,
            difficultyBadge = if (shieldFactor > 0.8) "ELITE TACTICAL" else if (shieldFactor > 0.5) "VETERAN" else "STANDARD"
        )
    }

    /**
     * Initializes a new Battle Session against a territory.
     */
    fun initiateBattle(
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
    ): BattleSession {
        val challenge = generateChallengeForTerritory(territoryAreaSqMeters, defenseRecord)
        
        val battleId = "btl_${UUID.randomUUID().toString().take(12)}"
        
        // Battle expires in 15 minutes if not started/completed
        val expiresAt = System.currentTimeMillis() + (15 * 60 * 1000L)
        
        val rawPayload = "$battleId:$territoryId:$attackerUserId"
        val signature = sha256Hex("RUN2CAPTURE-BATTLE-V1:$rawPayload")
        
        return BattleSession(
            battleId = battleId,
            territoryId = territoryId,
            territoryName = territoryName,
            defenderUserId = defenderUserId,
            defenderDisplayName = defenderDisplayName,
            defenderFaction = defenderFaction,
            defenderColorHex = defenderColorHex,
            defenderRecord = defenseRecord,
            attackerUserId = attackerUserId,
            attackerDisplayName = attackerDisplayName,
            attackerFaction = attackerFaction,
            challenge = challenge,
            status = BattleStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            expiresAt = expiresAt,
            serverSignature = signature,
            territoryAreaSqMeters = territoryAreaSqMeters
        )
    }

    /**
     * Evaluates a completed run against the battle's challenge requirements.
     */
    fun evaluateBattleOutcome(
        battle: BattleSession,
        distanceCompletedMeters: Double,
        paceAchievedMinPerKm: Double,
        elapsedSeconds: Long
    ): BattleChallengeEvaluation {
        if (battle.isExpired) {
             return BattleChallengeEvaluation(
                battleId = battle.battleId,
                isPassed = false,
                distanceCompletedMeters = distanceCompletedMeters,
                paceAchievedMinPerKm = paceAchievedMinPerKm,
                elapsedSeconds = elapsedSeconds,
                summaryNotes = "Battle expired before completion."
            )
        }

        val challenge = battle.challenge
        var isPassed = true
        val notes = mutableListOf<String>()

        if (distanceCompletedMeters < challenge.targetDistanceMeters) {
            isPassed = false
            notes.add("Failed: Did not reach target distance of %.1f km.".format(challenge.targetDistanceMeters / 1000.0))
        }

        if (elapsedSeconds > challenge.timeLimitSeconds) {
            isPassed = false
            notes.add("Failed: Exceeded time limit of %d minutes.".format(challenge.timeLimitSeconds / 60))
        }

        if (paceAchievedMinPerKm > challenge.minPaceMinPerKm) {
            isPassed = false
            notes.add("Failed: Pace was too slow (Required: sub %.1f min/km).".format(challenge.minPaceMinPerKm))
        }

        if (isPassed) {
            notes.add("Success: Sector defenses compromised. Territory captured.")
        }

        return BattleChallengeEvaluation(
            battleId = battle.battleId,
            isPassed = isPassed,
            distanceCompletedMeters = distanceCompletedMeters,
            paceAchievedMinPerKm = paceAchievedMinPerKm,
            elapsedSeconds = elapsedSeconds,
            summaryNotes = notes.joinToString(" ")
        )
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
