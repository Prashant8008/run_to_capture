package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.CustomShapes
import com.example.core.designsystem.RunColors
import com.example.core.designsystem.RunTypography

enum class RunBadgeVariant {
    PRIMARY,
    ACCENT,
    ERROR,
    WARNING,
    MUTED
}

@Composable
fun RunBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: RunBadgeVariant = RunBadgeVariant.PRIMARY
) {
    val dotColor = when (variant) {
        RunBadgeVariant.PRIMARY -> RunColors.ElectricLime
        RunBadgeVariant.ACCENT -> RunColors.FactionCipherCyan
        RunBadgeVariant.ERROR -> RunColors.Error
        RunBadgeVariant.WARNING -> RunColors.Warning
        RunBadgeVariant.MUTED -> RunColors.OnSurfaceMuted
    }

    val backgroundColor = when (variant) {
        RunBadgeVariant.PRIMARY -> RunColors.ElectricLimeContainer
        RunBadgeVariant.ACCENT -> RunColors.FactionCipherCyanContainer
        RunBadgeVariant.ERROR -> RunColors.FactionApexCrimsonContainer
        RunBadgeVariant.WARNING -> RunColors.FactionSolarisAmberContainer
        RunBadgeVariant.MUTED -> RunColors.SurfaceVariant
    }

    val textColor = when (variant) {
        RunBadgeVariant.PRIMARY -> RunColors.ElectricLime
        RunBadgeVariant.ACCENT -> RunColors.FactionCipherCyan
        RunBadgeVariant.ERROR -> RunColors.Error
        RunBadgeVariant.WARNING -> RunColors.FactionSolarisAmber
        RunBadgeVariant.MUTED -> RunColors.OnSurfaceMuted
    }

    RunStatusBadge(
        text = text,
        modifier = modifier,
        dotColor = dotColor,
        backgroundColor = backgroundColor,
        borderColor = dotColor.copy(alpha = 0.4f),
        textColor = textColor
    )
}

@Composable
fun RunStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = RunColors.ElectricLime,
    backgroundColor: Color = RunColors.SurfaceVariant,
    borderColor: Color = RunColors.CardBorder,
    textColor: Color = RunColors.OnSurface
) {
    Surface(
        modifier = modifier,
        shape = CustomShapes.Pill,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text.uppercase(),
                style = RunTypography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
