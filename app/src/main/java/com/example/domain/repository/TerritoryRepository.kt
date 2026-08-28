package com.example.domain.repository

import com.example.core.database.entity.TerritoryEntity
import com.example.domain.model.DevTerritory
import com.example.domain.model.Faction
import com.example.domain.model.GpsPoint
import com.example.domain.model.ExpansionPreviewResult
import com.example.domain.model.ServerConfirmedExpansion
import com.example.domain.model.TerritoryExpansionRuleConfig
import kotlinx.coroutines.flow.Flow

interface TerritoryRepository {
    fun observeAllTerritories(): Flow<List<TerritoryEntity>>
    fun observeAllDevTerritories(): Flow<List<DevTerritory>>
    fun observeUserTerritories(userId: String): Flow<List<TerritoryEntity>>
    suspend fun getTerritoriesForUser(userId: String): List<TerritoryEntity>
    suspend fun getTerritoryById(territoryId: String): TerritoryEntity?
    suspend fun getAllDevTerritories(): List<DevTerritory>

    // Server Configuration
    fun getServerConfig(): TerritoryExpansionRuleConfig
    fun updateServerConfig(config: TerritoryExpansionRuleConfig)

    // Phase 9 Territory Expansion Pipeline
    suspend fun calculateExpansionPreview(
        sessionId: String,
        userId: String,
        points: List<GpsPoint>
    ): ExpansionPreviewResult

    suspend fun confirmTerritoryExpansion(
        preview: ExpansionPreviewResult,
        userId: String,
        displayName: String,
        faction: Faction
    ): ServerConfirmedExpansion

    suspend fun saveConfirmedTerritory(expansion: ServerConfirmedExpansion)
    suspend fun seedInitialSectorsIfEmpty()
    suspend fun seedMockTerritories(territories: List<DevTerritory>)
    suspend fun clearAllTerritories()
}
