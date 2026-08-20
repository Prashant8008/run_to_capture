package com.example.feature.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.*
import com.example.core.designsystem.components.TacticalMapBackground
import com.example.domain.model.FlagBackground
import com.example.domain.model.FlagBorder
import com.example.domain.model.FlagConfig
import com.example.domain.model.FlagEmblem
import com.example.domain.model.FlagPattern
import com.example.feature.customization.components.ColorPicker
import com.example.feature.customization.components.FlagCanvas
import com.example.feature.customization.components.LiveFlagPreview
import com.example.feature.customization.components.LivePlayerPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagCreatorScreen(
    viewModel: FlagCreatorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    TacticalMapBackground(
        modifier = modifier.fillMaxSize(),
        showRadarPulse = true,
        overlayAlpha = 0.40f
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "CUSTOMIZATION STUDIO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "PHASE 4 // VISUAL IDENTITY & FLAG FORGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF659900),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111827)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFAFBF9).copy(alpha = 0.92f)
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Live Player Preview (Avatar + Username + Territory Color + Live Flag)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    LivePlayerPreview(
                        username = uiState.username,
                        faction = uiState.faction,
                        territoryColor = uiState.territoryColor,
                        flag = uiState.flag,
                        avatarUrl = uiState.avatarUrl
                    )
                }

                // 2. Live Flag Preview Banner
                item {
                    LiveFlagPreview(flag = uiState.flag)
                }

                // 3. Category Selector Tabs
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customization_tabs"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CustomizationTab.entries) { tab ->
                            val isSelected = uiState.activeTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTab(tab) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFFAFBF9).copy(alpha = 0.9f),
                                    labelColor = Color(0xFF475569),
                                    selectedContainerColor = Color(0xFFD0F838),
                                    selectedLabelColor = Color(0xFF111827)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = Color(0xFF8DC600)
                                ),
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                // 4. Tab Content Area
                item {
                    when (uiState.activeTab) {
                        CustomizationTab.COLOR -> {
                            ColorPicker(
                                selectedColor = uiState.territoryColor,
                                onColorSelected = { viewModel.selectTerritoryColor(it) }
                            )
                        }
                        CustomizationTab.BACKGROUND -> {
                            BackgroundSelector(
                                selectedBackground = uiState.flag.background,
                                onSelect = { viewModel.selectBackground(it) }
                            )
                        }
                        CustomizationTab.PATTERN -> {
                            PatternSelector(
                                flag = uiState.flag,
                                selectedPattern = uiState.flag.pattern,
                                onSelect = { viewModel.selectPattern(it) }
                            )
                        }
                        CustomizationTab.EMBLEM -> {
                            EmblemSelector(
                                flag = uiState.flag,
                                selectedEmblem = uiState.flag.emblem,
                                onSelect = { viewModel.selectEmblem(it) }
                            )
                        }
                        CustomizationTab.BORDER -> {
                            BorderSelector(
                                flag = uiState.flag,
                                selectedBorder = uiState.flag.border,
                                onSelect = { viewModel.selectBorder(it) }
                            )
                        }
                    }
                }

                // 5. Save Action Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.saveCustomization() },
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_customization_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0F838),
                            contentColor = Color(0xFF111827),
                            disabledContainerColor = Color(0xFFD0F838).copy(alpha = 0.5f),
                            disabledContentColor = Color(0xFF111827).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = Color(0xFF111827),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEPLOYING CONFIG...",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVE VISUAL IDENTITY",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundSelector(
    selectedBackground: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFBF9).copy(alpha = 0.95f))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("background_selector")
    ) {
        Text(
            text = "FLAG BACKGROUND BASE",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlagBackground.entries.forEach { bg ->
                val isSelected = selectedBackground.equals(bg.id, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFE6FCF8) else Color(0xFFF1F5F2))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF8DC600) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(bg.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("bg_option_${bg.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(bg.color)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                    Text(
                        text = bg.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color(0xFF111827) else Color(0xFF475569),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatternSelector(
    flag: FlagConfig,
    selectedPattern: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFBF9).copy(alpha = 0.95f))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("pattern_selector")
    ) {
        Text(
            text = "GEOMETRIC PATTERN DIVISION",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlagPattern.entries.forEach { pattern ->
                val isSelected = selectedPattern.equals(pattern.id, ignoreCase = true)
                Column(
                    modifier = Modifier
                        .width(155.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFE6FCF8) else Color(0xFFF1F5F2))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF8DC600) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(pattern.id) }
                        .padding(8.dp)
                        .testTag("pattern_option_${pattern.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlagCanvas(
                        flag = flag.copy(pattern = pattern.id),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        showBorder = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pattern.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFF111827) else Color(0xFF475569),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                    Text(
                        text = pattern.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmblemSelector(
    flag: FlagConfig,
    selectedEmblem: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFBF9).copy(alpha = 0.95f))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("emblem_selector")
    ) {
        Text(
            text = "TACTICAL SECTOR EMBLEMS",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlagEmblem.entries.forEach { emblem ->
                val isSelected = selectedEmblem.equals(emblem.id, ignoreCase = true)
                Column(
                    modifier = Modifier
                        .width(155.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFE6FCF8) else Color(0xFFF1F5F2))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF8DC600) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(emblem.id) }
                        .padding(8.dp)
                        .testTag("emblem_option_${emblem.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlagCanvas(
                        flag = flag.copy(emblem = emblem.id),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        showBorder = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = emblem.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFF111827) else Color(0xFF475569),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                    Text(
                        text = emblem.lore,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BorderSelector(
    flag: FlagConfig,
    selectedBorder: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFBF9).copy(alpha = 0.95f))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("border_selector")
    ) {
        Text(
            text = "FLAG BORDER & TRIM FRAME",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlagBorder.entries.forEach { border ->
                val isSelected = selectedBorder.equals(border.id, ignoreCase = true)
                Column(
                    modifier = Modifier
                        .width(155.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFE6FCF8) else Color(0xFFF1F5F2))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF8DC600) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(border.id) }
                        .padding(8.dp)
                        .testTag("border_option_${border.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlagCanvas(
                        flag = flag.copy(border = border.id),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        showBorder = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = border.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFF111827) else Color(0xFF475569),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

