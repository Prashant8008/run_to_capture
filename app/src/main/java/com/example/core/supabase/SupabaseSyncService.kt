package com.example.core.supabase

import android.util.Log
import com.example.core.database.dao.TerritoryDao
import com.example.core.database.entity.TerritoryEntity
import com.example.core.supabase.model.SupabaseProfile
import com.example.core.supabase.model.SupabaseRun
import com.example.core.supabase.model.SupabaseTerritory
import com.example.domain.model.AuthUser
import com.example.domain.model.RunSessionResult
import com.example.domain.model.ServerConfirmedExpansion
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseSyncService(
    private val territoryDao: TerritoryDao
) {
    private val client = SupabaseClientProvider.client
    private val tag = "SupabaseSyncService"

    suspend fun syncProfile(user: AuthUser): Boolean = withContext(Dispatchers.IO) {
        try {
            val profile = SupabaseProfile(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                faction = user.faction.name,
                avatarUrl = user.avatarUrl,
                territoryColor = user.territoryColor,
                totalAreaSqMeters = user.totalAreaSqMeters,
                totalDistanceMeters = user.totalDistanceMeters,
                territoriesCount = user.territoriesCount,
                xp = user.xp,
                level = user.level
            )
            client.from("profiles").upsert(profile)
            Log.i(tag, "Successfully synced user profile to Supabase: ${user.displayName}")
            true
        } catch (e: Exception) {
            Log.w(tag, "Supabase profile sync failed (offline or table not ready): ${e.message}")
            false
        }
    }

    suspend fun uploadCompletedRun(
        runResult: RunSessionResult,
        user: AuthUser,
        routeJson: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val paceSecs = if (runResult.distanceKm > 0.001) (runResult.durationSeconds / runResult.distanceKm).toLong() else 0L
            val run = SupabaseRun(
                id = runResult.sessionId,
                userId = user.id,
                userDisplayName = user.displayName,
                faction = user.faction.name,
                distanceMeters = runResult.distanceMeters,
                durationSeconds = runResult.durationSeconds,
                avgPaceSecondsPerKm = paceSecs,
                caloriesBurned = runResult.caloriesBurned,
                isClosedLoop = runResult.capturedTerritoriesCount > 0,
                enclosedAreaSqMeters = 0.0,
                routeGeoJson = routeJson
            )
            client.from("runs").upsert(run)
            Log.i(tag, "Successfully uploaded run to Supabase: ${runResult.sessionId}")
            true
        } catch (e: Exception) {
            Log.w(tag, "Supabase run upload failed: ${e.message}")
            false
        }
    }

    suspend fun uploadCapturedTerritory(
        expansion: ServerConfirmedExpansion
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val territory = SupabaseTerritory(
                id = expansion.territoryId,
                ownerUserId = expansion.ownerUserId,
                ownerDisplayName = expansion.ownerDisplayName,
                faction = expansion.faction.name,
                areaSqMeters = expansion.totalAreaSqMeters,
                geoJsonCoordinates = formatBoundaryJson(expansion.boundaryPolygon),
                h3HexIndexes = expansion.confirmedCells.joinToString(","),
                defenseLevel = 100,
                capturedAt = expansion.confirmedAt,
                isAuthoritative = true
            )
            client.from("territories").upsert(territory)
            Log.i(tag, "Successfully uploaded territory to Supabase: ${expansion.territoryId}")
            true
        } catch (e: Exception) {
            Log.w(tag, "Supabase territory upload failed: ${e.message}")
            false
        }
    }

    suspend fun fetchAndSyncWorldTerritories(): List<TerritoryEntity> = withContext(Dispatchers.IO) {
        try {
            val remoteTerritories = client.from("territories")
                .select()
                .decodeList<SupabaseTerritory>()

            if (remoteTerritories.isNotEmpty()) {
                val entities = remoteTerritories.map { remote ->
                    TerritoryEntity(
                        id = remote.id,
                        ownerUserId = remote.ownerUserId,
                        ownerDisplayName = remote.ownerDisplayName,
                        faction = remote.faction,
                        geoJsonCoordinates = remote.geoJsonCoordinates,
                        areaSqMeters = remote.areaSqMeters,
                        h3HexIndexes = remote.h3HexIndexes,
                        defenseLevel = remote.defenseLevel,
                        capturedAt = remote.capturedAt,
                        isAuthoritative = remote.isAuthoritative,
                        isSynced = true
                    )
                }
                territoryDao.insertTerritories(entities)
                Log.i(tag, "Successfully synced ${entities.size} territories from Supabase into Room database.")
                return@withContext entities
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not fetch world territories from Supabase: ${e.message}")
        }
        emptyList()
    }

    suspend fun fetchLeaderboardProfiles(): List<SupabaseProfile> = withContext(Dispatchers.IO) {
        try {
            val profiles = client.from("profiles")
                .select()
                .decodeList<SupabaseProfile>()
            Log.i(tag, "Fetched ${profiles.size} profiles from Supabase for live leaderboard")
            profiles
        } catch (e: Exception) {
            Log.w(tag, "Could not fetch profiles from Supabase: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchUserRuns(userId: String): List<SupabaseRun> = withContext(Dispatchers.IO) {
        try {
            val runs = client.from("runs")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseRun>()
            Log.i(tag, "Fetched ${runs.size} runs from Supabase for user $userId")
            runs
        } catch (e: Exception) {
            Log.w(tag, "Could not fetch user runs from Supabase: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAllRuns(): List<SupabaseRun> = withContext(Dispatchers.IO) {
        try {
            val runs = client.from("runs")
                .select()
                .decodeList<SupabaseRun>()
            runs
        } catch (e: Exception) {
            Log.w(tag, "Could not fetch all runs from Supabase: ${e.message}")
            emptyList()
        }
    }

    private fun formatBoundaryJson(polygon: List<com.example.domain.model.LatLng>): String {
        return polygon.joinToString(prefix = "[", postfix = "]") {
            "{\"latitude\":${it.latitude},\"longitude\":${it.longitude}}"
        }
    }
}
