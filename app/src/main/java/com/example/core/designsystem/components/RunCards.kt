package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CustomShapes
import com.example.core.designsystem.RunColors
import com.example.core.designsystem.RunSpacing

@Composable
fun RunCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = RunColors.Surface,
    borderColor: Color = RunColors.CardBorder,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = RunSpacing.m,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CustomShapes.Card,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
