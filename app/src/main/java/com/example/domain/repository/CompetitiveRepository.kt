package com.example.domain.repository

import com.example.domain.model.Challenge
import com.example.domain.model.ChallengeCondition
import com.example.domain.model.LeaderboardCategory
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.LeaderboardPeriod
import kotlinx.coroutines.flow.Flow

interface CompetitiveRepository {
    fun getLeaderboard(
        category: LeaderboardCategory,
        period: LeaderboardPeriod
    ): Flow<List<LeaderboardEntry>>

    fun getActiveChallenges(): Flow<List<Challenge>>

    suspend fun updateChallengeProgress(
        userId: String,
        condition: ChallengeCondition,
        amount: Double
    )
}
