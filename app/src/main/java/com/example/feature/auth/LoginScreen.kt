package com.example.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AuthState

// Design Tokens matching the screenshot
private val ScreenBgColor = Color(0xFFE5ECE8) // Soft tactical light grey-green background
private val CardBgColor = Color(0xFFFAFBF9) // Crisp card background
private val CardBorderColor = Color(0xFFE1E7E3)
private val LimeAccent = Color(0xFF8DC600) // Tactical lime accent for labels and links
private val LimeButtonBg = Color(0xFFD0F838) // Vibrant chartreuse / lime button fill
private val PrimaryDarkText = Color(0xFF111827) // High-contrast black
private val SubtitleText = Color(0xFF263238)
private val PlaceholderGrey = Color(0xFF94A3B8)
private val InputBorderColor = Color(0xFFE2E8F0)
private val CyanBadgeBg = Color(0xFFE6FCF8)
private val CyanBadgeBorder = Color(0xFFA5F3FC)
private val CyanBadgeText = Color(0xFF00B4D8)
private val CyanBadgeDot = Color(0xFF00E5FF)

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
    val focusManager = LocalFocusManager.current

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
        containerColor = ScreenBgColor,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Elevated Rounded Card Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0x15000000),
                            spotColor = Color(0x1F000000)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    color = CardBgColor,
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Cyan Badge: ● RUN2CAPTURE // OPS
                        TacticalOpsBadge()

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large Headline: Tactical Login
                        Text(
                            text = "Tactical\nLogin",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 46.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = PrimaryDarkText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Subtitle
                        Text(
                            text = "Enter the grid and conquer your\nsector.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 22.sp
                            ),
                            color = SubtitleText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Error Banner (if any)
                        val currentError = uiState.generalError ?: (authState as? AuthState.Error)?.message
                        AnimatedVisibility(visible = !currentError.isNullOrEmpty()) {
                            if (!currentError.isNullOrEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEE2E2))
                                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentError,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFFB91C1C),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Input 1: Runner Email
                        TacticalInputField(
                            value = uiState.emailInput,
                            onValueChange = viewModel::onEmailChanged,
                            label = "Runner Email",
                            placeholder = "agent@sector.io",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = PrimaryDarkText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            isError = uiState.emailError != null,
                            errorMessage = uiState.emailError,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            testTag = "login_email_input"
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Input 2: Security Password
                        TacticalInputField(
                            value = uiState.passwordInput,
                            onValueChange = viewModel::onPasswordChanged,
                            label = null,
                            placeholder = "Security Password",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Password",
                                    tint = PrimaryDarkText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = viewModel::togglePasswordVisibility,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
                                        tint = PrimaryDarkText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            isPassword = true,
                            isPasswordVisible = uiState.isPasswordVisible,
                            isError = uiState.passwordError != null,
                            errorMessage = uiState.passwordError,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login()
                            },
                            testTag = "login_password_input"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Forgot password? link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot password?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LimeAccent,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        viewModel.setForgotPasswordDialogVisible(true)
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("forgot_password_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // INITIATE UPLINK Primary Action Button
                        val isLoading = authState is AuthState.Loading
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.login()
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeButtonBg,
                                contentColor = PrimaryDarkText,
                                disabledContainerColor = LimeButtonBg.copy(alpha = 0.6f),
                                disabledContentColor = PrimaryDarkText.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color(0x338DC600),
                                    spotColor = Color(0x668DC600)
                                )
                                .testTag("login_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = PrimaryDarkText,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "INITIATE UPLINK",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.6.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // OR CONTINUE WITH Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0),
                                thickness = 1.dp
                            )
                            Text(
                                text = "OR CONTINUE WITH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0),
                                thickness = 1.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Google Sign In Button
                        Surface(
                            onClick = {
                                viewModel.loginWithGoogle("dev_google_id_token_${System.currentTimeMillis()}")
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_login_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = PrimaryDarkText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Footer: New operative to the sector? Register Call-sign
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onNavigateToRegister()
                    }
                    .testTag("navigate_to_signup_button")
                ) {
                    Text(
                        text = "New operative to the sector?",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Register\nCall-sign",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        ),
                        color = LimeAccent,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Top Pill Badge: ● RUN2CAPTURE // OPS
 */
@Composable
private fun TacticalOpsBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CyanBadgeBg,
        border = BorderStroke(1.dp, CyanBadgeBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(CyanBadgeDot)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RUN2CAPTURE // OPS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = CyanBadgeText
                )
            )
        }
    }
}

/**
 * Custom Outlined Tactical Input Field with integrated label
 */
@Composable
private fun TacticalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null,
    testTag: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Main Input Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isError) Color(0xFFEF4444) else InputBorderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = PlaceholderGrey,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = PrimaryDarkText,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(PrimaryDarkText),
                            visualTransformation = if (isPassword && !isPasswordVisible) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = imeAction
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onDone?.invoke() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(testTag)
                        )
                    }
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
            }

            // Floating Label Tag on top-left border (e.g. "Runner Email")
            if (label != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .align(Alignment.TopStart)
                ) {
                    // Small floating badge
                    Row(
                        modifier = Modifier
                            .background(CardBgColor)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = LimeAccent
                            )
                        )
                    }
                }
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Pixel-accurate Google Multi-color G Icon drawn natively
 */
@Composable
private fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val radius = width.coerceAtMost(height) / 2f

        // Google 4-color palette
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)
        val blue = Color(0xFF4285F4)

        // Blue horizontal crossbar and right sector
        val stroke = radius * 0.42f
        val arcRadius = radius - stroke / 2f

        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 100f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            topLeft = androidx.compose.ui.geometry.Offset(cx - arcRadius, cy - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
        )
        drawArc(
            color = yellow,
            startAngle = 120f,
            sweepAngle = 70f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            topLeft = androidx.compose.ui.geometry.Offset(cx - arcRadius, cy - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
        )
        drawArc(
            color = green,
            startAngle = 20f,
            sweepAngle = 110f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            topLeft = androidx.compose.ui.geometry.Offset(cx - arcRadius, cy - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
        )
        drawArc(
            color = blue,
            startAngle = 310f,
            sweepAngle = 80f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            topLeft = androidx.compose.ui.geometry.Offset(cx - arcRadius, cy - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
        )

        // Middle blue bar
        drawRect(
            color = blue,
            topLeft = androidx.compose.ui.geometry.Offset(cx - stroke * 0.1f, cy - stroke / 2f),
            size = androidx.compose.ui.geometry.Size(radius, stroke)
        )
    }
}
