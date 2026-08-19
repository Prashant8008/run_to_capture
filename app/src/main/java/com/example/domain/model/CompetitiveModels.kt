package com.example.domain.model

enum class LeaderboardCategory(val displayName: String) {
    TERRITORY("Territory Area"),
    CAPTURES("Captures"),
    DEFENSES("Defenses"),
    WINS("Battle Wins"),
    DISTANCE("Distance Run")
}

enum class LeaderboardPeriod(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ALL_TIME("All Time")
}

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val faction: Faction,
    val flagConfig: FlagConfig?,
    val score: Double,
    val formattedScore: String
)

enum class CompetitiveChallengeType(val displayName: String) {
    DAILY("Daily Challenge"),
    WEEKLY("Weekly Challenge"),
    SPECIAL("Special Challenge")
}

enum class ChallengeCondition {
    DISTANCE_KM,
    CAPTURE_CELLS,
    DEFEND_TERRITORY,
    CAPTURE_TERRITORY
}

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val type: CompetitiveChallengeType,
    val condition: ChallengeCondition,
    val targetValue: Double,
    val currentProgress: Double,
    val isCompleted: Boolean,
    val rewardXp: Long
)
