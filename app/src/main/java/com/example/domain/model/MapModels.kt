package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    fun toFormattedString(): String = "%.5f, %.5f".format(latitude, longitude)
}

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val heading: Float get() = bearingDegrees
    val toLatLng: LatLng get() = LatLng(latitude, longitude)
}

enum class GpsSignalStatus(val label: String, val isGood: Boolean) {
    SEARCHING("SEARCHING...", false),
    POOR("WEAK GPS", false),
    GOOD("GPS LOCKED", true),
    DISABLED("GPS OFF", false)
}

@JsonClass(generateAdapter = true)
data class DevTerritory(
    val id: String,
    val name: String,
    val factionId: String,
    val colorHex: String,
    val coordinates: List<LatLng>,
    val areaSqMeters: Double,
    val defenseLevel: Int = 100
)

