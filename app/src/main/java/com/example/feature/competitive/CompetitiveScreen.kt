package com.example.feature.competitive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.*
import com.example.core.designsystem.components.RunCard
import com.example.domain.model.Challenge
import com.example.domain.model.LeaderboardCategory
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.LeaderboardPeriod
import com.example.feature.customization.components.FlagCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitiveScreen(
    viewModel: CompetitiveViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ColorDarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("COMPETITIVE NETWORK", color = ColorTextPrimary, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorElectricLime)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorDarkBackground)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = ColorDarkBackground,
                contentColor = ColorElectricLime,
                indicator = { tabPositions ->
                    if (uiState.selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                            color = ColorElectricLime
                        )
                    }
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("LEADERBOARDS", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("CHALLENGES", fontWeight = FontWeight.Bold) }
                )
            }

            if (uiState.selectedTab == 0) {
                LeaderboardContent(
                    uiState = uiState,
                    onCategorySelected = viewModel::selectCategory,
                    onPeriodSelected = viewModel::selectPeriod
                )
            } else {
                ChallengesContent(uiState = uiState)
            }
        }
    }
}

@Composable
fun LeaderboardContent(
    uiState: CompetitiveUiState,
    onCategorySelected: (LeaderboardCategory) -> Unit,
    onPeriodSelected: (LeaderboardPeriod) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Categories
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LeaderboardCategory.values()) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorElectricLime,
                        selectedLabelColor = ColorDarkBackground
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Periods
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LeaderboardPeriod.values()) { period ->
                FilterChip(
                    selected = uiState.selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(period.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorElectricLime,
                        selectedLabelColor = ColorDarkBackground
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorElectricLime)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.leaderboardEntries) { entry ->
                    LeaderboardItem(entry)
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val factionColor = when (entry.faction) {
        com.example.domain.model.Faction.APEX -> ColorApexRed
        com.example.domain.model.Faction.CIPHER -> ColorCipherCyan
        com.example.domain.model.Faction.SOLARIS -> ColorSolarisGold
    }

    RunCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.rank <= 3) ColorSolarisGold else ColorTextSecondary,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(40.dp)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(factionColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = factionColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.faction.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = factionColor
                )
            }

            if (entry.flagConfig != null) {
                FlagCanvas(
                    flag = entry.flagConfig,
                    modifier = Modifier
                        .size(36.dp, 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp, 24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = entry.formattedScore,
                style = MaterialTheme.typography.titleMedium,
                color = ColorElectricLime,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ChallengesContent(uiState: CompetitiveUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(uiState.challenges) { challenge ->
            ChallengeItem(challenge)
        }
    }
}

@Composable
fun ChallengeItem(challenge: Challenge) {
    RunCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (challenge.isCompleted) Icons.Default.EmojiEvents else Icons.Default.Stars,
                        contentDescription = null,
                        tint = if (challenge.isCompleted) ColorSolarisGold else ColorElectricLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "${challenge.rewardXp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorElectricLime,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${challenge.type.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTextSecondary
                )
                Text(
                    text = "${challenge.currentProgress.toInt()} / ${challenge.targetValue.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { if (challenge.targetValue > 0) (challenge.currentProgress.toFloat() / challenge.targetValue.toFloat()).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (challenge.isCompleted) ColorSolarisGold else ColorElectricLime,
                trackColor = ColorDarkSurfaceElevated
            )
        }
    }
}
