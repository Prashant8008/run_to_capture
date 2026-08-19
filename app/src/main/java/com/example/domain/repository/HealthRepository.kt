package com.example.domain.repository

import com.example.domain.model.HealthState
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    val healthState: Flow<HealthState>
    suspend fun checkHealth(): HealthState
}
