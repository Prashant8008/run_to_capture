package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class Faction(
    val id: String,
    val displayName: String,
    val hexColor: String,
    val lore: String
) {
    APEX(
        id = "APEX",
        displayName = "Apex Vanguard",
        hexColor = "#FF3B30",
        lore = "Relentless speed & offensive dominance."
    ),
    CIPHER(
        id = "CIPHER",
        displayName = "Cipher Syndicate",
        hexColor = "#00F0FF",
        lore = "Tactical strategy & encrypted precision."
    ),
    SOLARIS(
        id = "SOLARIS",
        displayName = "Solaris Collective",
        hexColor = "#FF9500",
        lore = "Endurance & collective solar energy."
    );

    val primaryColor: Color
        get() = when (this) {
            APEX -> Color(0xFFFF3B30)
            CIPHER -> Color(0xFF00F0FF)
            SOLARIS -> Color(0xFFFF9500)
        }

    val description: String get() = lore

    companion object {
        fun fromId(id: String): Faction {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CIPHER
        }
    }
}
