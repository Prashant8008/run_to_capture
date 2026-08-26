package com.example.data.repository

import com.example.domain.model.AuthState
import com.example.domain.model.Challenge
import com.example.domain.model.ChallengeCondition
import com.example.domain.model.CompetitiveChallengeType
import com.example.domain.model.Faction
import com.example.domain.model.LeaderboardCategory
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.LeaderboardPeriod
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CompetitiveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CompetitiveRepositoryImpl(
    private val authRepository: AuthRepository
) : CompetitiveRepository {

    // In-memory mock server state for challenges
    private val _challenges = MutableStateFlow<List<Challenge>>(
        listOf(
            Challenge(
                id = "c_daily_1",
                title = "Daily Recon",
                description = "Run a total of 5 km today.",
                type = CompetitiveChallengeType.DAILY,
                condition = ChallengeCondition.DISTANCE_KM,
                targetValue = 5.0,
                currentProgress = 0.0,
                isCompleted = false,
                rewardXp = 500
            ),
            Challenge(
                id = "c_weekly_1",
                title = "Syndicate Expansion",
                description = "Capture 10 hex cells for your syndicate.",
                type = CompetitiveChallengeType.WEEKLY,
                condition = ChallengeCondition.CAPTURE_CELLS,
                targetValue = 10.0,
                currentProgress = 0.0,
                isCompleted = false,
                rewardXp = 1500
            ),
            Challenge(
                id = "c_special_1",
                title = "Apex Predator",
                description = "Capture an enemy territory.",
                type = CompetitiveChallengeType.SPECIAL,
                condition = ChallengeCondition.CAPTURE_TERRITORY,
                targetValue = 1.0,
                currentProgress = 0.0,
                isCompleted = false,
                rewardXp = 2500
            )
        )
    )

    override fun getLeaderboard(
        category: LeaderboardCategory,
        period: LeaderboardPeriod
    ): Flow<List<LeaderboardEntry>> {
        return authRepository.authState.map { authState ->
            val currentUser = if (authState is AuthState.Authenticated) authState.user else null
            
            // Server-generated mock list
            val serverList = mutableListOf<LeaderboardEntry>()
            
            val mockNames = listOf("ZeroCool", "AcidBurn", "CrashOverride", "CerealKiller", "LordNikon", "Phantom", "Ghost", "Specter")
            val mockFactions = listOf(Faction.CIPHER, Faction.APEX, Faction.SOLARIS)
            
            // Generate some random opponents
            for (i in 0 until 15) {
                val score = when (category) {
                    LeaderboardCategory.TERRITORY -> 10000.0 + (100000.0 * Math.random())
                    LeaderboardCategory.CAPTURES -> 1.0 + (50 * Math.random()).toInt()
                    LeaderboardCategory.DEFENSES -> 0.0 + (20 * Math.random()).toInt()
                    LeaderboardCategory.WINS -> 1.0 + (30 * Math.random()).toInt()
                    LeaderboardCategory.DISTANCE -> 5000.0 + (200000.0 * Math.random())
                }
                
                val formattedScore = when (category) {
                    LeaderboardCategory.TERRITORY -> "%.0f m²".format(score)
                    LeaderboardCategory.DISTANCE -> "%.1f km".format(score / 1000.0)
                    else -> score.toInt().toString()
                }

                serverList.add(
                    LeaderboardEntry(
                        rank = 0, // Will sort later
                        userId = "bot_$i",
                        displayName = mockNames[i % mockNames.size] + "_${10 + i}",
                        avatarUrl = null,
                        faction = mockFactions[i % mockFactions.size],
                        flagConfig = null,
                        score = score,
                        formattedScore = formattedScore
                    )
                )
            }
            
            // Inject current user
            if (currentUser != null) {
                val userScore = when (category) {
                    LeaderboardCategory.TERRITORY -> currentUser.totalAreaSqMeters
                    LeaderboardCategory.CAPTURES -> currentUser.territoriesCapturedCount.toDouble()
                    LeaderboardCategory.DEFENSES -> 0.0 // not tracked currently
                    LeaderboardCategory.WINS -> currentUser.territoriesCapturedCount.toDouble()
                    LeaderboardCategory.DISTANCE -> currentUser.totalDistanceMeters
                }
                
                val userFormattedScore = when (category) {
                    LeaderboardCategory.TERRITORY -> "%.0f m²".format(userScore)
                    LeaderboardCategory.DISTANCE -> "%.1f km".format(userScore / 1000.0)
                    else -> userScore.toInt().toString()
                }
                
                serverList.add(
                    LeaderboardEntry(
                        rank = 0,
                        userId = currentUser.id,
                        displayName = currentUser.displayName,
                        avatarUrl = currentUser.avatarUrl,
                        faction = currentUser.faction,
                        flagConfig = currentUser.flag,
                        score = userScore,
                        formattedScore = userFormattedScore
                    )
                )
            }
            
            // Sort and rank
            serverList.sortByDescending { it.score }
            serverList.mapIndexed { index, entry -> 
                entry.copy(rank = index + 1)
            }
        }
    }

    override fun getActiveChallenges(): Flow<List<Challenge>> {
        return _challenges
    }

    override suspend fun updateChallengeProgress(
        userId: String,
        condition: ChallengeCondition,
        amount: Double
    ) {
        // Server authoritative logic
        val current = _challenges.value.toMutableList()
        var updated = false
        
        for (i in current.indices) {
            val c = current[i]
            if (!c.isCompleted && c.condition == condition) {
                val newProgress = (c.currentProgress + amount).coerceAtMost(c.targetValue)
                val isCompleted = newProgress >= c.targetValue
                
                current[i] = c.copy(
                    currentProgress = newProgress,
                    isCompleted = isCompleted
                )
                updated = true
                
                if (isCompleted) {
                    authRepository.awardProgression(
                        sources = listOf(Pair(com.example.core.progression.XpSource.CHALLENGE, 1))
                    )
                }
            }
        }
        
        if (updated) {
            _challenges.value = current
        }
    }
}
