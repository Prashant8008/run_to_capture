package com.example.domain.model

enum class PoiType(val label: String, val icon: String, val colorHex: String) {
    SUPPLY_DROP("SUPPLY CACHE", "📦", "#FFB800"),
    ENERGY_CELL("ENERGY CELL", "⚡", "#00F0FF"),
    FACTION_BEACON("FACTION OUTPOST", "🚩", "#CCFF00"),
    DEFENSE_RADAR("RADAR TOWER", "📡", "#FF3366")
}

data class TacticalPoi(
    val id: String,
    val name: String,
    val type: PoiType,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val bonusXp: Int = 150,
    val rewardText: String = "+150 XP • Territory Boost"
)
