package com.example.feature.customization.components

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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.domain.model.FlagConfig

/**
 * Live Flag Preview Banner showcasing the rendered flag with tactical framing.
 */
@Composable
fun LiveFlagPreview(
    flag: FlagConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorDarkCard)
            .border(1.dp, Color(0xFF272D38), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("live_flag_preview")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row with status badge
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
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = ColorElectricLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "STANDARD // LIVE FLAG BANNER",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorElectricLime,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${flag.patternEnum.displayName.uppercase()} // ${flag.emblemEnum.displayName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Flag Render Canvas
            FlagCanvas(
                flag = flag,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("flag_canvas")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Configuration Details Matrix
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorDarkSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SpecChip(label = "BG", value = flag.backgroundEnum.displayName)
                SpecChip(label = "PATTERN", value = flag.patternEnum.displayName)
                SpecChip(label = "EMBLEM", value = flag.emblemEnum.displayName)
                SpecChip(label = "BORDER", value = flag.borderEnum.displayName)
            }
        }
    }
}

@Composable
private fun SpecChip(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = ColorTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
