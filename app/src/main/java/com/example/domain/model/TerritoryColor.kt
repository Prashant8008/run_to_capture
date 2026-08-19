package com.example.domain.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Standard territory colors available for sector capture.
 */
enum class StandardTerritoryColor(
    val id: String,
    val displayName: String,
    val hex: String,
    val color: Color
) {
    BLUE("blue", "Sector Blue", "#007AFF", Color(0xFF007AFF)),
    PURPLE("purple", "Cipher Purple", "#9D00FF", Color(0xFF9D00FF)),
    RED("red", "Apex Red", "#FF3B30", Color(0xFFFF3B30)),
    ORANGE("orange", "Solaris Orange", "#FF9500", Color(0xFFFF9500)),
    CYAN("cyan", "Neon Cyan", "#00F0FF", Color(0xFF00F0FF)),
    PINK("pink", "Pulse Pink", "#FF2D55", Color(0xFFFF2D55)),
    GOLD("gold", "Dominance Gold", "#FFD700", Color(0xFFFFD700)),
    GREEN("green", "Matrix Green", "#00E676", Color(0xFF00E676)),
    INDIGO("indigo", "Void Indigo", "#4B0082", Color(0xFF4B0082));

    companion object {
        fun fromId(id: String): StandardTerritoryColor? {
            return entries.find { it.id.equals(id.trim(), ignoreCase = true) }
        }

        fun getHexForColor(nameOrHex: String): String {
            val standard = fromId(nameOrHex)
            if (standard != null) return standard.hex
            return if (nameOrHex.startsWith("#")) nameOrHex.uppercase(Locale.ROOT) else "#${nameOrHex.uppercase(Locale.ROOT)}"
        }

        fun parseColor(nameOrHex: String): Color {
            val standard = fromId(nameOrHex)
            if (standard != null) return standard.color
            return try {
                val clean = nameOrHex.trim().removePrefix("#")
                val fullHex = if (clean.length == 6) "FF$clean" else clean
                Color(fullHex.toLong(16))
            } catch (e: Exception) {
                Color(0xFF00F0FF) // fallback cyan
            }
        }
    }
}

/**
 * Result of map contrast & visibility validation.
 */
data class MapContrastResult(
    val isValid: Boolean,
    val brightness: Float,
    val contrastDistance: Float,
    val message: String
)

/**
 * Validates visibility of territory colors against the dark tactical map background (#0E121A).
 */
object MapContrastValidator {
    private const val MAP_R = 14
    private const val MAP_G = 18
    private const val MAP_B = 26
    private const val MIN_BRIGHTNESS = 0.22f
    private const val MIN_CONTRAST_DIST = 45f

    fun validate(hexColor: String): MapContrastResult {
        val clean = hexColor.trim().removePrefix("#")
        if (clean.length != 6 && clean.length != 3) {
            return MapContrastResult(
                isValid = false,
                brightness = 0f,
                contrastDistance = 0f,
                message = "Invalid hex format. Use #RRGGBB"
            )
        }

        val fullHex = if (clean.length == 3) {
            clean.map { "$it$it" }.joinToString("")
        } else {
            clean
        }

        return try {
            val r = fullHex.substring(0, 2).toInt(16)
            val g = fullHex.substring(2, 4).toInt(16)
            val b = fullHex.substring(4, 6).toInt(16)

            // Perceived luminance / brightness (ITU-R BT.601)
            val brightness = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            // Distance in RGB color space against map background
            val dr = r - MAP_R
            val dg = g - MAP_G
            val db = b - MAP_B
            val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()

            if (brightness < MIN_BRIGHTNESS) {
                MapContrastResult(
                    isValid = false,
                    brightness = brightness,
                    contrastDistance = dist,
                    message = "Color too dark (${(brightness * 100).toInt()}%). Minimum map visibility required is 22%."
                )
            } else if (dist < MIN_CONTRAST_DIST) {
                MapContrastResult(
                    isValid = false,
                    brightness = brightness,
                    contrastDistance = dist,
                    message = "Color blends with map sector background. Choose higher contrast."
                )
            } else {
                MapContrastResult(
                    isValid = true,
                    brightness = brightness,
                    contrastDistance = dist,
                    message = "Optimal map sector visibility (Contrast: ${(brightness * 100).toInt()}%)"
                )
            }
        } catch (e: Exception) {
            MapContrastResult(
                isValid = false,
                brightness = 0f,
                contrastDistance = 0f,
                message = "Failed to parse color values."
            )
        }
    }
}
