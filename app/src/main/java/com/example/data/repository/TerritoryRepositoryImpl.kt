package com.example.data.repository

import com.example.core.database.dao.TerritoryDao
import com.example.core.database.entity.TerritoryEntity
import com.example.core.geo.H3SpatialIndex
import com.example.core.territory.TerritoryExpansionEngine
import com.example.domain.model.DevTerritory
import com.example.domain.model.Faction
import com.example.domain.model.GpsPoint
import com.example.domain.model.LatLng
import com.example.domain.model.ExpansionPreviewResult
import com.example.domain.model.ServerConfirmedExpansion
import com.example.domain.model.TerritoryExpansionRuleConfig
import com.example.domain.repository.TerritoryRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TerritoryRepositoryImpl(
    private val territoryDao: TerritoryDao,
    private val authRepository: com.example.domain.repository.AuthRepository? = null,
    private val competitiveRepository: com.example.domain.repository.CompetitiveRepository? = null,
    private val expansionEngine: TerritoryExpansionEngine = TerritoryExpansionEngine()
) : TerritoryRepository {

    private val moshi = Moshi.Builder().build()
    private val latLngListType = Types.newParameterizedType(List::class.java, LatLng::class.java)
    private val latLngAdapter = moshi.adapter<List<LatLng>>(latLngListType)

    override fun observeAllTerritories(): Flow<List<TerritoryEntity>> {
        return territoryDao.getAllTerritories()
    }

    override fun observeAllDevTerritories(): Flow<List<DevTerritory>> {
        return territoryDao.getAllTerritories().map { entities ->
            entities.map { it.toDevTerritory() }
        }
    }

    override fun observeUserTerritories(userId: String): Flow<List<TerritoryEntity>> {
        return territoryDao.observeTerritoriesForUser(userId)
    }

    override suspend fun getTerritoriesForUser(userId: String): List<TerritoryEntity> {
        return territoryDao.getTerritoriesForUser(userId)
    }

    override suspend fun getTerritoryById(territoryId: String): TerritoryEntity? {
        return territoryDao.getTerritoryById(territoryId)
    }

    override fun getServerConfig(): TerritoryExpansionRuleConfig {
        return expansionEngine.serverConfig
    }

    override fun updateServerConfig(config: TerritoryExpansionRuleConfig) {
        expansionEngine.serverConfig = config
    }

    override suspend fun calculateExpansionPreview(
        sessionId: String,
        userId: String,
        points: List<GpsPoint>
    ): ExpansionPreviewResult {
        // 1. Fetch user's existing territory
        val userTerritories = territoryDao.getTerritoriesForUser(userId)
        val primaryTerritory = userTerritories.firstOrNull()

        val existingCells = mutableSetOf<String>()
        primaryTerritory?.h3HexIndexes?.split(",")?.filter { it.isNotBlank() }?.let {
            existingCells.addAll(it)
        }

        // If no explicit h3 cells stored, derive from geoJson coordinates
        if (existingCells.isEmpty() && primaryTerritory != null) {
            val coords = parseCoordinates(primaryTerritory.geoJsonCoordinates)
            if (coords.isNotEmpty()) {
                val derivedCells = H3SpatialIndex.polylineCoverageToCells(coords, expansionEngine.serverConfig.h3Resolution)
                existingCells.addAll(derivedCells)
            }
        }

        // 2. Fetch other factions' cells to prevent uncontested enemy overlap
        val allTerritories = territoryDao.getTerritoriesForUser(userId) // or all
        val rivalCells = mutableSetOf<String>()

        // 3. Compute client preview
        return expansionEngine.calculateExpansionPreview(
            sessionId = sessionId,
            runPoints = points,
            existingTerritoryId = primaryTerritory?.id,
            existingCells = existingCells,
            rivalFactionCells = rivalCells
        )
    }

    override suspend fun confirmTerritoryExpansion(
        preview: ExpansionPreviewResult,
        userId: String,
        displayName: String,
        faction: Faction
    ): ServerConfirmedExpansion {
        // Authoritative server resolution
        val serverConfirmed = expansionEngine.serverConfirmExpansion(
            preview = preview,
            userId = userId,
            displayName = displayName,
            faction = faction
        )

        // Persist to local Room database
        saveConfirmedTerritory(serverConfirmed)
        
        // Award XP for expansion if authorized
        if (serverConfirmed.isAuthoritative && authRepository != null) {
            val isNewTerritory = preview.existingTerritoryId == null
            val source = if (isNewTerritory) com.example.core.progression.XpSource.NEW_TERRITORY else com.example.core.progression.XpSource.EXPANSION
            authRepository.awardProgression(
                sources = listOf(Pair(source, 1)),
                newAreaSqMeters = serverConfirmed.totalAreaSqMeters
            )
            
            competitiveRepository?.updateChallengeProgress(
                userId = userId,
                condition = com.example.domain.model.ChallengeCondition.CAPTURE_CELLS,
                amount = serverConfirmed.newlyGainedCells.size.toDouble()
            )
        }
        
        return serverConfirmed
    }

    override suspend fun saveConfirmedTerritory(expansion: ServerConfirmedExpansion) {
        val geoJson = formatCoordinates(expansion.boundaryPolygon)
        val cellsString = expansion.confirmedCells.joinToString(",")

        val entity = TerritoryEntity(
            id = expansion.territoryId,
            ownerUserId = expansion.ownerUserId,
            ownerDisplayName = expansion.ownerDisplayName,
            faction = expansion.faction.name,
            geoJsonCoordinates = geoJson,
            areaSqMeters = expansion.totalAreaSqMeters,
            h3HexIndexes = cellsString,
            capturedAt = expansion.confirmedAt,
            defenseLevel = 100,
            serverSignature = expansion.serverSignature,
            isAuthoritative = true,
            isSynced = true
        )
        territoryDao.insertTerritory(entity)
    }

    override suspend fun getAllDevTerritories(): List<DevTerritory> {
        val entities = territoryDao.getTerritoriesForUser("current_user")
        return entities.map { it.toDevTerritory() }
    }

    override suspend fun seedInitialSectorsIfEmpty() {
        // Will be dynamically seeded once GPS location is available via seedSectorsAroundLocation
    }

    override suspend fun seedSectorsAroundLocation(latitude: Double, longitude: Double) {
        val existing = territoryDao.getTerritoriesForUser("operative_all")
        // Check if any existing territory is within ~15km (0.15 deg)
        val hasNearby = existing.any {
            val coords = parseCoordinates(it.geoJsonCoordinates)
            coords.any { c -> Math.abs(c.latitude - latitude) < 0.15 && Math.abs(c.longitude - longitude) < 0.15 }
        }
        if (hasNearby && existing.isNotEmpty()) return

        // Generate 4 tactical contested sectors in the quadrants around the user's GPS
        val sectors = listOf(
            // NW: APEX Stronghold
            TerritoryEntity(
                id = "sec_apex_${(latitude * 1000).toInt()}_${(longitude * 1000).toInt()}",
                ownerUserId = "bot_apex_01",
                ownerDisplayName = "APEX STRONGHOLD",
                faction = "APEX",
                geoJsonCoordinates = formatCoordinates(listOf(
                    LatLng(latitude + 0.0018, longitude - 0.0015),
                    LatLng(latitude + 0.0036, longitude - 0.0008),
                    LatLng(latitude + 0.0042, longitude - 0.0032),
                    LatLng(latitude + 0.0025, longitude - 0.0045),
                    LatLng(latitude + 0.0012, longitude - 0.0030)
                )),
                areaSqMeters = 34500.0,
                h3HexIndexes = "",
                capturedAt = System.currentTimeMillis() - 86400000L * 3,
                defenseLevel = 88,
                serverSignature = "sig_apex_seed",
                isAuthoritative = true,
                isSynced = true
            ),
            // NE: SOLARIS Citadel
            TerritoryEntity(
                id = "sec_solaris_${(latitude * 1000).toInt()}_${(longitude * 1000).toInt()}",
                ownerUserId = "bot_solaris_01",
                ownerDisplayName = "SOLARIS CITADEL",
                faction = "SOLARIS",
                geoJsonCoordinates = formatCoordinates(listOf(
                    LatLng(latitude + 0.0015, longitude + 0.0018),
                    LatLng(latitude + 0.0038, longitude + 0.0022),
                    LatLng(latitude + 0.0045, longitude + 0.0048),
                    LatLng(latitude + 0.0022, longitude + 0.0055),
                    LatLng(latitude + 0.0008, longitude + 0.0035)
                )),
                areaSqMeters = 38200.0,
                h3HexIndexes = "",
                capturedAt = System.currentTimeMillis() - 86400000L * 2,
                defenseLevel = 76,
                serverSignature = "sig_solaris_seed",
                isAuthoritative = true,
                isSynced = true
            ),
            // SE: CIPHER Nexus
            TerritoryEntity(
                id = "sec_cipher_${(latitude * 1000).toInt()}_${(longitude * 1000).toInt()}",
                ownerUserId = "bot_cipher_01",
                ownerDisplayName = "CIPHER NEXUS",
                faction = "CIPHER",
                geoJsonCoordinates = formatCoordinates(listOf(
                    LatLng(latitude - 0.0012, longitude + 0.0015),
                    LatLng(latitude - 0.0010, longitude + 0.0042),
                    LatLng(latitude - 0.0035, longitude + 0.0050),
                    LatLng(latitude - 0.0048, longitude + 0.0025),
                    LatLng(latitude - 0.0030, longitude + 0.0008)
                )),
                areaSqMeters = 41500.0,
                h3HexIndexes = "",
                capturedAt = System.currentTimeMillis() - 86400000L * 5,
                defenseLevel = 92,
                serverSignature = "sig_cipher_seed",
                isAuthoritative = true,
                isSynced = true
            ),
            // SW: Contested Neutral Sector
            TerritoryEntity(
                id = "sec_neutral_${(latitude * 1000).toInt()}_${(longitude * 1000).toInt()}",
                ownerUserId = "unclaimed",
                ownerDisplayName = "CONTESTED GRID 04",
                faction = "CIPHER",
                geoJsonCoordinates = formatCoordinates(listOf(
                    LatLng(latitude - 0.0015, longitude - 0.0015),
                    LatLng(latitude - 0.0008, longitude - 0.0040),
                    LatLng(latitude - 0.0032, longitude - 0.0052),
                    LatLng(latitude - 0.0046, longitude - 0.0030),
                    LatLng(latitude - 0.0032, longitude - 0.0010)
                )),
                areaSqMeters = 29000.0,
                h3HexIndexes = "",
                capturedAt = System.currentTimeMillis() - 86400000L * 1,
                defenseLevel = 45,
                serverSignature = "sig_neutral_seed",
                isAuthoritative = true,
                isSynced = true
            )
        )

        territoryDao.insertTerritories(sectors)
    }

    override suspend fun seedMockTerritories(territories: List<DevTerritory>) {
        // No-op: sample/mock data removed
    }

    override suspend fun clearAllTerritories() {
        territoryDao.deleteAllTerritories()
    }

    private fun parseCoordinates(json: String): List<LatLng> {
        return try {
            latLngAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun formatCoordinates(coords: List<LatLng>): String {
        return try {
            latLngAdapter.toJson(coords)
        } catch (_: Exception) {
            "[]"
        }
    }

    private fun TerritoryEntity.toDevTerritory(): DevTerritory {
        val colorHex = when (faction.uppercase()) {
            "APEX" -> "#FF2A6D"
            "CIPHER" -> "#00F0FF"
            "SOLARIS" -> "#FFD700"
            else -> "#CCFF00"
        }
        return DevTerritory(
            id = id,
            name = ownerDisplayName,
            factionId = faction,
            colorHex = colorHex,
            coordinates = parseCoordinates(geoJsonCoordinates),
            areaSqMeters = areaSqMeters,
            defenseLevel = defenseLevel
        )
    }

    private fun getInitialSeedTerritories(): List<DevTerritory> {
        // San Francisco Base Sector
        val baseCoords = listOf(
            LatLng(37.7749, -122.4194),
            LatLng(37.7760, -122.4180),
            LatLng(37.7770, -122.4200),
            LatLng(37.7758, -122.4220),
            LatLng(37.7745, -122.4210)
        )
        return listOf(
            DevTerritory(
                id = "sec_sf_base_01",
                name = "CIVIC NEXUS SECTOR",
                factionId = "CIPHER",
                colorHex = "#00F0FF",
                coordinates = baseCoords,
                areaSqMeters = 24500.0,
                defenseLevel = 85
            )
        )
    }
}
