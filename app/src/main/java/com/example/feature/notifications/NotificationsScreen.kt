package com.example.feature.notifications

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorBrandOlive
import com.example.core.designsystem.ColorTacticalBorder
import com.example.core.designsystem.ColorTacticalCanvas
import com.example.core.designsystem.ColorTacticalCard
import com.example.core.designsystem.ColorTacticalLime
import com.example.core.designsystem.components.RunBottomNavBar
import com.example.core.designsystem.components.RunNavTab

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToUrl: (String) -> Unit = {},
    onNavigateToTab: (RunNavTab) -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        containerColor = ColorTacticalCanvas,
        bottomBar = {
            RunBottomNavBar(
                selectedTab = RunNavTab.BATTLES,
                onTabSelected = { tab ->
                    if (tab == RunNavTab.MAP) onNavigateBack()
                    else onNavigateToTab(tab)
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar: RUN2CAPTURE logo pill & Notification Bell (Matching Image 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RUN2CAPTURE Logo pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE2E9D8))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC7F338)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "RUN2CAPTURE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        ),
                        color = ColorBrandOlive
                    )
                }

                // Notification Bell
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorTacticalCard)
                        .border(1.dp, ColorTacticalBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Subtitle with Filter Icon (Matching Image 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Battles",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = ColorBrandOlive
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Territory conflicts in progress.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                }

                // Filter icon button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorTacticalCard)
                        .border(1.dp, ColorTacticalBorder, RoundedCornerShape(12.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Battles",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Active Contested Battle Card (Matching Image 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(ColorTacticalCard)
                    .border(1.dp, ColorTacticalBorder, RoundedCornerShape(22.dp))
                    .padding(18.dp)
                    .testTag("active_battle_card")
            ) {
                Column {
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDE9FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "GHOST_RUNNER",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = ColorBrandOlive
                                )
                                Text(
                                    text = "SECTOR 7G",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // IN PROGRESS Amber Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFDF0D5))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD97706))
                                )
                                Text(
                                    text = "IN PROGRESS",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF85580C)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STAKE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "450 XP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = ColorBrandOlive
                            )
                        }

                        // View Map -> Pill button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8D5), RoundedCornerShape(50))
                                .clickable { onNavigateBack() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "View Map",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorBrandOlive
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ColorBrandOlive,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Empty State Contested Card with dashed border (Matching Image 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFAFBF6))
                    .border(1.5.dp, Color(0xFFD6DEC7), RoundedCornerShape(22.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Satellite Icon in soft square
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE8EEDB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SatelliteAlt,
                            contentDescription = null,
                            tint = ColorBrandOlive,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No active battles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = ColorBrandOlive
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "You currently have no contested territories. Find a new sector to claim.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Lime Pill Button: [target icon] Find a Territory
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTacticalLime,
                            contentColor = Color(0xFF111827)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(50))
                            .testTag("find_territory_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Find a Territory",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
