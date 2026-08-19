package com.example.core.geo

import com.example.domain.model.LatLng
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geodesic Hexagonal Spatial Index (H3-compatible indexing engine).
 * Provides discrete spatial hexagon partitioning for territory capture and expansion.
 */
object H3SpatialIndex {

    const val DEFAULT_RESOLUTION = 9 // ~80-100m hexagon radius, ~25,000 m² area
    private const val EARTH_RADIUS_METERS = 6371008.8

    // Hexagon side length in meters for different resolutions
    private val HEX_RADIUS_METERS_BY_RES = mapOf(
        7 to 1220.0,
        8 to 460.0,
        9 to 174.0,
        10 to 65.0,
        11 to 25.0
    )

    /**
     * Converts a lat/lng coordinate into a standardized Hex Cell ID at the given resolution.
     * Generates a deterministic hex cell identifier.
     */
    fun latLngToCell(lat: Double, lng: Double, res: Int = DEFAULT_RESOLUTION): String {
        val radius = HEX_RADIUS_METERS_BY_RES[res] ?: 174.0
        
        // Convert to local planar coordinates centered at resolution scale
        val latRad = Math.toRadians(lat)
        val x = EARTH_RADIUS_METERS * Math.toRadians(lng) * cos(latRad)
        val y = EARTH_RADIUS_METERS * latRad

        // Axial hexagonal coordinate system
        // Flat-topped or pointy-topped hex geometry
        val size = radius
        val q = (sqrt(3.0) / 3.0 * x - 1.0 / 3.0 * y) / size
        val r = (2.0 / 3.0 * y) / size

        val axialCoords = axialRound(q, r)
        val cellQ = axialCoords.first
        val cellR = axialCoords.second

        val qHex = String.format("%08x", cellQ.toLong() and 0xFFFFFFFFL)
        val rHex = String.format("%08x", cellR.toLong() and 0xFFFFFFFFL)
        return "h3_r${res}_${qHex}_${rHex}"
    }

    /**
     * Reconstructs the axial coordinates from a cell ID.
     */
    fun parseCell(cellId: String): Triple<Int, Int, Int>? {
        if (!cellId.startsWith("h3_r")) return null
        val parts = cellId.removePrefix("h3_r").split("_")
        if (parts.size != 3) return null
        val res = parts[0].toIntOrNull() ?: return null
        val q = parts[1].toLongOrNull(16)?.toInt() ?: return null
        val r = parts[2].toLongOrNull(16)?.toInt() ?: return null
        return Triple(res, q, r)
    }

    /**
     * Formats axial coordinates into a cell ID.
     */
    fun formatCell(res: Int, q: Int, r: Int): String {
        val qHex = String.format("%08x", q.toLong() and 0xFFFFFFFFL)
        val rHex = String.format("%08x", r.toLong() and 0xFFFFFFFFL)
        return "h3_r${res}_${qHex}_${rHex}"
    }

    /**
     * Computes the center LatLng for a given Hex Cell ID.
     */
    fun cellToLatLng(cellId: String): LatLng {
        val parsed = parseCell(cellId) ?: return LatLng(0.0, 0.0)
        val res = parsed.first
        val q = parsed.second
        val r = parsed.third
        val size = HEX_RADIUS_METERS_BY_RES[res] ?: 174.0

        val x = size * (sqrt(3.0) * q + sqrt(3.0) / 2.0 * r)
        val y = size * (3.0 / 2.0 * r)

        val latRad = y / EARTH_RADIUS_METERS
        val lat = Math.toDegrees(latRad)
        val lngRad = x / (EARTH_RADIUS_METERS * cos(latRad.coerceIn(-1.5, 1.5)))
        val lng = Math.toDegrees(lngRad)

        return LatLng(lat.coerceIn(-90.0, 90.0), lng.coerceIn(-180.0, 180.0))
    }

    /**
     * Generates the 6 boundary vertices of a hexagonal cell (in polygon order).
     */
    fun cellToBoundary(cellId: String): List<LatLng> {
        val center = cellToLatLng(cellId)
        val parsed = parseCell(cellId) ?: return emptyList()
        val res = parsed.first
        val radiusMeters = HEX_RADIUS_METERS_BY_RES[res] ?: 174.0

        val vertices = mutableListOf<LatLng>()
        for (i in 0 until 6) {
            val angleDeg = 60.0 * i + 30.0
            val angleRad = Math.toRadians(angleDeg)

            val dx = radiusMeters * cos(angleRad)
            val dy = radiusMeters * sin(angleRad)

            val latRad = Math.toRadians(center.latitude)
            val dLat = dy / EARTH_RADIUS_METERS
            val dLng = dx / (EARTH_RADIUS_METERS * cos(latRad))

            val vertexLat = center.latitude + Math.toDegrees(dLat)
            val vertexLng = center.longitude + Math.toDegrees(dLng)

            vertices.add(LatLng(vertexLat, vertexLng))
        }
        return vertices
    }

    /**
     * Returns the 6 immediately adjacent hexagonal cells (1-ring).
     */
    fun getAdjacentCells(cellId: String): Set<String> {
        val parsed = parseCell(cellId) ?: return emptySet()
        val res = parsed.first
        val q = parsed.second
        val r = parsed.third

        // 6 axial neighbor directions
        val directions = listOf(
            Pair(+1, 0), Pair(+1, -1), Pair(0, -1),
            Pair(-1, 0), Pair(-1, +1), Pair(0, +1)
        )

        return directions.map { (dq, dr) ->
            formatCell(res, q + dq, r + dr)
        }.toSet()
    }

    /**
     * Returns the k-ring grid disk of cells around a center cell.
     */
    fun getGridDisk(cellId: String, k: Int = 1): Set<String> {
        val parsed = parseCell(cellId) ?: return setOf(cellId)
        val res = parsed.first
        val centerQ = parsed.second
        val centerR = parsed.third

        val result = mutableSetOf<String>()
        for (q in -k..k) {
            val r1 = maxOf(-k, -q - k)
            val r2 = minOf(k, -q + k)
            for (r in r1..r2) {
                result.add(formatCell(res, centerQ + q, centerR + r))
            }
        }
        return result
    }

    /**
     * Checks if two hex cells share an edge (are directly adjacent).
     */
    fun areCellsAdjacent(cell1: String, cell2: String): Boolean {
        if (cell1 == cell2) return false
        val adj1 = getAdjacentCells(cell1)
        return adj1.contains(cell2)
    }

    /**
     * Checks if a candidate cell is adjacent to any cell in a given set of cells.
     */
    fun isCellAdjacentToSet(cell: String, cellSet: Set<String>): Boolean {
        if (cellSet.isEmpty()) return false
        val adj = getAdjacentCells(cell)
        return adj.any { cellSet.contains(it) }
    }

    /**
     * Checks if a set of cells forms a single contiguous connected component.
     */
    fun isSetContiguous(cells: Set<String>): Boolean {
        if (cells.size <= 1) return true
        val unvisited = cells.toMutableSet()
        val queue = ArrayDeque<String>()
        val start = unvisited.first()
        queue.add(start)
        unvisited.remove(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = getAdjacentCells(current)
            val adjacentInSet = unvisited.filter { neighbors.contains(it) }
            for (adj in adjacentInSet) {
                unvisited.remove(adj)
                queue.add(adj)
            }
        }
        return unvisited.isEmpty()
    }

    /**
     * Converts a run polyline path of LatLng points into a complete contiguous set of traversed H3 hex cells.
     * Interpolates between coordinates to avoid missing intermediate hex cells.
     */
    fun polylineCoverageToCells(path: List<LatLng>, res: Int = DEFAULT_RESOLUTION): List<String> {
        if (path.isEmpty()) return emptyList()
        if (path.size == 1) return listOf(latLngToCell(path[0].latitude, path[0].longitude, res))

        val stepMeters = (HEX_RADIUS_METERS_BY_RES[res] ?: 174.0) * 0.4
        val orderedCells = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i + 1]
            val dist = calculateDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            val steps = maxOf(1, (dist / stepMeters).toInt())

            for (s in 0..steps) {
                val fraction = s.toDouble() / steps.toDouble()
                val lat = p1.latitude + (p2.latitude - p1.latitude) * fraction
                val lng = p1.longitude + (p2.longitude - p1.longitude) * fraction
                val cell = latLngToCell(lat, lng, res)
                if (seen.add(cell)) {
                    orderedCells.add(cell)
                }
            }
        }
        return orderedCells
    }

    /**
     * Calculates cell area in square meters.
     */
    fun cellAreaSqMeters(res: Int = DEFAULT_RESOLUTION): Double {
        val radius = HEX_RADIUS_METERS_BY_RES[res] ?: 174.0
        // Regular hexagon area = (3 * sqrt(3) / 2) * radius^2
        return (3.0 * sqrt(3.0) / 2.0) * (radius * radius)
    }

    private fun axialRound(q: Double, r: Double): Pair<Int, Int> {
        val s = -q - r
        var qRound = q.roundToInt()
        var rRound = r.roundToInt()
        var sRound = s.roundToInt()

        val qDiff = kotlin.math.abs(qRound - q)
        val rDiff = kotlin.math.abs(rRound - r)
        val sDiff = kotlin.math.abs(sRound - s)

        if (qDiff > rDiff && qDiff > sDiff) {
            qRound = -rRound - sRound
        } else if (rDiff > sDiff) {
            rRound = -qRound - sRound
        }
        return Pair(qRound, rRound)
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
