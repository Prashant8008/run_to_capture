package com.example.feature.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorBrandOlive
import com.example.core.designsystem.ColorCipherCyan
import com.example.core.designsystem.ColorSolarisGold
import com.example.core.designsystem.ColorTacticalBorder
import com.example.core.designsystem.ColorTacticalCanvas
import com.example.core.designsystem.ColorTacticalCard
import com.example.core.designsystem.ColorTacticalLime
import com.example.core.designsystem.components.RunBottomNavBar
import com.example.core.designsystem.components.RunNavTab
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
    onNavigateToTab: (RunNavTab) -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authRepository.authState.collectAsState()
    val healthState by healthRepository.healthState.collectAsState(initial = HealthState.Idle)
    val customState by (customizationRepository?.customizationState?.collectAsState()
        ?: remember { mutableStateOf(PlayerCustomization()) })
    val coroutineScope = rememberCoroutineScope()
    var showAccountSettingsDialog by remember { mutableStateOf(false) }

    val user: AuthUser = when (val state = authState) {
        is AuthState.Authenticated -> state.user
        else -> AuthUser(
            id = "unknown",
            email = "operator_01@run2capture.io",
            displayName = "OPERATOR_01",
            faction = Faction.CIPHER
        )
    }

    val activeTerritoryColor = customState.territoryColor.ifEmpty { user.territoryColor }
    val activeFlag = customState.flag
    val displayName = if (user.displayName.isNotBlank()) user.displayName.uppercase() else "OPERATOR_01"

    Scaffold(
        containerColor = ColorTacticalCanvas,
        bottomBar = {
            RunBottomNavBar(
                selectedTab = RunNavTab.PROFILE,
                onTabSelected = { tab ->
                    if (tab == RunNavTab.MAP) onNavigateToMap()
                    else onNavigateToTab(tab)
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("player_identity_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar / Actions: Title & Sign Out
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROFILE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = ColorBrandOlive
                )

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
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Operator Hero Card (Matching Image 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(ColorTacticalCard)
                    .border(1.dp, ColorTacticalBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .testTag("operator_profile_card"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with glow border + Overlaid NEON VANGUARD cyan pill badge
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2F3F5))
                                .border(3.dp, Color(0xFF00F0FF), CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Operator Avatar",
                                tint = Color(0xFF0D6B69),
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        // Cyan Pill: NEON VANGUARD
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF00F0FF))
                                .border(1.dp, Color(0xFF00C8D6), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "NEON VANGUARD",
                                color = Color(0xFF111827),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // OPERATOR_01 Name
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = ColorBrandOlive
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // LVL 42 // ELITE
                    Text(
                        text = "LVL 42 // ELITE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP Info & Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "XP: 14,250",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "NEXT: 15,000",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { 0.95f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ColorTacticalLime,
                        trackColor = Color(0xFFE2E8D5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Metrics Row: Total Area Controlled & Sectors Captured (Matching Image 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: TOTAL AREA CONTROLLED
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(3.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColorTacticalCard)
                        .border(1.dp, ColorTacticalBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL AREA CONTROLLED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "12.4 km²",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = ColorBrandOlive
                        )
                    }
                }

                // Card 2: SECTORS CAPTURED
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(3.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColorTacticalCard)
                        .border(1.dp, ColorTacticalBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "SECTORS CAPTURED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "48",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = ColorBrandOlive
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Full Width Card: CURRENT STREAK (Matching Image 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorTacticalCard)
                    .border(1.dp, ColorTacticalBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT STREAK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "14 DAYS",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = ColorBrandOlive
                        )
                    }

                    // Lime flame badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEBF8CD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Flame",
                            tint = Color(0xFF659900),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PREFERENCES Section (Matching Image 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preferences Card Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorTacticalCard)
                    .border(1.dp, ColorTacticalBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 6.dp)
            ) {
                Column {
                    // Item 1: Territory Color & Flag
                    PreferenceItem(
                        icon = Icons.Outlined.Palette,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Color(0xFF7C3AED),
                        title = "Territory Color & Flag",
                        subtitle = "Customize your capture markers",
                        onClick = onNavigateToCustomization,
                        testTag = "pref_color_flag"
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 16.dp)
                            .background(Color(0xFFF1F5EB))
                    )

                    // Item 2: Account Settings
                    PreferenceItem(
                        icon = Icons.Default.Settings,
                        iconBg = Color(0xFFE2E9D8),
                        iconTint = Color(0xFF2D3E10),
                        title = "Account Settings",
                        subtitle = "Privacy, linked socials, security",
                        onClick = { showAccountSettingsDialog = true },
                        testTag = "pref_account_settings"
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 16.dp)
                            .background(Color(0xFFF1F5EB))
                    )

                    // Item 3: Notifications
                    PreferenceItem(
                        icon = Icons.Default.Notifications,
                        iconBg = Color(0xFFE2E9D8),
                        iconTint = Color(0xFF2D3E10),
                        title = "Notifications",
                        subtitle = "Push alerts for territory disputes",
                        onClick = { onNavigateToTab(RunNavTab.BATTLES) },
                        testTag = "pref_notifications"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreferenceItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ColorBrandOlive
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF64748B)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}
