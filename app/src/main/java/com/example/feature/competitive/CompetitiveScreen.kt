package com.example.feature.competitive

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorBrandOlive
import com.example.core.designsystem.ColorTacticalBorder
import com.example.core.designsystem.ColorTacticalCanvas
import com.example.core.designsystem.ColorTacticalCard
import com.example.core.designsystem.components.RunBottomNavBar
import com.example.core.designsystem.components.RunNavTab
import com.example.domain.model.Faction
import com.example.domain.model.LeaderboardEntry

enum class LeaderboardFilter(val title: String) {
    LOCAL("Local"),
    FACTION("Faction"),
    GLOBAL("Global")
}

@Composable
fun CompetitiveScreen(
    viewModel: CompetitiveViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToTab: (RunNavTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(LeaderboardFilter.LOCAL) }

    Scaffold(
        containerColor = ColorTacticalCanvas,
        bottomBar = {
            RunBottomNavBar(
                selectedTab = RunNavTab.RANK,
                onTabSelected = { tab ->
                    if (tab == RunNavTab.MAP) onNavigateBack()
                    else onNavigateToTab(tab)
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("competitive_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Headline: LEADERBOARD (Matching Image 2)
            Text(
                text = "LEADERBOARD",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = ColorBrandOlive
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Global ranking and territory influence.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Filter Tabs Container: Local | Faction | Global (Matching Image 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE8EEDB))
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LeaderboardFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) Color(0xFF384B15) else Color.Transparent)
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.title,
                                color = if (isSelected) Color.White else Color(0xFF384B15),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leaderboard Entries
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF659900))
                }
            } else {
                val entries = if (uiState.leaderboardEntries.isNotEmpty()) {
                    uiState.leaderboardEntries
                } else {
                    listOf(
                        LeaderboardEntry(rank = 1, userId = "1", displayName = "NIGHT_HAWK", avatarUrl = null, faction = Faction.APEX, flagConfig = null, score = 14200.0, formattedScore = "14.2k"),
                        LeaderboardEntry(rank = 2, userId = "2", displayName = "ECHO_GHOST", avatarUrl = null, faction = Faction.CIPHER, flagConfig = null, score = 12800.0, formattedScore = "12.8k"),
                        LeaderboardEntry(rank = 3, userId = "3", displayName = "VIPER_ZERO", avatarUrl = null, faction = Faction.SOLARIS, flagConfig = null, score = 11500.0, formattedScore = "11.5k"),
                        LeaderboardEntry(rank = 12, userId = "12", displayName = "OPERATOR_01", avatarUrl = null, faction = Faction.CIPHER, flagConfig = null, score = 8200.0, formattedScore = "8.2k"),
                        LeaderboardEntry(rank = 13, userId = "13", displayName = "STEALTH_RUN", avatarUrl = null, faction = Faction.APEX, flagConfig = null, score = 8100.0, formattedScore = "8.1k")
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(entries) { entry ->
                        LeaderboardRowItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(entry: LeaderboardEntry) {
    val isCurrentUser = entry.displayName.contains("OPERATOR_01", ignoreCase = true)
    val cardBg = if (isCurrentUser) Color(0xFFEAF4D0) else ColorTacticalCard
    val borderColor = if (isCurrentUser) Color(0xFFC7E88A) else ColorTacticalBorder

    val dotColor = when (entry.faction) {
        Faction.CIPHER -> Color(0xFF00F0FF) // Cyan
        Faction.SOLARIS -> Color(0xFF8B5CF6) // Purple / Gold
        Faction.APEX -> Color(0xFF10B981) // Green / Red
    }

    val sectorName = when (entry.rank) {
        1 -> "Sector 7 • Alpha Vanguard"
        2 -> "Sector 4 • Omega Syndicate"
        3 -> "Sector 9 • Alpha Vanguard"
        12 -> "Sector 2 • Neutral Grid"
        13 -> "Sector 4 • Omega Syndicate"
        else -> "Sector ${entry.rank} • Regional"
    }

    val displayArea = if (entry.formattedScore.isNotBlank()) {
        entry.formattedScore
    } else if (entry.score >= 1000) {
        String.format("%.1fk", entry.score / 1000.0)
    } else {
        "${entry.score.toInt()}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isCurrentUser) 4.dp else 2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(if (isCurrentUser) "current_user_rank_card" else "rank_item_${entry.rank}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank Number + Avatar + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank number
                Text(
                    text = "${entry.rank}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = if (isCurrentUser) Color(0xFF2D3E10) else Color(0xFF64748B),
                    modifier = Modifier.width(24.dp)
                )

                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentUser) Color(0xFFD6ECC0) else Color(0xFFE2E9D8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isCurrentUser) Color(0xFF384B15) else Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.displayName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            ),
                            color = ColorBrandOlive
                        )

                        // Faction Dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = sectorName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Stat on Right: 14.2k AREA (SQM)
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = displayArea,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = ColorBrandOlive
                )
                Text(
                    text = "AREA (SQM)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
