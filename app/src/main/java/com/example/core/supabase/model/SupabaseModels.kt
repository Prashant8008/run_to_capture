package com.example.core.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseProfile(
    val id: String,
    val email: String? = null,
    @SerialName("display_name")
    val displayName: String = "OPERATIVE",
    val faction: String = "CIPHER",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("territory_color")
    val territoryColor: String = "#00F0FF",
    @SerialName("total_area_sq_meters")
    val totalAreaSqMeters: Double = 0.0,
    @SerialName("total_distance_meters")
    val totalDistanceMeters: Double = 0.0,
    @SerialName("territories_count")
    val territoriesCount: Int = 0,
    val xp: Long = 0,
    val level: Int = 1,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class SupabaseRun(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_display_name")
    val userDisplayName: String = "OPERATIVE",
    val faction: String = "CIPHER",
    @SerialName("distance_meters")
    val distanceMeters: Double = 0.0,
    @SerialName("duration_seconds")
    val durationSeconds: Long = 0,
    @SerialName("avg_pace_seconds_per_km")
    val avgPaceSecondsPerKm: Long = 0,
    @SerialName("calories_burned")
    val caloriesBurned: Int = 0,
    @SerialName("is_closed_loop")
    val isClosedLoop: Boolean = false,
    @SerialName("enclosed_area_sq_meters")
    val enclosedAreaSqMeters: Double = 0.0,
    @SerialName("route_geojson")
    val routeGeoJson: String = "[]",
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class SupabaseTerritory(
    val id: String,
    @SerialName("owner_user_id")
    val ownerUserId: String,
    @SerialName("owner_display_name")
    val ownerDisplayName: String = "OPERATIVE",
    val faction: String = "CIPHER",
    @SerialName("area_sq_meters")
    val areaSqMeters: Double = 0.0,
    @SerialName("geojson_coordinates")
    val geoJsonCoordinates: String = "[]",
    @SerialName("h3_hex_indexes")
    val h3HexIndexes: String = "",
    @SerialName("defense_level")
    val defenseLevel: Int = 100,
    @SerialName("captured_at")
    val capturedAt: Long = System.currentTimeMillis(),
    @SerialName("is_authoritative")
    val isAuthoritative: Boolean = true
)
