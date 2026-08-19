package com.example.feature.map.bridge

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.domain.model.DevTerritory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.ref.WeakReference

/**
 * Clean bridge between Android Kotlin and Leaflet JavaScript inside WebView.
 * Strictly adheres to separation of concerns:
 * - Kotlin handles business logic, GPS updates, and UI state.
 * - Leaflet handles map rendering, gestures, and visual polygon/marker rendering.
 */
interface LeafletMapController {
    fun setCenter(lat: Double, lng: Double, animated: Boolean = true)
    fun setZoom(zoom: Int, animated: Boolean = true)
    fun setUserLocation(lat: Double, lng: Double, accuracy: Float = 0f, heading: Float? = null, colorHex: String? = null)
    fun renderTerritories(territories: List<DevTerritory>)
    fun clearTerritories()
    fun updateRoute(points: List<com.example.domain.model.LatLng>, colorHex: String? = null)
    fun clearRoute()
    fun renderNewRunCells(hexPolygons: List<List<com.example.domain.model.LatLng>>, colorHex: String? = null)
    fun renderExpansionPreview(previewPolygons: List<List<com.example.domain.model.LatLng>>, colorHex: String? = null)
    fun renderConfirmedTerritory(territory: DevTerritory, colorHex: String? = null)
    fun clearExpansionLayers()
    fun fitBoundsToPoints(points: List<com.example.domain.model.LatLng>)
    fun zoomIn()
    fun zoomOut()
    fun panBy(dx: Int, dy: Int)
    fun setTileUrl(url: String, subdomains: String = "abcd", maxZoom: Int = 19, attribution: String = "")
}

interface MapEventListener {
    fun onMapReady()
    fun onTerritoryClicked(territoryId: String)
    fun onMapMoved(lat: Double, lng: Double)
    fun onMapZoomChanged(zoom: Int)
    fun onTileLoading()
    fun onTileLoaded()
    fun onTileError(errorMessage: String?)
}

class LeafletBridge(
    webView: WebView,
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
) : LeafletMapController {

    private val webViewRef = WeakReference(webView)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var eventListener: MapEventListener? = null

    fun setEventListener(listener: MapEventListener?) {
        this.eventListener = listener
    }

    private fun evaluateJavascript(script: String) {
        mainHandler.post {
            webViewRef.get()?.evaluateJavascript(script, null)
        }
    }

    override fun setCenter(lat: Double, lng: Double, animated: Boolean) {
        evaluateJavascript("window.setCenter($lat, $lng, $animated);")
    }

    override fun setZoom(zoom: Int, animated: Boolean) {
        evaluateJavascript("window.setZoom($zoom, $animated);")
    }

    override fun setUserLocation(
        lat: Double,
        lng: Double,
        accuracy: Float,
        heading: Float?,
        colorHex: String?
    ) {
        val headingVal = heading?.toString() ?: "null"
        val colorVal = if (colorHex != null) "'$colorHex'" else "null"
        evaluateJavascript("window.setUserLocation($lat, $lng, $accuracy, $headingVal, $colorVal);")
    }

    override fun renderTerritories(territories: List<DevTerritory>) {
        val rawJson: String = try {
            val type = Types.newParameterizedType(List::class.java, DevTerritory::class.java)
            val adapter = moshi.adapter<List<DevTerritory>>(type)
            adapter.toJson(territories)
        } catch (e: Throwable) {
            Log.e("LeafletBridge", "Moshi adapter fallback for territories: ${e.message}")
            val sb = StringBuilder("[")
            territories.forEachIndexed { i, t ->
                if (i > 0) sb.append(",")
                sb.append("{")
                sb.append("\"id\":\"${t.id}\",")
                sb.append("\"name\":\"${t.name.replace("\"", "\\\"")}\",")
                sb.append("\"factionId\":\"${t.factionId}\",")
                sb.append("\"colorHex\":\"${t.colorHex}\",")
                sb.append("\"areaSqMeters\":${t.areaSqMeters},")
                sb.append("\"defenseLevel\":${t.defenseLevel},")
                sb.append("\"coordinates\":[")
                t.coordinates.forEachIndexed { ci, c ->
                    if (ci > 0) sb.append(",")
                    sb.append("{\"latitude\":${c.latitude},\"longitude\":${c.longitude}}")
                }
                sb.append("]")
                sb.append("}")
            }
            sb.append("]")
            sb.toString()
        }

        // Pass raw JSON safely into window.renderTerritories
        val escaped = rawJson.replace("\\", "\\\\").replace("'", "\\'")
        evaluateJavascript("window.renderTerritories('$escaped');")
    }

    override fun clearTerritories() {
        evaluateJavascript("window.clearTerritories();")
    }

    override fun updateRoute(points: List<com.example.domain.model.LatLng>, colorHex: String?) {
        val colorParam = if (colorHex != null) "'$colorHex'" else "'#CCFF00'"
        val sb = StringBuilder("[")
        points.forEachIndexed { i, p ->
            if (i > 0) sb.append(",")
            sb.append("{\"lat\":${p.latitude},\"lng\":${p.longitude}}")
        }
        sb.append("]")
        evaluateJavascript("window.updateRoute('${sb.toString()}', $colorParam);")
    }

    override fun clearRoute() {
        evaluateJavascript("window.clearRoute();")
    }

    override fun renderNewRunCells(hexPolygons: List<List<com.example.domain.model.LatLng>>, colorHex: String?) {
        val colorParam = if (colorHex != null) "'$colorHex'" else "'#00F0FF'"
        val sb = StringBuilder("[")
        hexPolygons.forEachIndexed { i, poly ->
            if (i > 0) sb.append(",")
            sb.append("[")
            poly.forEachIndexed { j, pt ->
                if (j > 0) sb.append(",")
                sb.append("{\"lat\":${pt.latitude},\"lng\":${pt.longitude}}")
            }
            sb.append("]")
        }
        sb.append("]")
        evaluateJavascript("window.renderNewRunCells('${sb.toString()}', $colorParam);")
    }

    override fun renderExpansionPreview(previewPolygons: List<List<com.example.domain.model.LatLng>>, colorHex: String?) {
        val colorParam = if (colorHex != null) "'$colorHex'" else "'#CCFF00'"
        val sb = StringBuilder("[")
        previewPolygons.forEachIndexed { i, poly ->
            if (i > 0) sb.append(",")
            sb.append("[")
            poly.forEachIndexed { j, pt ->
                if (j > 0) sb.append(",")
                sb.append("{\"lat\":${pt.latitude},\"lng\":${pt.longitude}}")
            }
            sb.append("]")
        }
        sb.append("]")
        evaluateJavascript("window.renderExpansionPreview('${sb.toString()}', $colorParam);")
    }

    override fun renderConfirmedTerritory(territory: DevTerritory, colorHex: String?) {
        val color = colorHex ?: territory.colorHex
        val sb = StringBuilder("{")
        sb.append("\"id\":\"${territory.id}\",")
        sb.append("\"name\":\"${territory.name}\",")
        sb.append("\"colorHex\":\"$color\",")
        sb.append("\"areaSqMeters\":${territory.areaSqMeters},")
        sb.append("\"coordinates\":[")
        territory.coordinates.forEachIndexed { i, c ->
            if (i > 0) sb.append(",")
            sb.append("{\"lat\":${c.latitude},\"lng\":${c.longitude}}")
        }
        sb.append("]")
        sb.append("}")
        evaluateJavascript("window.renderConfirmedTerritory('${sb.toString()}', '$color');")
    }

    override fun clearExpansionLayers() {
        evaluateJavascript("window.clearExpansionLayers();")
    }

    override fun fitBoundsToPoints(points: List<com.example.domain.model.LatLng>) {
        if (points.isEmpty()) return
        val sb = StringBuilder("[")
        points.forEachIndexed { i, p ->
            if (i > 0) sb.append(",")
            sb.append("{\"lat\":${p.latitude},\"lng\":${p.longitude}}")
        }
        sb.append("]")
        evaluateJavascript("window.fitBoundsToPoints('${sb.toString()}');")
    }

    override fun zoomIn() {
        evaluateJavascript("window.zoomIn();")
    }

    override fun zoomOut() {
        evaluateJavascript("window.zoomOut();")
    }

    override fun panBy(dx: Int, dy: Int) {
        evaluateJavascript("window.panBy($dx, $dy);")
    }

    override fun setTileUrl(url: String, subdomains: String, maxZoom: Int, attribution: String) {
        val escapedUrl = url.replace("'", "\\'")
        val escapedAttr = attribution.replace("'", "\\'")
        evaluateJavascript("window.setTileLayer('$escapedUrl', '$subdomains', $maxZoom, '$escapedAttr');")
    }

    /**
     * Exposed JavaScript interface registered on WebView as 'AndroidMapBridge'.
     */
    inner class JavascriptInterfaceBridge {

        @JavascriptInterface
        fun mapReady() {
            mainHandler.post {
                eventListener?.onMapReady()
            }
        }

        @JavascriptInterface
        fun territoryClicked(territoryId: String) {
            mainHandler.post {
                eventListener?.onTerritoryClicked(territoryId)
            }
        }

        @JavascriptInterface
        fun mapMoved(lat: Double, lng: Double) {
            mainHandler.post {
                eventListener?.onMapMoved(lat, lng)
            }
        }

        @JavascriptInterface
        fun mapZoomChanged(zoom: Int) {
            mainHandler.post {
                eventListener?.onMapZoomChanged(zoom)
            }
        }

        @JavascriptInterface
        fun tileLoading() {
            mainHandler.post {
                eventListener?.onTileLoading()
            }
        }

        @JavascriptInterface
        fun tileLoaded() {
            mainHandler.post {
                eventListener?.onTileLoaded()
            }
        }

        @JavascriptInterface
        fun tileError(errorMessage: String?) {
            mainHandler.post {
                eventListener?.onTileError(errorMessage)
            }
        }
    }
}
