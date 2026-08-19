package com.example.feature.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.core.designsystem.components.RunBadge
import com.example.core.designsystem.components.RunBadgeVariant
import com.example.core.designsystem.components.RunPrimaryButton
import com.example.core.designsystem.components.RunTextField
import com.example.domain.model.AuthState

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToDashboard()
        }
    }

    if (uiState.showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = uiState.forgotPasswordEmail,
            onEmailChanged = viewModel::onForgotPasswordEmailChanged,
            isSent = uiState.forgotPasswordSent,
            onDismiss = { viewModel.setForgotPasswordDialogVisible(false) },
            onSubmit = { viewModel.submitForgotPassword() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorDarkBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo & Branding
                Spacer(modifier = Modifier.height(16.dp))
                RunBadge(
                    text = "RUN2CAPTURE // OPS",
                    variant = RunBadgeVariant.ACCENT
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tactical Login",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Enter the grid and conquer your sector.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

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

                // Email Field
                RunTextField(
                    value = uiState.emailInput,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Runner Email",
                    placeholder = "agent@sector.io",
                    leadingIcon = Icons.Default.Email,
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                RunTextField(
                    value = uiState.passwordInput,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Security Password",
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    isPasswordVisible = uiState.isPasswordVisible,
                    onPasswordVisibilityToggle = viewModel::togglePasswordVisibility,
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError,
                    keyboardType = KeyboardType.Password,
                    modifier = Modifier.testTag("login_password_input")
                )

                // Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.setForgotPasswordDialogVisible(true) },
                        modifier = Modifier.testTag("forgot_password_button")
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorElectricLime,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Login Button
                RunPrimaryButton(
                    text = "Initiate Uplink",
                    onClick = viewModel::login,
                    isLoading = authState is AuthState.Loading,
                    modifier = Modifier.testTag("login_submit_button")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ColorDarkSurfaceElevated
                    )
                    Text(
                        text = "OR CONTINUE WITH",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ColorDarkSurfaceElevated
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Google Authentication Button
                OutlinedButton(
                    onClick = {
                        viewModel.loginWithGoogle("dev_google_id_token_${System.currentTimeMillis()}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_login_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "G  Sign in with Google",
                        style = MaterialTheme.typography.labelLarge,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Navigate to Signup
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "New operative to the sector?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextSecondary
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.testTag("navigate_to_signup_button")
                    ) {
                        Text(
                            text = "Register Call-sign",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorElectricLime,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
