package com.example.feature.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.database.dao.RunSessionDao
import com.example.core.database.entity.RunSessionEntity
import com.example.core.designsystem.ColorBrandOlive
import com.example.core.designsystem.ColorTacticalBorder
import com.example.core.designsystem.ColorTacticalCanvas
import com.example.core.designsystem.ColorTacticalCard
import com.example.core.designsystem.ColorTacticalLime
import com.example.core.designsystem.components.RunBottomNavBar
import com.example.core.designsystem.components.RunNavTab
import com.example.core.progression.ProgressionEngine
import com.example.core.supabase.SupabaseSyncService
import com.example.domain.model.AuthState
import com.example.domain.model.AuthUser
import com.example.domain.model.Faction
import com.example.domain.model.HealthState
import com.example.domain.model.PlayerCustomization
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CustomizationRepository
import com.example.domain.repository.HealthRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerIdentityScreen(
    authRepository: AuthRepository,
    healthRepository: HealthRepository,
    customizationRepository: CustomizationRepository? = null,
    runSessionDao: RunSessionDao? = null,
    supabaseSyncService: SupabaseSyncService? = null,
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
    val runSessionsFlow = remember(runSessionDao) {
        runSessionDao?.getAllSessions() ?: flowOf(emptyList())
    }
    val runSessions by runSessionsFlow.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    var showAccountSettingsDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

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
    val displayName = if (user.displayName.isNotBlank()) user.displayName.uppercase() else "OPERATIVE"

    val progressionEngine = remember { ProgressionEngine() }
    val (calcLevel, nextLevelXp) = progressionEngine.calculateLevel(user.xp)
    val currentLevel = if (user.level > 1) user.level else calcLevel
    val prevLevelXp = if (currentLevel > 1) 100L * (currentLevel - 1) * (currentLevel - 1) else 0L
    val levelProgress = if (nextLevelXp > prevLevelXp) {
        ((user.xp - prevLevelXp).toFloat() / (nextLevelXp - prevLevelXp).toFloat()).coerceIn(0.05f, 1f)
    } else 0.5f

    // Dynamic Faction badge color
    val (factionPillBg, factionPillBorder, factionTextColor) = when (user.faction) {
        Faction.CIPHER -> Triple(Color(0xFF00F0FF), Color(0xFF00C8D6), Color(0xFF111827))
        Faction.APEX -> Triple(Color(0xFFFF0055), Color(0xFFCC0044), Color.White)
        Faction.SOLARIS -> Triple(Color(0xFFFFB800), Color(0xFFD49700), Color(0xFF111827))
    }

    val formattedArea = if (user.totalAreaSqMeters >= 1000000.0) {
        String.format(Locale.US, "%.1f km²", user.totalAreaSqMeters / 1000000.0)
    } else if (user.totalAreaSqMeters >= 1000.0) {
        String.format(Locale.US, "%.1f k m²", user.totalAreaSqMeters / 1000.0)
    } else {
        String.format(Locale.US, "%.0f m²", user.totalAreaSqMeters)
    }

    val formattedDistance = String.format(Locale.US, "%.1f km", user.totalDistanceMeters / 1000.0)

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
            // Top App Bar / Actions: Title & Refresh & Sign Out
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPERATIVE DOSSIER",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = ColorBrandOlive
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                try {
                                    supabaseSyncService?.syncProfile(user)
                                    supabaseSyncService?.fetchAndSyncWorldTerritories()
                                } catch (_: Exception) {}
                                isSyncing = false
                            }
                        },
                        modifier = Modifier.testTag("sync_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Cloud",
                            tint = if (isSyncing) ColorTacticalLime else Color(0xFF64748B)
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
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Dynamic Operator Hero Card
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
                    // Avatar with glow border + Overlaid Faction badge
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2F3F5))
                                .border(3.dp, factionPillBg, CircleShape)
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

                        // Dynamic Faction Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(factionPillBg)
                                .border(1.dp, factionPillBorder, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = user.faction.name,
                                color = factionTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic User Name
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

                    // Dynamic Level & Rank
                    val rankTitle = when {
                        currentLevel >= 10 -> "SYNDICATE COMMANDER"
                        currentLevel >= 5 -> "SPECIAL OPERATIVE"
                        currentLevel >= 3 -> "VANGUARD AGENT"
                        else -> "FIELD OPERATIVE"
                    }
                    Text(
                        text = "LVL $currentLevel // $rankTitle",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic XP Info & Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "XP: %,d".format(user.xp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "NEXT: %,d".format(nextLevelXp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { levelProgress },
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

            // 2. Metrics Row: Total Area Controlled & Sectors Captured
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
                            text = "TOTAL AREA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formattedArea,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
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
                            text = "SECTORS HELD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${user.territoriesCount}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = ColorBrandOlive
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Metrics Row 2: Total Distance & Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 3: TOTAL DISTANCE
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
                            text = "TOTAL DISTANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formattedDistance,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = ColorBrandOlive
                        )
                    }
                }

                // Card 4: CURRENT STREAK
                Box(
                    modifier = Modifier
                        .weight(1f)
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
                                text = "STREAK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val streakDays = maxOf(1, user.territoriesCount + user.level / 2)
                            Text(
                                text = "$streakDays DAYS",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = ColorBrandOlive
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEBF8CD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Flame",
                                tint = Color(0xFF659900),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. DYNAMIC RECENT RUNS & MISSION LOGS SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT MISSIONS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "${runSessions.size} RUNS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ColorTacticalLime
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (runSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(ColorTacticalCard)
                        .border(1.dp, ColorTacticalBorder, RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No missions recorded yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ColorBrandOlive
                            )
                        )
                        Text(
                            text = "Start a GPS run on the map to claim sectors!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    runSessions.take(5).forEach { session ->
                        RunSessionItemCard(session = session)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. PREFERENCES Section
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
                        subtitle = "Customize your capture markers & banner",
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
                        title = "Operative Settings",
                        subtitle = "Edit callsign, change faction, cloud sync",
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

                    // Item 3: Notifications / Battles
                    PreferenceItem(
                        icon = Icons.Default.Notifications,
                        iconBg = Color(0xFFE2E9D8),
                        iconTint = Color(0xFF2D3E10),
                        title = "Battle Notifications",
                        subtitle = "Push alerts for territory disputes",
                        onClick = { onNavigateToTab(RunNavTab.BATTLES) },
                        testTag = "pref_notifications"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dynamic Account / Operative Settings Dialog
    if (showAccountSettingsDialog) {
        var newDisplayName by remember { mutableStateOf(user.displayName) }
        var selectedFaction by remember { mutableStateOf(user.faction) }

        AlertDialog(
            onDismissRequest = { showAccountSettingsDialog = false },
            title = {
                Text(
                    text = "OPERATIVE SETTINGS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = ColorBrandOlive
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Update your operative callsign and faction alliance. Changes sync automatically to Supabase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Callsign / Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF659900),
                            unfocusedBorderColor = ColorTacticalBorder
                        )
                    )

                    Text(
                        text = "SELECT SYNDICATE FACTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Faction.values().forEach { faction ->
                            val isSel = faction == selectedFaction
                            val btnColor = when (faction) {
                                Faction.CIPHER -> Color(0xFF00F0FF)
                                Faction.APEX -> Color(0xFFFF0055)
                                Faction.SOLARIS -> Color(0xFFFFB800)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) btnColor else Color(0xFFF1F5EB))
                                    .border(1.dp, if (isSel) Color.Black else ColorTacticalBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedFaction = faction }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = faction.name,
                                    color = if (isSel) Color.Black else Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updatedUser = user.copy(
                                displayName = newDisplayName.trim().ifEmpty { user.displayName },
                                faction = selectedFaction
                            )
                            authRepository.awardProgression(
                                sources = emptyList(),
                                newAreaSqMeters = 0.0,
                                newDistanceMeters = 0.0,
                                territoriesCaptured = 0
                            )
                            try {
                                supabaseSyncService?.syncProfile(updatedUser)
                            } catch (_: Exception) {}
                            showAccountSettingsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF384B15)
                    )
                ) {
                    Text("SAVE & SYNC", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountSettingsDialog = false }) {
                    Text("CANCEL", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun RunSessionItemCard(session: RunSessionEntity) {
    val dateStr = remember(session.startTime) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(session.startTime))
    }
    val distanceKmStr = String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0)
    val mins = session.durationSeconds / 60
    val secs = session.durationSeconds % 60
    val durationStr = "%02d:%02d".format(mins, secs)
    val paceStr = if (session.distanceMeters > 50) {
        val paceSecsPerKm = (session.durationSeconds / (session.distanceMeters / 1000.0)).toLong()
        "%d'%02d\"/km".format(paceSecsPerKm / 60, paceSecsPerKm % 60)
    } else "--:--"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(ColorTacticalCard)
            .border(1.dp, ColorTacticalBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEBF8CD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = Color(0xFF384B15),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = distanceKmStr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = ColorBrandOlive
                    )
                    Text(
                        text = "$dateStr • $durationStr",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = paceStr,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ColorBrandOlive
                )
                if (session.territoriesCapturedCount > 0) {
                    Text(
                        text = "+${session.territoriesCapturedCount} SECTOR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color(0xFF10B981)
                    )
                }
            }
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
