package com.example.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.core.designsystem.components.RunBadge
import com.example.core.designsystem.components.RunBadgeVariant
import com.example.core.designsystem.components.RunPrimaryButton
import com.example.core.designsystem.components.RunSecondaryButton

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badge: String
)

private val steps = listOf(
    OnboardingStep(
        title = "Claim Real World Ground",
        subtitle = "TURF CAPTURE VIA RUNNING",
        description = "Turn every outdoor sprint, jog, or tactical walk into polygon territory in real time with high-precision GPS tracking.",
        icon = Icons.Default.DirectionsRun,
        badge = "PHASE 01 // LOCOMOTION"
    ),
    OnboardingStep(
        title = "Encircle To Capture",
        subtitle = "GEOMETRIC CLOSED LOOPS",
        description = "Run a closed geometric loop around city blocks and parks. Run2Capture automatically validates and binds the territory to your faction.",
        icon = Icons.Default.Map,
        badge = "PHASE 02 // SECTOR CONTROL"
    ),
    OnboardingStep(
        title = "Ascend The Factions",
        subtitle = "APEX // CIPHER // SOLARIS",
        description = "Align with a global syndicate. Compete for regional dominance, maintain daily turf streaks, and defend your sector perimeter.",
        icon = Icons.Default.Shield,
        badge = "PHASE 03 // SYNDICATE DOMINANCE"
    )
)

@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Scaffold(
        containerColor = ColorDarkBackground,
        modifier = modifier.fillMaxSize().testTag("onboarding_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunBadge(
                    text = step.badge,
                    variant = RunBadgeVariant.ACCENT
                )
                if (currentStepIndex < steps.size - 1) {
                    TextButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Skip Briefing",
                            color = ColorTextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Center Animated Step Content
            AnimatedContent(
                targetState = step,
                label = "step_transition"
            ) { currentStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ColorDarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = currentStep.title,
                            tint = ColorElectricLime,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = currentStep.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorElectricLime,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentStep.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom Controls (Pagers + CTA)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStepIndex) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentStepIndex) ColorElectricLime else ColorDarkSurfaceElevated
                                )
                        )
                    }
                }

                if (currentStepIndex < steps.size - 1) {
                    RunPrimaryButton(
                        text = "Continue Briefing",
                        onClick = { currentStepIndex++ },
                        modifier = Modifier.testTag("onboarding_next_button")
                    )
                } else {
                    RunPrimaryButton(
                        text = "Enlist Now",
                        onClick = onNavigateToRegister,
                        modifier = Modifier.testTag("onboarding_register_button")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    RunSecondaryButton(
                        text = "Sign In with Existing ID",
                        onClick = onNavigateToLogin,
                        modifier = Modifier.testTag("onboarding_login_button")
                    )
                }
            }
        }
    }
}
