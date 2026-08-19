package com.example.feature.customization.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.domain.model.FlagBackground
import com.example.domain.model.FlagBorder
import com.example.domain.model.FlagConfig
import com.example.domain.model.FlagEmblem
import com.example.domain.model.FlagPattern
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity Vector & Geometric Flag Renderer.
 * Renders combinations of Backgrounds, Patterns, Emblems, and Borders.
 */
@Composable
fun FlagCanvas(
    flag: FlagConfig,
    modifier: Modifier = Modifier,
    showBorder: Boolean = true
) {
    Box(
        modifier = modifier
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0D12))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // 1. Draw Background
            drawRect(
                color = flag.backgroundEnum.color,
                size = size
            )

            // 2. Draw Geometric Pattern
            drawFlagPattern(flag.patternEnum, w, h, flag.backgroundEnum)

            // 3. Draw Tactical Emblem
            drawFlagEmblem(flag.emblemEnum, w, h)

            // 4. Specular Sheen & Grid Weave Overlay for Tactile Depth
            drawTacticalOverlay(w, h)

            // 5. Draw Border Frame
            if (showBorder && flag.borderEnum != FlagBorder.NONE) {
                drawFlagBorder(flag.borderEnum, w, h)
            }
        }
    }
}

private fun DrawScope.drawFlagPattern(
    pattern: FlagPattern,
    w: Float,
    h: Float,
    bg: FlagBackground
) {
    val patternColor = when (bg) {
        FlagBackground.NAVY, FlagBackground.OBSIDIAN, FlagBackground.CYBER_BLACK, FlagBackground.CHARCOAL ->
            Color.White.copy(alpha = 0.18f)
        FlagBackground.CRIMSON, FlagBackground.EMERALD, FlagBackground.AMETHYST, FlagBackground.ROYAL_BLUE ->
            Color(0xFFFFD700).copy(alpha = 0.22f)
        FlagBackground.GOLD ->
            Color(0xFF0D1B2A).copy(alpha = 0.25f)
    }

    when (pattern) {
        FlagPattern.SOLID -> {
            // Subtle fine diagonal hatch
            var x = 0f
            while (x < w + h) {
                drawLine(
                    color = patternColor.copy(alpha = 0.06f),
                    start = Offset(x, 0f),
                    end = Offset(x - h, h),
                    strokeWidth = 1.5f
                )
                x += 16f
            }
        }
        FlagPattern.DIAGONAL -> {
            // Bold forward tactical sash
            val path = Path().apply {
                moveTo(w * 0.25f, 0f)
                lineTo(w * 0.55f, 0f)
                lineTo(w * 0.75f, h)
                lineTo(w * 0.45f, h)
                close()
            }
            drawPath(path, color = patternColor)
        }
        FlagPattern.STRIPES_VERTICAL -> {
            // Center third stripe
            drawRect(
                color = patternColor,
                topLeft = Offset(w / 3f, 0f),
                size = Size(w / 3f, h)
            )
        }
        FlagPattern.STRIPES_HORIZONTAL -> {
            // Center horizontal band
            drawRect(
                color = patternColor,
                topLeft = Offset(0f, h / 3f),
                size = Size(w, h / 3f)
            )
        }
        FlagPattern.CROSS -> {
            // Sector Command Cross
            val thicknessW = w * 0.18f
            val thicknessH = h * 0.22f
            // Vertical bar
            drawRect(
                color = patternColor,
                topLeft = Offset((w - thicknessW) / 2f, 0f),
                size = Size(thicknessW, h)
            )
            // Horizontal bar
            drawRect(
                color = patternColor,
                topLeft = Offset(0f, (h - thicknessH) / 2f),
                size = Size(w, thicknessH)
            )
        }
        FlagPattern.CHEVRON -> {
            // Dual military rank chevrons
            val path1 = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.5f, h * 0.5f)
                lineTo(0f, h)
                lineTo(w * 0.2f, h)
                lineTo(w * 0.7f, h * 0.5f)
                lineTo(w * 0.2f, 0f)
                close()
            }
            drawPath(path1, color = patternColor)
        }
        FlagPattern.SPLIT_DIAGONAL -> {
            // Corner-to-corner triangular split
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(0f, h)
                close()
            }
            drawPath(path, color = patternColor)
        }
        FlagPattern.CHECKER -> {
            // Coordinate 4x3 matrix grid
            val cols = 4
            val rows = 3
            val cw = w / cols
            val ch = h / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if ((r + c) % 2 == 1) {
                        drawRect(
                            color = patternColor,
                            topLeft = Offset(c * cw, r * ch),
                            size = Size(cw, ch)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawFlagEmblem(
    emblem: FlagEmblem,
    w: Float,
    h: Float
) {
    val cx = w / 2f
    val cy = h / 2f
    val radius = h * 0.32f

    // Emblem gold/silver core color
    val emblemColor = Color(0xFFFFD700)
    val emblemStroke = Color(0xFF14171D)

    // Center circular backing plate
    drawCircle(
        color = Color(0xFF0E121A).copy(alpha = 0.65f),
        radius = radius * 1.15f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = emblemColor.copy(alpha = 0.5f),
        radius = radius * 1.15f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
    )

    when (emblem) {
        FlagEmblem.WOLF -> {
            // Apex predator wolf silhouette
            val path = Path().apply {
                moveTo(cx - radius * 0.6f, cy + radius * 0.5f)
                lineTo(cx - radius * 0.8f, cy - radius * 0.2f)
                lineTo(cx - radius * 0.4f, cy - radius * 0.7f) // Left ear
                lineTo(cx - radius * 0.15f, cy - radius * 0.3f)
                lineTo(cx + radius * 0.15f, cy - radius * 0.3f)
                lineTo(cx + radius * 0.4f, cy - radius * 0.7f) // Right ear
                lineTo(cx + radius * 0.8f, cy - radius * 0.2f)
                lineTo(cx + radius * 0.6f, cy + radius * 0.5f)
                lineTo(cx, cy + radius * 0.8f) // Muzzle
                close()
            }
            drawPath(path, color = emblemColor)
            // Wolf eyes
            drawCircle(color = Color(0xFF00F0FF), radius = radius * 0.1f, center = Offset(cx - radius * 0.25f, cy - radius * 0.05f))
            drawCircle(color = Color(0xFF00F0FF), radius = radius * 0.1f, center = Offset(cx + radius * 0.25f, cy - radius * 0.05f))
        }
        FlagEmblem.EAGLE -> {
            // Imperial eagle with spread wings
            val path = Path().apply {
                moveTo(cx, cy - radius * 0.8f) // Head
                lineTo(cx + radius * 0.2f, cy - radius * 0.5f)
                lineTo(cx + radius * 0.85f, cy - radius * 0.3f) // Right wingtip
                lineTo(cx + radius * 0.4f, cy + radius * 0.3f)
                lineTo(cx + radius * 0.2f, cy + radius * 0.75f) // Tail
                lineTo(cx, cy + radius * 0.5f)
                lineTo(cx - radius * 0.2f, cy + radius * 0.75f)
                lineTo(cx - radius * 0.4f, cy + radius * 0.3f)
                lineTo(cx - radius * 0.85f, cy - radius * 0.3f) // Left wingtip
                lineTo(cx - radius * 0.2f, cy - radius * 0.5f)
                close()
            }
            drawPath(path, color = emblemColor)
        }
        FlagEmblem.FALCON -> {
            // Swift diving falcon
            val path = Path().apply {
                moveTo(cx, cy + radius * 0.8f) // Dive beak
                lineTo(cx + radius * 0.3f, cy + radius * 0.2f)
                lineTo(cx + radius * 0.9f, cy - radius * 0.6f) // High wing
                lineTo(cx + radius * 0.1f, cy - radius * 0.4f)
                lineTo(cx, cy - radius * 0.8f)
                lineTo(cx - radius * 0.1f, cy - radius * 0.4f)
                lineTo(cx - radius * 0.9f, cy - radius * 0.6f)
                lineTo(cx - radius * 0.3f, cy + radius * 0.2f)
                close()
            }
            drawPath(path, color = emblemColor)
        }
        FlagEmblem.SKULL -> {
            // Combat angular skull
            val path = Path().apply {
                moveTo(cx - radius * 0.55f, cy - radius * 0.2f)
                lineTo(cx - radius * 0.55f, cy - radius * 0.6f)
                lineTo(cx, cy - radius * 0.8f)
                lineTo(cx + radius * 0.55f, cy - radius * 0.6f)
                lineTo(cx + radius * 0.55f, cy - radius * 0.2f)
                lineTo(cx + radius * 0.35f, cy + radius * 0.2f)
                lineTo(cx + radius * 0.3f, cy + radius * 0.65f) // Jaw
                lineTo(cx - radius * 0.3f, cy + radius * 0.65f)
                lineTo(cx - radius * 0.35f, cy + radius * 0.2f)
                close()
            }
            drawPath(path, color = emblemColor)
            // Eye sockets
            drawCircle(color = Color(0xFF0E121A), radius = radius * 0.14f, center = Offset(cx - radius * 0.22f, cy - radius * 0.15f))
            drawCircle(color = Color(0xFF0E121A), radius = radius * 0.14f, center = Offset(cx + radius * 0.22f, cy - radius * 0.15f))
        }
        FlagEmblem.SHIELD -> {
            // Aegis shield with internal cross
            val path = Path().apply {
                moveTo(cx - radius * 0.65f, cy - radius * 0.7f)
                lineTo(cx + radius * 0.65f, cy - radius * 0.7f)
                lineTo(cx + radius * 0.65f, cy)
                lineTo(cx, cy + radius * 0.85f)
                lineTo(cx - radius * 0.65f, cy)
                close()
            }
            drawPath(path, color = emblemColor)
            drawPath(path, color = emblemStroke, style = Stroke(width = 3f))
        }
        FlagEmblem.BOLT -> {
            // High-voltage lightning bolt
            val path = Path().apply {
                moveTo(cx + radius * 0.2f, cy - radius * 0.85f)
                lineTo(cx - radius * 0.6f, cy)
                lineTo(cx - radius * 0.05f, cy)
                lineTo(cx - radius * 0.3f, cy + radius * 0.85f)
                lineTo(cx + radius * 0.6f, cy - radius * 0.1f)
                lineTo(cx + radius * 0.05f, cy - radius * 0.1f)
                close()
            }
            drawPath(path, color = Color(0xFFCCFF00))
        }
        FlagEmblem.BLADE -> {
            // Dual crossed combat blades
            drawLine(
                color = emblemColor,
                start = Offset(cx - radius * 0.65f, cy - radius * 0.65f),
                end = Offset(cx + radius * 0.65f, cy + radius * 0.65f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = emblemColor,
                start = Offset(cx + radius * 0.65f, cy - radius * 0.65f),
                end = Offset(cx - radius * 0.65f, cy + radius * 0.65f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawCircle(color = Color(0xFF00F0FF), radius = radius * 0.2f, center = Offset(cx, cy))
        }
        FlagEmblem.STAR -> {
            // Crisp 5-point commander star
            val path = Path()
            val points = 5
            val outer = radius * 0.8f
            val inner = radius * 0.35f
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) outer else inner
                val angle = Math.toRadians((i * 36 - 90).toDouble())
                val x = (cx + r * cos(angle)).toFloat()
                val y = (cy + r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = emblemColor)
        }
        FlagEmblem.CROWN -> {
            // 3-Peak royal hegemony crown
            val path = Path().apply {
                moveTo(cx - radius * 0.6f, cy + radius * 0.4f)
                lineTo(cx - radius * 0.7f, cy - radius * 0.4f)
                lineTo(cx - radius * 0.25f, cy)
                lineTo(cx, cy - radius * 0.7f)
                lineTo(cx + radius * 0.25f, cy)
                lineTo(cx + radius * 0.7f, cy - radius * 0.4f)
                lineTo(cx + radius * 0.6f, cy + radius * 0.4f)
                close()
            }
            drawPath(path, color = emblemColor)
        }
        FlagEmblem.DRAGON -> {
            // Tactical wyrm dragon
            val path = Path().apply {
                moveTo(cx - radius * 0.7f, cy)
                lineTo(cx - radius * 0.4f, cy - radius * 0.6f)
                lineTo(cx, cy - radius * 0.8f)
                lineTo(cx + radius * 0.5f, cy - radius * 0.5f)
                lineTo(cx + radius * 0.8f, cy - radius * 0.1f)
                lineTo(cx + radius * 0.3f, cy + radius * 0.2f)
                lineTo(cx + radius * 0.6f, cy + radius * 0.7f)
                lineTo(cx, cy + radius * 0.4f)
                lineTo(cx - radius * 0.5f, cy + radius * 0.7f)
                close()
            }
            drawPath(path, color = Color(0xFFFF3B30))
        }
        FlagEmblem.RADAR -> {
            // Cyber sweep radar
            drawCircle(color = Color(0xFF00F0FF), radius = radius * 0.75f, center = Offset(cx, cy), style = Stroke(width = 2.5f))
            drawCircle(color = Color(0xFF00F0FF).copy(alpha = 0.5f), radius = radius * 0.45f, center = Offset(cx, cy), style = Stroke(width = 2f))
            drawLine(color = Color(0xFF00F0FF), start = Offset(cx - radius * 0.75f, cy), end = Offset(cx + radius * 0.75f, cy), strokeWidth = 2f)
            drawLine(color = Color(0xFF00F0FF), start = Offset(cx, cy - radius * 0.75f), end = Offset(cx, cy + radius * 0.75f), strokeWidth = 2f)
            drawLine(color = Color(0xFFCCFF00), start = Offset(cx, cy), end = Offset(cx + radius * 0.65f, cy - radius * 0.4f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        FlagEmblem.CIRCUIT -> {
            // Cipher circuit matrix
            drawRect(
                color = Color(0xFF00F0FF),
                topLeft = Offset(cx - radius * 0.35f, cy - radius * 0.35f),
                size = Size(radius * 0.7f, radius * 0.7f)
            )
            // Traces
            drawLine(color = Color(0xFF00F0FF), start = Offset(cx - radius * 0.8f, cy - radius * 0.2f), end = Offset(cx - radius * 0.35f, cy - radius * 0.2f), strokeWidth = 3f)
            drawLine(color = Color(0xFF00F0FF), start = Offset(cx + radius * 0.35f, cy + radius * 0.2f), end = Offset(cx + radius * 0.8f, cy + radius * 0.2f), strokeWidth = 3f)
            drawLine(color = Color(0xFF00F0FF), start = Offset(cx, cy - radius * 0.8f), end = Offset(cx, cy - radius * 0.35f), strokeWidth = 3f)
            drawLine(color = Color(0xFF00F0FF), start = Offset(cx, cy + radius * 0.35f), end = Offset(cx, cy + radius * 0.8f), strokeWidth = 3f)
        }
    }
}

private fun DrawScope.drawTacticalOverlay(w: Float, h: Float) {
    // Subtle top-left to bottom-right sheen gradient
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.35f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        size = size
    )
}

private fun DrawScope.drawFlagBorder(border: FlagBorder, w: Float, h: Float) {
    val strokeWidth = border.strokeWidthDp * density

    when (border) {
        FlagBorder.DOUBLE_GOLD -> {
            // Outer gold border
            drawRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(w - strokeWidth, h - strokeWidth),
                style = Stroke(width = strokeWidth * 0.6f)
            )
            // Inner gold border
            val innerInset = strokeWidth * 1.8f
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                topLeft = Offset(innerInset, innerInset),
                size = Size(w - innerInset * 2, h - innerInset * 2),
                style = Stroke(width = strokeWidth * 0.4f)
            )
        }
        FlagBorder.NEON_CYAN -> {
            // Neon cyber glow stroke
            drawRect(
                color = Color(0xFF00F0FF),
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(w - strokeWidth, h - strokeWidth),
                style = Stroke(width = strokeWidth)
            )
        }
        else -> {
            drawRect(
                color = border.color,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(w - strokeWidth, h - strokeWidth),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
