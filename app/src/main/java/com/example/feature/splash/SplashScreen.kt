package com.example.feature.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.RunColors
import com.example.domain.model.AuthState
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authRepository.authState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val progressAnimation by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        delay(1400)
        authRepository.checkSession()
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onNavigateToDashboard()
            }
            is AuthState.Unauthenticated -> {
                onNavigateToOnboarding()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunColors.Background)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background tactical radar/topo grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            for (x in 0..(size.width / step).toInt()) {
                for (y in 0..(size.height / step).toInt()) {
                    drawCircle(
                        color = Color(0x0C14171A),
                        radius = 1.2f,
                        center = Offset(x * step, y * step)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hexagon Tactical Logo
            Canvas(modifier = Modifier.size(64.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width * 0.45f
                val hexPath = Path()
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i) - 30.0)
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()

                drawPath(
                    path = hexPath,
                    color = RunColors.Ink,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Interior tactical lime cross-lines
                drawLine(
                    color = RunColors.Lime,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = RunColors.Lime,
                    start = Offset(center.x - radius * 0.866f, center.y - radius * 0.5f),
                    end = Offset(center.x + radius * 0.866f, center.y + radius * 0.5f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = RunColors.Lime,
                    start = Offset(center.x - radius * 0.866f, center.y + radius * 0.5f),
                    end = Offset(center.x + radius * 0.866f, center.y - radius * 0.5f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // RUN2CAPTURE Title
            Text(
                text = "RUN2CAPTURE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = RunColors.Ink
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cyan Pill: CLAIM THE MAP
            Surface(
                shape = RoundedCornerShape(50),
                color = RunColors.CyanTint,
                border = androidx.compose.foundation.BorderStroke(1.dp, RunColors.Cyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(RunColors.Cyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLAIM THE MAP",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = RunColors.CyanDeep
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Tactical Progress Bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(RunColors.GlassBorder)
            ) {
                Box(
                    modifier = Modifier
                        .width((140 * progressAnimation).dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(RunColors.Lime)
                )
            }
        }
    }
}
