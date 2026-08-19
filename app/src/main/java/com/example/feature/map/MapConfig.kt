package com.example.feature.map

/**
 * Available map tile styles for Run2Capture.
 * Street View is the primary default view at launch; users can switch to Satellite or Tactical Dark.
 */
enum class MapLayerType(
    val title: String,
    val description: String,
    val tileUrl: String,
    val subdomains: String,
    val maxZoom: Int,
    val attribution: String
) {
    STREET(
        title = "Street View",
        description = "Standard high-contrast street map",
        tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        subdomains = "abc",
        maxZoom = 19,
        attribution = "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors"
    ),
    SATELLITE(
        title = "Satellite",
        description = "High-resolution orbital satellite imagery",
        tileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        subdomains = "",
        maxZoom = 19,
        attribution = "Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
    ),
    TACTICAL_DARK(
        title = "Dark Tactical",
        description = "High-contrast tactical stealth grid",
        tileUrl = "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
        subdomains = "abcd",
        maxZoom = 19,
        attribution = "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors &copy; <a href=\"https://carto.com/attributions\">CARTO</a>"
    )
}

/**
 * Map configuration defaults and parameters.
 */
object MapConfig {
    // Default Street View tiles (OpenStreetMap)
    val DEFAULT_LAYER = MapLayerType.STREET

    const val DEFAULT_ZOOM = 16
    const val MIN_ZOOM = 3
    const val MAX_ZOOM = 19

    // Fallback default coordinates (San Francisco)
    const val DEFAULT_LAT = 37.7749
    const val DEFAULT_LNG = -122.4194
}

