package com.example.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.RunColors

data class OnboardingStep(
    val phaseBadge: String,
    val stepEyebrow: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val trackingParam: String,
    val rewardParam: String,
    val minLoopParam: String,
    val signalParam: String
)

private val steps = listOf(
    OnboardingStep(
        phaseBadge = "PHASE 01 // LOCOMOTION",
        stepEyebrow = "STEP ONE // MOVEMENT PROTOCOL",
        title = "Every Run Becomes Ground You Hold.",
        description = "Your GPS trace draws the perimeter. Close the loop in the real world and the territory is yours to defend.",
        icon = Icons.Default.DirectionsRun,
        trackingParam = "High-Precision GPS",
        rewardParam = "Polygon Capture",
        minLoopParam = "400m Perimeter",
        signalParam = "Outdoor Required"
    ),
    OnboardingStep(
        phaseBadge = "PHASE 02 // TERRITORY",
        stepEyebrow = "STEP TWO // GROUND CONTROL",
        title = "Claim It. Shape It. Defend It.",
        description = "Closed loops become polygons on the live map. Expand your borders, or lose them to whoever runs harder.",
        icon = Icons.Default.Map,
        trackingParam = "Real-Time Perimeter",
        rewardParam = "Faction Expansion",
        minLoopParam = "600m Boundary",
        signalParam = "Zero Signal Loss"
    ),
    OnboardingStep(
        phaseBadge = "PHASE 03 // SYNDICATE DOMINANCE",
        stepEyebrow = "STEP THREE // ALIGNMENT",
        title = "Ascend The Factions.",
        description = "Pledge to a faction and stack your captures toward regional dominance. Every meter you hold counts for your colors.",
        icon = Icons.Default.Shield,
        trackingParam = "Syndicate Sync",
        rewardParam = "Weekly Multipliers",
        minLoopParam = "Territory Defense",
        signalParam = "GPS + Biometrics"
    )
)

@Composable
fun CornerBracketWrap(
    modifier: Modifier = Modifier,
    icon: ImageVector
) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // 4 Corner Brackets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bracketSize = 22.dp.toPx()
            val stroke = 3.dp.toPx()
            val color = RunColors.Lime

            // Top-Left
            drawLine(color, Offset(0f, 0f), Offset(bracketSize, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(0f, bracketSize), strokeWidth = stroke, cap = StrokeCap.Round)

            // Top-Right
            drawLine(color, Offset(size.width, 0f), Offset(size.width - bracketSize, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, bracketSize), strokeWidth = stroke, cap = StrokeCap.Round)

            // Bottom-Left
            drawLine(color, Offset(0f, size.height), Offset(bracketSize, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - bracketSize), strokeWidth = stroke, cap = StrokeCap.Round)

            // Bottom-Right
            drawLine(color, Offset(size.width, size.height), Offset(size.width - bracketSize, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - bracketSize), strokeWidth = stroke, cap = StrokeCap.Round)
        }

        // Centered Icon Tile
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFEDECE7))
                .border(1.dp, RunColors.GlassBorder, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RunColors.Ink,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Scaffold(
        containerColor = RunColors.Background,
        modifier = modifier.fillMaxSize().testTag("onboarding_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row with Cyan Status Pill and Skip Briefing
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Linear Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(RunColors.GlassBorder)
                    ) {
                        val progressFraction = (currentStepIndex + 1f) / steps.size.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(RunColors.Lime)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RunColors.CyanTint,
                            border = BorderStroke(1.dp, RunColors.Cyan.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(RunColors.Cyan)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = step.phaseBadge,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = RunColors.CyanDeep
                                    )
                                )
                            }
                        }

                        if (currentStepIndex < steps.size - 1) {
                            TextButton(
                                onClick = onNavigateToLogin,
                                modifier = Modifier.testTag("onboarding_skip_button")
                            ) {
                                Text(
                                    text = "SKIP BRIEFING",
                                    color = RunColors.Body,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = "STEP 3 / 3",
                                color = RunColors.Body,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                // Center Dynamic Step Hero Section
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "step_hero_transition"
                ) { currentStep ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // Tactical Corner-Bracket Wrapped Icon Tile
                        CornerBracketWrap(icon = currentStep.icon)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Eyebrow Lime
                        Text(
                            text = currentStep.stepEyebrow,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = RunColors.LimeDeep
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Headline
                        Text(
                            text = currentStep.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                letterSpacing = (-0.4).sp
                            ),
                            color = RunColors.Ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Body Subtitle
                        Text(
                            text = currentStep.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontSize = 13.5.sp
                            ),
                            color = RunColors.Body,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress Indicator Dots (20dp wide active)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            steps.indices.forEach { index ->
                                val isCurrent = index == currentStepIndex
                                Box(
                                    modifier = Modifier
                                        .size(width = if (isCurrent) 20.dp else 6.dp, height = 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) RunColors.LimeDeep else Color(0xFFD7D5CE))
                                )
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Primary Lime Button: ENLIST NOW
                    Button(
                        onClick = {
                            if (currentStepIndex < steps.size - 1) {
                                currentStepIndex++
                            } else {
                                onNavigateToRegister()
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RunColors.Lime,
                            contentColor = RunColors.LimeText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(999.dp),
                                ambientColor = Color(0x33CFF23A),
                                spotColor = Color(0x66CFF23A)
                            )
                            .testTag(if (currentStepIndex == steps.size - 1) "onboarding_register_button" else "onboarding_next_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (currentStepIndex == steps.size - 1) "ENLIST NOW" else "CONTINUE BRIEFING",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (currentStepIndex == steps.size - 1) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Ghost White Button: SIGN IN WITH EXISTING ID
                        Surface(
                            onClick = onNavigateToLogin,
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, RunColors.GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(4.dp, RoundedCornerShape(999.dp))
                                .testTag("onboarding_login_button")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SIGN IN WITH EXISTING ID",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        color = RunColors.Ink
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
