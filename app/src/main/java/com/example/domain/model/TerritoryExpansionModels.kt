package com.example.domain.model

import com.squareup.moshi.JsonClass

/**
 * Server-side configurable rules for territory expansion.
 */
@JsonClass(generateAdapter = true)
data class TerritoryExpansionRuleConfig(
    val allowDisconnectedExpansion: Boolean = false, // Prefer geographically connected expansion
    val requireExistingTerritoryConnection: Boolean = true, // Must connect to existing territory if player already owns territory
    val maxNewCellsPerRun: Int = 15, // Maximum expansion quota ceiling per run
    val minRunDistanceMeters: Double = 50.0,
    val minGpsPoints: Int = 2,
    val maxSpeedLimitMps: Double = 7.78, // ~28 km/h (Strict human running limit, rejects vehicles/cycling)
    val maxSingleStepDistanceMeters: Double = 250.0, // Anti-teleport threshold
    val h3Resolution: Int = 9,
    val allowEnemyTakeover: Boolean = false,
    val defenseMultiplier: Double = 1.0,
    val configVersion: String = "1.0.0-server"
) {
    val maxSpeedLimitKmh: Double get() = maxSpeedLimitMps * 3.6
}

enum class RejectedCellReason(val label: String, val description: String) {
    DUPLICATE_ALREADY_OWNED("ALREADY OWNED", "Hex cell is already part of your syndicate territory"),
    NON_ADJACENT_DISCONNECTED("NON-ADJACENT", "Cell is disconnected from your existing territory boundaries"),
    MAX_EXPANSION_CAP_EXCEEDED("QUOTA LIMIT", "Exceeds the maximum expansion quota allowed for a single run"),
    TELEPORT_INVALID_GPS("INTEGRITY FAILED", "Cell traversed via invalid GPS anomaly or teleportation"),
    ENEMY_TERRITORY_SHIELDED("ENEMY SHIELDED", "Cell is defended by a rival syndicate and cannot be expanded into"),
    SPEED_LIMIT_EXCEEDED("SPEED ANOMALY", "Traversed at unrealistic velocity exceeding human running limits")
}

@JsonClass(generateAdapter = true)
data class CellEvaluation(
    val cellId: String,
    val isEligible: Boolean,
    val rejectionReason: RejectedCellReason? = null,
    val boundary: List<LatLng>,
    val center: LatLng,
    val areaSqMeters: Double
)

/**
 * Client preview of the territory expansion.
 * IMPORTANT: Client preview is NOT authoritative. Server decides final ownership.
 */
@JsonClass(generateAdapter = true)
data class ExpansionPreviewResult(
    val sessionId: String,
    val existingTerritoryId: String?,
    val existingCells: Set<String>,
    val runTraversedCells: Set<String>,
    val eligibleNewCells: Set<String>,
    val rejectedCells: Map<String, RejectedCellReason>,
    val previewMergedCells: Set<String>,
    val existingAreaSqMeters: Double,
    val projectedTotalAreaSqMeters: Double,
    val gainedAreaSqMeters: Double,
    val validationPassed: Boolean,
    val validationNotes: List<String>,
    val isAuthoritative: Boolean = false
) {
    val eligibleCount: Int get() = eligibleNewCells.size
    val totalCount: Int get() = previewMergedCells.size
    val formattedGainedArea: String get() = "%.0f m²".format(gainedAreaSqMeters)
    val formattedTotalArea: String get() = "%.0f m²".format(projectedTotalAreaSqMeters)
    val formattedExistingArea: String get() = "%.0f m²".format(existingAreaSqMeters)
}

/**
 * Authoritative territory expansion confirmation returned from server.
 */
@JsonClass(generateAdapter = true)
data class ServerConfirmedExpansion(
    val territoryId: String,
    val ownerUserId: String,
    val ownerDisplayName: String,
    val faction: Faction,
    val confirmedCells: Set<String>,
    val newlyGainedCells: Set<String>,
    val totalAreaSqMeters: Double,
    val gainedAreaSqMeters: Double,
    val boundaryPolygon: List<LatLng>,
    val hexCellBoundaries: List<List<LatLng>>,
    val confirmedAt: Long = System.currentTimeMillis(),
    val serverSignature: String,
    val isAuthoritative: Boolean = true,
    val isOfflineCached: Boolean = false
)
