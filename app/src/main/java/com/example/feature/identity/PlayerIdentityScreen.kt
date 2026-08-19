package com.example.feature.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorCipherCyan
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorSolarisGold
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Palette
import com.example.core.designsystem.components.RunBadge
import com.example.core.designsystem.components.RunBadgeVariant
import com.example.core.designsystem.components.RunCard
import com.example.core.designsystem.components.RunPrimaryButton
import com.example.core.designsystem.components.RunSecondaryButton
import com.example.domain.model.AuthState
import com.example.domain.model.AuthUser
import com.example.domain.model.Faction
import com.example.domain.model.HealthState
import com.example.domain.model.PlayerCustomization
import com.example.domain.model.StandardTerritoryColor
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CustomizationRepository
import com.example.domain.repository.HealthRepository
import com.example.feature.customization.components.FlagCanvas
import kotlinx.coroutines.launch

@Composable
fun PlayerIdentityScreen(
    authRepository: AuthRepository,
    healthRepository: HealthRepository,
    customizationRepository: CustomizationRepository? = null,
    onNavigateToMap: () -> Unit = {},
    onNavigateToCustomization: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authRepository.authState.collectAsState()
    val healthState by healthRepository.healthState.collectAsState(initial = HealthState.Idle)
    val customState by (customizationRepository?.customizationState?.collectAsState() 
        ?: remember { androidx.compose.runtime.mutableStateOf(PlayerCustomization()) })
    val coroutineScope = rememberCoroutineScope()

    val user: AuthUser = when (val state = authState) {
        is AuthState.Authenticated -> state.user
        else -> AuthUser(
            id = "unknown",
            email = "unauthenticated@sector.io",
            displayName = "Operative",
            faction = Faction.CIPHER
        )
    }

    val activeTerritoryColor = customState.territoryColor.ifEmpty { user.territoryColor }
    val activeFlag = customState.flag

    val factionColor = when (user.faction) {
        Faction.APEX -> ColorApexRed
        Faction.CIPHER -> ColorCipherCyan
        Faction.SOLARIS -> ColorSolarisGold
    }

    val territoryColorObj = StandardTerritoryColor.parseColor(activeTerritoryColor)
    val territoryHex = StandardTerritoryColor.getHexForColor(activeTerritoryColor)

    Scaffold(
        containerColor = ColorDarkBackground,
        modifier = modifier.fillMaxSize().testTag("player_identity_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section: Back to Map, Faction Header & Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToMap,
                        modifier = Modifier.testTag("back_to_map_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Radar Map",
                            tint = ColorElectricLime
                        )
                    }
                    RunBadge(
                        text = "STATUS // AUTHENTICATED",
                        variant = RunBadgeVariant.PRIMARY
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                            onLogout()
                        }
                    },
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = ColorTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Player Avatar & Call-sign Card
            RunCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(factionColor.copy(alpha = 0.2f))
                            .border(2.dp, factionColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = factionColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(factionColor)
                        )
                        Text(
                            text = "${user.faction.displayName} SYNDICATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = factionColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Visual Customization & Flag Card (Phase 4)
            RunCard(
                modifier = Modifier.fillMaxWidth().testTag("player_customization_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = ColorElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "VISUAL IDENTITY & FLAG",
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Territory Color Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorDarkSurfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(territoryColorObj)
                            )
                            Text(
                                text = territoryHex,
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mini Flag Banner Preview
                    FlagCanvas(
                        flag = activeFlag,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("identity_flag_preview")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Emblem: ${activeFlag.emblemEnum.displayName} • Pattern: ${activeFlag.patternEnum.displayName} • Border: ${activeFlag.borderEnum.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    RunPrimaryButton(
                        text = "CUSTOMIZE COLORS & FLAG",
                        onClick = onNavigateToCustomization,
                        modifier = Modifier.fillMaxWidth().testTag("customize_flag_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Vault Status Card
            RunCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ColorElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SECURITY VAULT // ANDROID KEYSTORE",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorDarkSurfaceElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ColorElectricLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AES-256 GCM Hardware Token Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = ColorElectricLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorDarkSurfaceElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = ColorTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auth Provider: ${user.authProvider.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary
                            )
                        }
                        Text(
                            text = "SECURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorElectricLime,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Career Stats
            RunCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "OPERATIVE STATS",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LEVEL ${user.level}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorElectricLime,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${user.xp} / ${user.nextLevelXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextSecondary
                            )
                        }
                        
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { if (user.nextLevelXp > 0) (user.xp.toFloat() / user.nextLevelXp) else 0f },
                            modifier = Modifier.width(150.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = ColorElectricLime,
                            trackColor = ColorDarkSurfaceElevated
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Territories",
                            value = "${user.territoriesCount}",
                            icon = Icons.Default.Map,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Area Claimed",
                            value = "${user.totalAreaSqMeters.toInt()} m²",
                            icon = Icons.Default.LocationOn,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Distance Run",
                            value = "${(user.totalDistanceMeters / 1000.0).format(1)} km",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Achievements Card
            if (user.achievements.isNotEmpty()) {
                RunCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ACHIEVEMENTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        user.achievements.forEach { achId ->
                            val ach = com.example.core.progression.Achievement.values().find { it.id == achId }
                            if (ach != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ColorSolarisGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = ach.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ColorTextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ach.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Backend Health Check Status
            RunCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "BACKEND SATELLITE LINK",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (healthState) {
                                is HealthState.Loading -> "Pinging backend server..."
                                is HealthState.Success -> "Connected (Status: ${(healthState as HealthState.Success).status})"
                                is HealthState.Error -> "Offline / Dev Simulation Mode"
                                else -> "Ready to ping backend"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (healthState) {
                                is HealthState.Success -> ColorElectricLime
                                is HealthState.Error -> ColorApexRed
                                else -> ColorTextPrimary
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                healthRepository.checkHealth()
                            }
                        },
                        modifier = Modifier.testTag("ping_health_button")
                    ) {
                        if (healthState is HealthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ColorElectricLime, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ping",
                                tint = ColorElectricLime
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign out button
            RunSecondaryButton(
                text = "Revoke Credentials & Sign Out",
                onClick = {
                    coroutineScope.launch {
                        authRepository.logout()
                        onLogout()
                    }
                },
                modifier = Modifier.testTag("revoke_credentials_button")
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorElectricLime,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = ColorTextPrimary,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorTextSecondary
        )
    }
}

private fun Double.format(digits: Int): String = String.format("%.${digits}f", this)
