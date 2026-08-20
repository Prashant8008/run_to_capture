package com.example.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsKabaddi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.core.designsystem.RunColors

enum class RunNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    MAP("Map", Icons.Filled.Map, Icons.Outlined.Map, "nav_tab_map"),
    BATTLES("Battles", Icons.Filled.SportsKabaddi, Icons.Outlined.SportsKabaddi, "nav_tab_battles"),
    RANK("Rank", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_tab_rank"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
}

@Composable
fun RunBottomNavBar(
    selectedTab: RunNavTab,
    onTabSelected: (RunNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(RunColors.Glass)
            .border(
                width = 1.dp,
                color = RunColors.GlassBorder
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("run_bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RunNavTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                onTabSelected(tab)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag(tab.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = if (isSelected) RunColors.LimeDeep else RunColors.Faint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title.uppercase(),
                            color = if (isSelected) RunColors.LimeDeep else RunColors.Faint,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
