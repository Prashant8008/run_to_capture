package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CustomShapes
import com.example.core.designsystem.RunColors
import com.example.core.designsystem.RunSpacing

@Composable
fun RunCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = RunColors.CardBorderLight,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: Dp = RunSpacing.m,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x0F000000),
                spotColor = Color(0x14000000)
            ),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Soft embossed tactile tile for icons and buttons (matching mockups 01, 02, 06, 07, 08)
 */
@Composable
fun TactileTile(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color(0xFFFAFBF7),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color(0x14000000),
                spotColor = Color(0x1A000000)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, Color(0xFFE2E5DC), shape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}

