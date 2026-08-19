package com.example.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorCipherCyan
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorSolarisGold
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.core.designsystem.components.RunBadge
import com.example.core.designsystem.components.RunBadgeVariant
import com.example.core.designsystem.components.RunPrimaryButton
import com.example.core.designsystem.components.RunTextField
import com.example.domain.model.AuthState
import com.example.domain.model.Faction

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToDashboard()
        }
    }

    Scaffold(
        containerColor = ColorDarkBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            RunBadge(
                text = "OPERATIVE ENLISTMENT",
                variant = RunBadgeVariant.PRIMARY
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Join Run2Capture",
                style = MaterialTheme.typography.headlineMedium,
                color = ColorTextPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Choose your faction and claim ground.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner
            val currentError = uiState.generalError ?: (authState as? AuthState.Error)?.message
            if (!currentError.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorApexRed.copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = ColorApexRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentError,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorApexRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Display Name (Call-sign)
            RunTextField(
                value = uiState.displayNameInput,
                onValueChange = viewModel::onDisplayNameChanged,
                label = "Operative Call-Sign",
                placeholder = "Viper_09",
                leadingIcon = Icons.Default.Badge,
                isError = uiState.displayNameError != null,
                errorMessage = uiState.displayNameError,
                modifier = Modifier.testTag("signup_display_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email
            RunTextField(
                value = uiState.emailInput,
                onValueChange = viewModel::onEmailChanged,
                label = "Encrypted Email",
                placeholder = "agent@sector.io",
                leadingIcon = Icons.Default.Email,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.testTag("signup_email_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password
            RunTextField(
                value = uiState.passwordInput,
                onValueChange = viewModel::onPasswordChanged,
                label = "Password (min 8 chars)",
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = uiState.isPasswordVisible,
                onPasswordVisibilityToggle = viewModel::togglePasswordVisibility,
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                keyboardType = KeyboardType.Password,
                modifier = Modifier.testTag("signup_password_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Password
            RunTextField(
                value = uiState.confirmPasswordInput,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = "Confirm Password",
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = uiState.isPasswordVisible,
                onPasswordVisibilityToggle = viewModel::togglePasswordVisibility,
                keyboardType = KeyboardType.Password,
                modifier = Modifier.testTag("signup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Faction Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SELECT ALLEGIANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Faction Option Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Faction.entries.forEach { faction ->
                    val isSelected = uiState.selectedFaction == faction
                    val factionColor = when (faction) {
                        Faction.APEX -> ColorApexRed
                        Faction.CIPHER -> ColorCipherCyan
                        Faction.SOLARIS -> ColorSolarisGold
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) factionColor.copy(alpha = 0.12f) else ColorDarkCard)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) factionColor else ColorDarkSurfaceElevated,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onFactionSelected(faction) }
                            .padding(14.dp)
                            .testTag("faction_card_${faction.id.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(factionColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = faction.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ColorTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = faction.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorTextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = factionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            RunPrimaryButton(
                text = "Deploy Operative",
                onClick = viewModel::register,
                isLoading = authState is AuthState.Loading,
                modifier = Modifier.testTag("signup_submit_button")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigate back to Login
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have credentials?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("navigate_to_login_button")
                ) {
                    Text(
                        text = "Tactical Login",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorElectricLime,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
