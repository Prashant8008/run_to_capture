package com.example.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RunLightColorScheme = lightColorScheme(
    primary = RunColors.ElectricLimeDark,
    onPrimary = Color.White,
    primaryContainer = RunColors.ElectricLimePill,
    onPrimaryContainer = Color(0xFF1E2E05),
    
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    
    background = RunColors.LightBackground,
    onBackground = RunColors.LightOnBackground,
    
    surface = RunColors.LightSurface,
    onSurface = RunColors.LightOnSurface,
    surfaceVariant = RunColors.LightSurfaceVariant,
    onSurfaceVariant = RunColors.LightOnSurfaceMuted,
    
    outline = RunColors.LightCardBorder,
    outlineVariant = Color(0xFFE2E8F0),
    
    error = RunColors.Error,
    onError = Color.White
)

private val RunDarkColorScheme = darkColorScheme(
    primary = RunColors.ElectricLime,
    onPrimary = RunColors.OnElectricLime,
    primaryContainer = RunColors.ElectricLimeContainer,
    onPrimaryContainer = RunColors.ElectricLime,
    
    secondary = RunColors.FactionCipherCyan,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = RunColors.FactionCipherCyanContainer,
    onSecondaryContainer = RunColors.FactionCipherCyan,
    
    tertiary = RunColors.FactionSolarisAmber,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = RunColors.FactionSolarisAmberContainer,
    onTertiaryContainer = RunColors.FactionSolarisAmber,
    
    background = RunColors.Background,
    onBackground = RunColors.OnBackground,
    
    surface = RunColors.Surface,
    onSurface = RunColors.OnSurface,
    surfaceVariant = RunColors.SurfaceVariant,
    onSurfaceVariant = RunColors.OnSurfaceMuted,
    
    outline = RunColors.CardBorder,
    outlineVariant = RunColors.SurfaceHighlight,
    
    error = RunColors.Error,
    onError = Color.White
)

@Composable
fun Run2CaptureTheme(
    darkTheme: Boolean = false, // Liquid Glass Light theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RunDarkColorScheme else RunLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RunTypography,
        shapes = RunShapes,
        content = content
    )
}

