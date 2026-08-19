package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class ChallengeType(val title: String, val icon: String) {
    PACE_SPRINT("TACTICAL SPRINT", "⚡"),
    ENDURANCE_DISTANCE("DISTANCE INFILTRATION", "🏃"),
    TIMED_ASSAULT("OVERDRIVE ASSAULT", "⏱"),
    HEX_DOMINATION("HEX OVERDRIVE", "◈")
}

@JsonClass(generateAdapter = true)
data class ChallengeRequirement(
    val type: ChallengeType,
    val targetDistanceMeters: Double,
    val minPaceMinPerKm: Double, // Maximum allowed min/km (lower is faster)
    val timeLimitSeconds: Long,
    val description: String,
    val difficultyBadge: String = "ELITE TACTICAL"
) {
    val formattedTargetDistance: String get() = "%.2f km".format(targetDistanceMeters / 1000.0)
    val formattedPaceRequirement: String get() = "Sub %d:%02d /km".format(
        minPaceMinPerKm.toInt(),
        ((minPaceMinPerKm - minPaceMinPerKm.toInt()) * 60).toInt()
    )
    val formattedTimeLimit: String get() = "%d mins".format(timeLimitSeconds / 60)
}

@JsonClass(generateAdapter = true)
data class TerritoryDefenseRecord(
    val totalBattles: Int = 12,
    val victories: Int = 9,
    val defeats: Int = 3,
    val currentDefenseStreak: Int = 4,
    val lastDefendedTimestamp: Long = System.currentTimeMillis() - 86400000L,
    val fortificationShield: Int = 85
) {
    val winRatePercent: Int get() = if (totalBattles > 0) ((victories.toDouble() / totalBattles) * 100).toInt() else 100
}

enum class BattleStatus {
    PENDING,
    ACTIVE,
    CHALLENGE_IN_PROGRESS,
    VICTORY,
    DEFEAT,
    EXPIRED,
    CANCELLED
}

enum class AttackRejectionReason(val label: String, val message: String) {
    OWN_TERRITORY("FRIENDLY SECTOR", "You cannot attack your own territory."),
    SAME_FACTION("ALLIED SECTOR", "Territory is held by an allied syndicate operative."),
    DUPLICATE_ACTIVE_BATTLE("RAID IN PROGRESS", "An active assault is already in progress on this sector."),
    TERRITORY_NOT_FOUND("INVALID SECTOR", "Target territory could not be located on the neural grid."),
    TERRITORY_SHIELDED("SHIELD OVERLOAD", "Territory is currently under electromagnetic lockdown."),
    CONCURRENT_LOCK("CONCURRENCY CONFLICT", "Another operative initiated an attack simultaneously.")
}

@JsonClass(generateAdapter = true)
data class AttackValidationResult(
    val isEligible: Boolean,
    val rejectionReason: AttackRejectionReason? = null,
    val message: String = ""
)

@JsonClass(generateAdapter = true)
data class BattleSession(
    val battleId: String,
    val territoryId: String,
    val territoryName: String,
    val defenderUserId: String,
    val defenderDisplayName: String,
    val defenderFaction: Faction,
    val defenderColorHex: String,
    val defenderFlagSvg: String? = null,
    val defenderRecord: TerritoryDefenseRecord = TerritoryDefenseRecord(),
    val attackerUserId: String,
    val attackerDisplayName: String,
    val attackerFaction: Faction,
    val challenge: ChallengeRequirement,
    val status: BattleStatus = BattleStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (15 * 60 * 1000L),
    val completedAt: Long? = null,
    val serverSignature: String = "",
    val territoryAreaSqMeters: Double = 25000.0
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt && status == BattleStatus.ACTIVE
    val remainingSeconds: Long get() = ((expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
    val formattedTimeRemaining: String
        get() {
            val s = remainingSeconds
            return "%02d:%02d".format(s / 60, s % 60)
        }
}

@JsonClass(generateAdapter = true)
data class BattleChallengeEvaluation(
    val battleId: String,
    val isPassed: Boolean,
    val distanceCompletedMeters: Double,
    val paceAchievedMinPerKm: Double,
    val elapsedSeconds: Long,
    val summaryNotes: String
)
