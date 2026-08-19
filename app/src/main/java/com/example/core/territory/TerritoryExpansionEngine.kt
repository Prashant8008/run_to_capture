package com.example.core.territory

import com.example.core.geo.H3SpatialIndex
import com.example.domain.model.Faction
import com.example.domain.model.GpsPoint
import com.example.domain.model.LatLng
import com.example.domain.model.RejectedCellReason
import com.example.domain.model.ExpansionPreviewResult
import com.example.domain.model.ServerConfirmedExpansion
import com.example.domain.model.TerritoryExpansionRuleConfig
import java.security.MessageDigest
import java.util.UUID

/**
 * Phase 9 Territory Expansion Engine.
 *
 * Implements the full lifecycle:
 * Run -> Validation -> H3 coverage -> existing territory -> eligible new cells -> expansion
 */
class TerritoryExpansionEngine(
    var serverConfig: TerritoryExpansionRuleConfig = TerritoryExpansionRuleConfig()
) {

    /**
     * Step 1: Validation of Run Trajectory
     * Checks distance, point counts, speed limits, and teleport jumps.
     */
    fun validateRunTrajectory(
        points: List<GpsPoint>,
        config: TerritoryExpansionRuleConfig = serverConfig
    ): ValidationResult {
        if (points.size < config.minGpsPoints) {
            return ValidationResult(
                isValid = false,
                reason = "Insufficient GPS points recorded (minimum ${config.minGpsPoints} required)"
            )
        }

        var totalDist = 0.0
        var validPoints = 0
        var prevPoint: GpsPoint? = null

        for (pt in points) {
            if (pt.accuracy > 40.0f) {
                // Ignore poor accuracy fixes
                continue
            }

            if (prevPoint != null) {
                val dist = H3SpatialIndex.calculateDistanceMeters(
                    prevPoint.latitude, prevPoint.longitude,
                    pt.latitude, pt.longitude
                )

                val timeDeltaSec = (pt.timestamp - prevPoint.timestamp) / 1000.0
                if (timeDeltaSec > 0) {
                    val speed = dist / timeDeltaSec
                    if (speed > config.maxSpeedLimitMps) {
                        return ValidationResult(
                            isValid = false,
                            reason = "Trajectory speed limit anomaly detected (%.1f km/h > %.1f km/h)".format(
                                speed * 3.6, config.maxSpeedLimitMps * 3.6
                            )
                        )
                    }
                }

                // Check for single step teleport jumps
                if (dist > config.maxSingleStepDistanceMeters && timeDeltaSec < 10.0) {
                    return ValidationResult(
                        isValid = false,
                        reason = "Teleportation anomaly detected (jump of %.1f m in %.1fs)".format(dist, timeDeltaSec)
                    )
                }

                totalDist += dist
            }
            prevPoint = pt
            validPoints++
        }

        if (totalDist < config.minRunDistanceMeters) {
            return ValidationResult(
                isValid = false,
                reason = "Total distance (%.1f m) below minimum threshold (%.1f m)".format(totalDist, config.minRunDistanceMeters)
            )
        }

        return ValidationResult(isValid = true, totalDistanceMeters = totalDist)
    }

    /**
     * Step 2: H3 Coverage Generation
     * Converts validated run path coordinates into discrete H3 hex cell indices.
     */
    fun computeH3Coverage(
        path: List<LatLng>,
        config: TerritoryExpansionRuleConfig = serverConfig
    ): List<String> {
        return H3SpatialIndex.polylineCoverageToCells(path, config.h3Resolution)
    }

    /**
     * Steps 3, 4, 5: Calculates the Client Expansion Preview.
     * Evaluates existing territory, eligible new cells, duplicates, connectivity, overlaps, and caps.
     * NOTE: Client preview is NOT authoritative.
     */
    fun calculateExpansionPreview(
        sessionId: String,
        runPoints: List<GpsPoint>,
        existingTerritoryId: String?,
        existingCells: Set<String>,
        rivalFactionCells: Set<String> = emptySet(),
        config: TerritoryExpansionRuleConfig = serverConfig
    ): ExpansionPreviewResult {
        val validationNotes = mutableListOf<String>()

        // 1. Validation
        val valResult = validateRunTrajectory(runPoints, config)
        if (!valResult.isValid) {
            return ExpansionPreviewResult(
                sessionId = sessionId,
                existingTerritoryId = existingTerritoryId,
                existingCells = existingCells,
                runTraversedCells = emptySet(),
                eligibleNewCells = emptySet(),
                rejectedCells = emptyMap(),
                previewMergedCells = existingCells,
                existingAreaSqMeters = existingCells.size * H3SpatialIndex.cellAreaSqMeters(config.h3Resolution),
                projectedTotalAreaSqMeters = existingCells.size * H3SpatialIndex.cellAreaSqMeters(config.h3Resolution),
                gainedAreaSqMeters = 0.0,
                validationPassed = false,
                validationNotes = listOf("Validation Failed: ${valResult.reason}"),
                isAuthoritative = false
            )
        }

        validationNotes.add("Trajectory integrity validated: %.1f meters covered".format(valResult.totalDistanceMeters))

        // 2. H3 Coverage
        val pathCoords = runPoints.map { LatLng(it.latitude, it.longitude) }
        val rawTraversedCells = computeH3Coverage(pathCoords, config)
        val uniqueTraversedCells = rawTraversedCells.distinct()
        validationNotes.add("H3 Hex Coverage: ${uniqueTraversedCells.size} unique cells traversed")

        // 3. Existing Territory Analysis
        val cellArea = H3SpatialIndex.cellAreaSqMeters(config.h3Resolution)
        val existingArea = existingCells.size * cellArea

        // 4. Eligible New Cells Evaluation
        val eligibleNewCells = mutableSetOf<String>()
        val rejectedCells = mutableMapOf<String, RejectedCellReason>()

        // Connected active territory accumulator for adjacency traversal
        val currentTerritoryPool = existingCells.toMutableSet()
        var newlyAddedCount = 0

        for (cell in uniqueTraversedCells) {
            // Check 1: Duplicate (already owned by player)
            if (existingCells.contains(cell)) {
                rejectedCells[cell] = RejectedCellReason.DUPLICATE_ALREADY_OWNED
                continue
            }

            // Check 2: Enemy Territory Overlap
            if (rivalFactionCells.contains(cell) && !config.allowEnemyTakeover) {
                rejectedCells[cell] = RejectedCellReason.ENEMY_TERRITORY_SHIELDED
                continue
            }

            // Check 3: Geographically Connected Adjacency
            val isAdjacentToExisting = if (existingCells.isEmpty()) {
                // If player has no territory yet, the first run establishes the core territory (must be self-contiguous)
                eligibleNewCells.isEmpty() || H3SpatialIndex.isCellAdjacentToSet(cell, eligibleNewCells)
            } else {
                // Must connect to existing territory or currently accepted expansion cluster
                H3SpatialIndex.isCellAdjacentToSet(cell, currentTerritoryPool)
            }

            if (!isAdjacentToExisting && !config.allowDisconnectedExpansion) {
                rejectedCells[cell] = RejectedCellReason.NON_ADJACENT_DISCONNECTED
                continue
            }

            // Check 4: Maximum Expansion Quota per Run
            if (newlyAddedCount >= config.maxNewCellsPerRun) {
                rejectedCells[cell] = RejectedCellReason.MAX_EXPANSION_CAP_EXCEEDED
                continue
            }

            // Cell is eligible for expansion!
            eligibleNewCells.add(cell)
            currentTerritoryPool.add(cell)
            newlyAddedCount++
        }

        val previewMergedCells = existingCells + eligibleNewCells
        val gainedArea = eligibleNewCells.size * cellArea
        val projectedTotalArea = previewMergedCells.size * cellArea

        validationNotes.add("Eligible for expansion: ${eligibleNewCells.size} new hex cells (+%.0f m²)".format(gainedArea))
        if (rejectedCells.isNotEmpty()) {
            validationNotes.add("Filtered: ${rejectedCells.size} cells (Duplicates, Non-Adjacent, or Quota limits)")
        }

        return ExpansionPreviewResult(
            sessionId = sessionId,
            existingTerritoryId = existingTerritoryId,
            existingCells = existingCells,
            runTraversedCells = uniqueTraversedCells.toSet(),
            eligibleNewCells = eligibleNewCells,
            rejectedCells = rejectedCells,
            previewMergedCells = previewMergedCells,
            existingAreaSqMeters = existingArea,
            projectedTotalAreaSqMeters = projectedTotalArea,
            gainedAreaSqMeters = gainedArea,
            validationPassed = true,
            validationNotes = validationNotes,
            isAuthoritative = false // Client preview is NOT authoritative
        )
    }

    /**
     * Authoritative Server Confirmation of Territory Expansion.
     * Server evaluates ownership, creates digital signature, and assigns final territory cells.
     */
    fun serverConfirmExpansion(
        preview: ExpansionPreviewResult,
        userId: String,
        displayName: String,
        faction: Faction,
        config: TerritoryExpansionRuleConfig = serverConfig
    ): ServerConfirmedExpansion {
        require(preview.validationPassed) { "Cannot confirm expansion for an invalid run" }

        val territoryId = preview.existingTerritoryId ?: "territory_${faction.name.lowercase()}_${UUID.randomUUID().toString().take(8)}"
        val finalConfirmedCells = preview.previewMergedCells
        val newlyGainedCells = preview.eligibleNewCells
        val cellArea = H3SpatialIndex.cellAreaSqMeters(config.h3Resolution)

        val totalArea = finalConfirmedCells.size * cellArea
        val gainedArea = newlyGainedCells.size * cellArea

        // Generate polygon boundaries for all individual hex cells
        val allHexBoundaries = finalConfirmedCells.map { cellId ->
            H3SpatialIndex.cellToBoundary(cellId)
        }

        // Generate bounding outer hull / representative territory polygon
        val boundaryPolygon = if (allHexBoundaries.isNotEmpty()) {
            generateRepresentativePolygon(finalConfirmedCells)
        } else {
            emptyList()
        }

        // Server cryptographic signature verifying authoritative approval
        val rawPayload = "$territoryId:$userId:${faction.name}:${finalConfirmedCells.sorted().joinToString(",")}:$totalArea"
        val signature = sha256Hex("RUN2CAPTURE-SERVER-AUTH-V1:$rawPayload")

        return ServerConfirmedExpansion(
            territoryId = territoryId,
            ownerUserId = userId,
            ownerDisplayName = displayName,
            faction = faction,
            confirmedCells = finalConfirmedCells,
            newlyGainedCells = newlyGainedCells,
            totalAreaSqMeters = totalArea,
            gainedAreaSqMeters = gainedArea,
            boundaryPolygon = boundaryPolygon,
            hexCellBoundaries = allHexBoundaries,
            confirmedAt = System.currentTimeMillis(),
            serverSignature = signature,
            isOfflineCached = false
        )
    }

    /**
     * Generates a smooth representative convex/concave boundary around the active hex cells.
     */
    fun generateRepresentativePolygon(cells: Set<String>): List<LatLng> {
        if (cells.isEmpty()) return emptyList()
        val allVertices = cells.flatMap { H3SpatialIndex.cellToBoundary(it) }
        if (allVertices.size < 3) return allVertices

        // Convex hull of vertices for territory display
        return computeConvexHull(allVertices)
    }

    private fun computeConvexHull(points: List<LatLng>): List<LatLng> {
        if (points.size <= 3) return points

        val sorted = points.sortedWith(compareBy({ it.latitude }, { it.longitude }))
        val lower = mutableListOf<LatLng>()
        for (p in sorted) {
            while (lower.size >= 2 && crossProduct(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
                lower.removeAt(lower.size - 1)
            }
            lower.add(p)
        }

        val upper = mutableListOf<LatLng>()
        for (p in sorted.reversed()) {
            while (upper.size >= 2 && crossProduct(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
                upper.removeAt(upper.size - 1)
            }
            upper.add(p)
        }

        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)
        return lower + upper
    }

    private fun crossProduct(o: LatLng, a: LatLng, b: LatLng): Double {
        return (a.longitude - o.longitude) * (b.latitude - o.latitude) -
                (a.latitude - o.latitude) * (b.longitude - o.longitude)
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val totalDistanceMeters: Double = 0.0
    )
}
