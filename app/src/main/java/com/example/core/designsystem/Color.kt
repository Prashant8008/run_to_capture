package com.example.core.designsystem

import androidx.compose.ui.graphics.Color

object RunColors {
    // Brand Accent — Electric Lime
    val ElectricLime = Color(0xFFCCFF00)
    val ElectricLimeDim = Color(0xFF99BF00)
    val ElectricLimeContainer = Color(0xFF1E2E05)
    val OnElectricLime = Color(0xFF0F1A00)

    // Dark Tactical Backgrounds
    val Background = Color(0xFF0C0E12)
    val Surface = Color(0xFF14171D)
    val SurfaceVariant = Color(0xFF1D222A)
    val SurfaceHighlight = Color(0xFF262D38)
    val CardBorder = Color(0xFF2B3340)

    // Content Colors
    val OnBackground = Color(0xFFF1F3F5)
    val OnSurface = Color(0xFFE2E6EA)
    val OnSurfaceMuted = Color(0xFF8B95A5)
    val OnSurfaceSubtle = Color(0xFF5A6474)

    // Faction Colors (Explicitly distinct from Electric Lime)
    val FactionApexCrimson = Color(0xFFFF3B30)
    val FactionApexCrimsonContainer = Color(0xFF380E0B)
    
    val FactionCipherCyan = Color(0xFF00F0FF)
    val FactionCipherCyanContainer = Color(0xFF042930)
    
    val FactionSolarisAmber = Color(0xFFFF9500)
    val FactionSolarisAmberContainer = Color(0xFF381F02)

    // Status Colors
    val Success = Color(0xFF34C759)
    val Warning = Color(0xFFFFCC00)
    val Error = Color(0xFFFF453A)
    val Info = Color(0xFF5856D6)

    // Map Specific
    val GpsActive = Color(0xFF00F0FF)
    val GpsAccuracyCircle = Color(0x3300F0FF)
    val ActiveTrail = Color(0xFFCCFF00)
}

// Aliases for convenient usage across UI screens
val ColorElectricLime = RunColors.ElectricLime
val ColorApexRed = RunColors.FactionApexCrimson
val ColorCipherCyan = RunColors.FactionCipherCyan
val ColorSolarisGold = RunColors.FactionSolarisAmber
val ColorDarkBackground = RunColors.Background
val ColorDarkCard = RunColors.Surface
val ColorDarkSurfaceElevated = RunColors.SurfaceVariant
val ColorTextPrimary = RunColors.OnBackground
val ColorTextSecondary = RunColors.OnSurfaceMuted
