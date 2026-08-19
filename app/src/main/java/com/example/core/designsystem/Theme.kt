package com.example.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    darkTheme: Boolean = true, // Dark theme is default for Run2Capture
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RunDarkColorScheme,
        typography = RunTypography,
        shapes = RunShapes,
        content = content
    )
}
