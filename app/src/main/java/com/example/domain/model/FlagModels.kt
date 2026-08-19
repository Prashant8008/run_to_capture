package com.example.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Structured flag components: Background, Pattern, Emblem, Border.
 */
enum class FlagBackground(
    val id: String,
    val displayName: String,
    val hexColor: String,
    val color: Color
) {
    NAVY("navy", "Deep Navy", "#0D1B2A", Color(0xFF0D1B2A)),
    CRIMSON("crimson", "Crimson Red", "#8B0000", Color(0xFF8B0000)),
    EMERALD("emerald", "Emerald Green", "#064E3B", Color(0xFF064E3B)),
    GOLD("gold", "Royal Gold", "#78590F", Color(0xFF78590F)),
    ROYAL_BLUE("royal_blue", "Royal Blue", "#1E3A8A", Color(0xFF1E3A8A)),
    OBSIDIAN("obsidian", "Obsidian", "#111827", Color(0xFF111827)),
    AMETHYST("amethyst", "Amethyst", "#4C1D95", Color(0xFF4C1D95)),
    CHARCOAL("charcoal", "Charcoal Slate", "#27272A", Color(0xFF27272A)),
    CYBER_BLACK("cyber_black", "Cyber Black", "#0A0D12", Color(0xFF0A0D12));

    companion object {
        fun fromId(id: String): FlagBackground =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: NAVY
    }
}

enum class FlagPattern(
    val id: String,
    val displayName: String,
    val description: String
) {
    SOLID("solid", "Solid Weave", "Uniform tactical weave"),
    DIAGONAL("diagonal", "Diagonal Sash", "Strike angle division"),
    STRIPES_VERTICAL("stripes_vertical", "Vertical Stripes", "Tricolor sector division"),
    STRIPES_HORIZONTAL("stripes_horizontal", "Horizontal Bands", "Parallel frequency bands"),
    CROSS("cross", "Sector Cross", "Quadrant command cross"),
    CHEVRON("chevron", "Chevron Strike", "Forward military rank"),
    SPLIT_DIAGONAL("split_diagonal", "Split Angle", "Dual-tone angular field"),
    CHECKER("checker", "Tactical Grid", "Coordinate matrix grid");

    companion object {
        fun fromId(id: String): FlagPattern =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: DIAGONAL
    }
}

enum class FlagEmblem(
    val id: String,
    val displayName: String,
    val lore: String
) {
    WOLF("wolf", "Apex Wolf", "Predatory speed and pack dominance"),
    EAGLE("eagle", "Imperial Eagle", "Aerial superiority and high vigil"),
    FALCON("falcon", "Swift Falcon", "High velocity sector strikes"),
    SKULL("skull", "Combat Skull", "Relentless front-line vanguard"),
    SHIELD("shield", "Aegis Shield", "Impenetrable perimeter defense"),
    BOLT("bolt", "Volt Lightning", "Instantaneous kinetic surge"),
    BLADE("blade", "Dual Blades", "Close-quarters sector claiming"),
    STAR("star", "Sector Star", "Five-point command authority"),
    CROWN("crown", "Hegemony Crown", "Absolute territory dominion"),
    DRAGON("dragon", "Wyrm Dragon", "Ancient power and resilience"),
    RADAR("radar", "Radar Scan", "Geospatial telemetry and tracking"),
    CIRCUIT("circuit", "Cipher Circuit", "Encrypted digital intelligence");

    companion object {
        fun fromId(id: String): FlagEmblem =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: WOLF
    }
}

enum class FlagBorder(
    val id: String,
    val displayName: String,
    val color: Color,
    val strokeWidthDp: Float
) {
    NONE("none", "Borderless", Color.Transparent, 0f),
    GOLD("gold", "Gilded Gold", Color(0xFFFFD700), 3f),
    SILVER("silver", "Titanium Silver", Color(0xFFE2E8F0), 3f),
    NEON_CYAN("neon_cyan", "Cyber Cyan Glow", Color(0xFF00F0FF), 3f),
    CRIMSON("crimson", "Vanguard Red", Color(0xFFFF3B30), 3f),
    DOUBLE_GOLD("double_gold", "Imperial Double Gold", Color(0xFFFFD700), 4f),
    CARBON("carbon", "Carbon Weave", Color(0xFF3F3F46), 3f);

    companion object {
        fun fromId(id: String): FlagBorder =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: GOLD
    }
}

/**
 * Structured Flag configuration data model.
 */
data class FlagConfig(
    val background: String = "navy",
    val pattern: String = "diagonal",
    val emblem: String = "wolf",
    val border: String = "gold"
) {
    val backgroundEnum: FlagBackground get() = FlagBackground.fromId(background)
    val patternEnum: FlagPattern get() = FlagPattern.fromId(pattern)
    val emblemEnum: FlagEmblem get() = FlagEmblem.fromId(emblem)
    val borderEnum: FlagBorder get() = FlagBorder.fromId(border)
}

/**
 * Full visual customization state for a player.
 */
data class PlayerCustomization(
    val territoryColor: String = "cyan",
    val flag: FlagConfig = FlagConfig()
) {
    val territoryColorHex: String get() = StandardTerritoryColor.getHexForColor(territoryColor)
    val isCustomColor: Boolean get() = StandardTerritoryColor.fromId(territoryColor) == null
}
