package com.example.domain.model

data class PlayerProfile(
    val userId: String,
    val displayName: String,
    val faction: Faction,
    val totalCapturedAreaSqMeters: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val territoriesOwnedCount: Int = 0,
    val flagSvgPattern: String? = null
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Territory(
    val id: String,
    val ownerUserId: String,
    val ownerDisplayName: String,
    val faction: Faction,
    val coordinates: List<GeoPoint>,
    val areaSqMeters: Double,
    val capturedAt: Long,
    val defenseLevel: Int = 100
)

data class RunSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val coordinates: List<GeoPoint> = emptyList(),
    val territoriesCapturedCount: Int = 0,
    val isActive: Boolean = true
)

sealed interface HealthState {
    data object Idle : HealthState
    data object Loading : HealthState
    data class Success(val status: String, val timestamp: Long = System.currentTimeMillis()) : HealthState
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : HealthState
}
