package com.example.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RunShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object CustomShapes {
    val Pill = RoundedCornerShape(50)
    val Viewport = RoundedCornerShape(32.dp)
    val Card = RoundedCornerShape(20.dp)
    val Badge = RoundedCornerShape(8.dp)
}
