package com.example.feature.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorCipherCyan
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorSolarisGold
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.core.designsystem.RunColors
import com.example.domain.model.Faction
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.UserLocation
import java.util.Locale

@Composable
fun LocationAcquisitionOverlay(
    visible: Boolean,
    userLocation: UserLocation?,
    gpsStatus: GpsSignalStatus,
    faction: Faction,
    username: String,
    onEnterMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factionColor = when (faction) {
        Faction.APEX -> ColorApexRed
        Faction.CIPHER -> ColorCipherCyan
        Faction.SOLARIS -> ColorSolarisGold
    }

    val isLocated = userLocation != null

    val infiniteTransition = rememberInfiniteTransition(label = "RadarScan")
    val radarPulse1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val radarPulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(750, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF090D16),
                            Color(0xFF04060A)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .testTag("location_acquisition_overlay")
        ) {
            // Background tactical grid canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 48.dp.toPx()
                val linePaint = Color(0x0C00F0FF)
                for (x in 0..(size.width / step).toInt()) {
                    drawLine(
                        color = linePaint,
                        start = Offset(x * step, 0f),
                        end = Offset(x * step, size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(size.height / step).toInt()) {
                    drawLine(
                        color = linePaint,
                        start = Offset(0f, y * step),
                        end = Offset(size.width, y * step),
                        strokeWidth = 1f
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tactical Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(factionColor.copy(alpha = 0.2f))
                                .border(1.5.dp, factionColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = factionColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = username.uppercase(Locale.US),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = ColorTextPrimary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "${faction.displayName.uppercase(Locale.US)} DIVISION",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = factionColor,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    // GPS Status Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLocated) Color(0xFF14532D).copy(alpha = 0.8f)
                                else Color(0xFF78350F).copy(alpha = 0.8f)
                            )
                            .border(
                                1.dp,
                                if (isLocated) Color(0xFF22C55E) else Color(0xFFF59E0B),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isLocated) Color(0xFF22C55E) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLocated) "GPS LOCKED" else "ACQUIRING",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                // Center: Radar Pulse Scanner & Satellite Crosshairs
                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated Concentric Pulse Rings
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxRadius = size.width / 2

                        // Outer fixed tactical ring
                        drawCircle(
                            color = factionColor.copy(alpha = 0.25f),
                            radius = maxRadius,
                            center = center,
                            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                        )
                        drawCircle(
                            color = factionColor.copy(alpha = 0.15f),
                            radius = maxRadius * 0.65f,
                            center = center,
                            style = Stroke(width = 1f)
                        )
                        drawCircle(
                            color = factionColor.copy(alpha = 0.1f),
                            radius = maxRadius * 0.35f,
                            center = center,
                            style = Stroke(width = 1f)
                        )

                        // Pulse Ring 1
                        val r1 = maxRadius * radarPulse1
                        val alpha1 = (1f - radarPulse1) * 0.6f
                        drawCircle(
                            color = if (isLocated) Color(0xFF22C55E).copy(alpha = alpha1) else factionColor.copy(alpha = alpha1),
                            radius = r1,
                            center = center,
                            style = Stroke(width = 2.5f)
                        )

                        // Pulse Ring 2
                        val r2 = maxRadius * radarPulse2
                        val alpha2 = (1f - radarPulse2) * 0.6f
                        drawCircle(
                            color = if (isLocated) Color(0xFF22C55E).copy(alpha = alpha2) else factionColor.copy(alpha = alpha2),
                            radius = r2,
                            center = center,
                            style = Stroke(width = 2.5f)
                        )

                        // Crosshair lines
                        drawLine(
                            color = factionColor.copy(alpha = 0.3f),
                            start = Offset(center.x - maxRadius, center.y),
                            end = Offset(center.x + maxRadius, center.y),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = factionColor.copy(alpha = 0.3f),
                            start = Offset(center.x, center.y - maxRadius),
                            end = Offset(center.x, center.y + maxRadius),
                            strokeWidth = 1f
                        )
                    }

                    // Rotating Radar Sweep Needle
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(radarRotation)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxRadius = size.width / 2
                        drawLine(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    factionColor.copy(alpha = 0.8f),
                                    factionColor.copy(alpha = 0.0f)
                                ),
                                center = center,
                                radius = maxRadius
                            ),
                            start = center,
                            end = Offset(center.x, center.y - maxRadius),
                            strokeWidth = 2f
                        )
                    }

                    // Center Satellite / GPS Icon Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLocated) Color(0xFF15803D)
                                else Color(0xFF0F172A)
                            )
                            .border(
                                width = 3.dp,
                                color = if (isLocated) Color(0xFF4ADE80) else factionColor,
                                shape = CircleShape
                            )
                            .shadow(16.dp, CircleShape, spotColor = factionColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLocated) Icons.Default.CheckCircle else Icons.Default.LocationSearching,
                            contentDescription = "GPS Fix",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Telemetry Data Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                        .border(1.5.dp, if (isLocated) Color(0xFF22C55E).copy(alpha = 0.6f) else Color(0xFF334155), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLocated) "COORDINATES LOCKED" else "ESTABLISHING GPS SATELLITE UPLINK",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isLocated) Color(0xFF4ADE80) else ColorTextPrimary,
                            letterSpacing = 1.2.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isLocated) "Tactical grid synchronized. Transitioning to live map view..."
                               else "Triangulating orbital constellation & sector telemetry...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ColorTextSecondary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Coordinates Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LATITUDE",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (userLocation != null) String.format(Locale.US, "%.5f°", userLocation.latitude) else "37.7749°",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLocated) factionColor else ColorTextSecondary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(Color(0xFF334155))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LONGITUDE",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (userLocation != null) String.format(Locale.US, "%.5f°", userLocation.longitude) else "-122.4194°",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLocated) factionColor else ColorTextSecondary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(Color(0xFF334155))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ACCURACY",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (userLocation != null) "±${userLocation.accuracyMeters.toInt()}m" else "CALIBRATING",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLocated) Color(0xFF22C55E) else Color(0xFFF59E0B)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isLocated) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = factionColor,
                            trackColor = Color(0xFF334155)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { 1.0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF22C55E),
                            trackColor = Color(0xFF14532D)
                        )
                    }
                }

                // Bottom Action Button: Quick Enter or Skip
                Button(
                    onClick = onEnterMapClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("enter_tactical_map_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocated) Color(0xFF22C55E) else factionColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isLocated) Icons.Default.Map else Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isLocated) "REVEAL TACTICAL MAP" else "ENTER MAP DIRECTLY",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
