package com.example.feature.customization.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.domain.model.Faction
import com.example.domain.model.FlagConfig
import com.example.domain.model.StandardTerritoryColor

/**
 * Live Operative Preview Card showing:
 * - Avatar with glowing faction & territory halo
 * - Call-sign / Username
 * - Faction Syndicate Badge
 * - Territory Color swatch & sector map visibility status
 * - Live Flag Banner
 */
@Composable
fun LivePlayerPreview(
    username: String,
    faction: Faction,
    territoryColor: String,
    flag: FlagConfig,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val parsedTerritoryColor = StandardTerritoryColor.parseColor(territoryColor)
    val colorHex = StandardTerritoryColor.getHexForColor(territoryColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFBF9).copy(alpha = 0.98f),
                        Color(0xFFF1F5F2).copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.5.dp, parsedTerritoryColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("live_player_preview")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Operative Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = ColorElectricLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIVE OPERATIVE IDENTITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF659900),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Faction Tag
                Text(
                    text = faction.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = faction.primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Player Profile Row (Avatar + Username + Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactical Avatar with dual color ring (Faction + Custom Territory Color)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F2))
                        .border(3.dp, parsedTerritoryColor, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = faction.primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Identity info column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = username.ifEmpty { "OPERATIVE" },
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("preview_username")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Territory Color Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(parsedTerritoryColor)
                                .testTag("preview_color_dot")
                        )
                        Text(
                            text = "Territory: $colorHex",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Standard: ${flag.emblemEnum.displayName} (${flag.patternEnum.displayName})",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Sector Hexagon Sample
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.width * 0.45f
                        val path = Path()
                        for (i in 0 until 6) {
                            val angle = Math.toRadians((i * 60 - 30).toDouble())
                            val x = (cx + r * kotlin.math.cos(angle)).toFloat()
                            val y = (cy + r * kotlin.math.sin(angle)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = parsedTerritoryColor.copy(alpha = 0.35f))
                        drawPath(
                            path,
                            color = parsedTerritoryColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Flag Banner Preview in Player Card
            FlagCanvas(
                flag = flag,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("preview_flag_canvas")
            )
        }
    }
}
