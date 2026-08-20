package com.example.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.RunColors
import com.example.domain.model.AuthState
import com.example.domain.model.Faction

@Composable
fun FactionSelectCard(
    title: String,
    description: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) dotColor.copy(alpha = 0.10f) else Color.White,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) dotColor else RunColors.GlassBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RunColors.Ink
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = RunColors.Body
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (isSelected) dotColor else Color(0xFFD7D5CE),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToDashboard()
        }
    }

    Scaffold(
        containerColor = RunColors.Background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(bottom = 90.dp)
                    .widthIn(max = 440.dp)
            ) {
                // Header
                Text(
                    text = "JOIN RUN2CAPTURE",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = RunColors.Ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = RunColors.CyanTint,
                    border = BorderStroke(1.dp, RunColors.Cyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RunColors.Cyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OPERATIVE ENLISTMENT PROTOCOL",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = RunColors.CyanDeep
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Error Banner
                val currentError = uiState.generalError ?: uiState.emailError ?: uiState.passwordError ?: uiState.displayNameError
                AnimatedVisibility(visible = currentError != null) {
                    currentError?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RunColors.Error.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, RunColors.Error.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = RunColors.Error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = RunColors.Error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 1. Operative Call-Sign
                Text(
                    text = "OPERATIVE CALL-SIGN",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = RunColors.Body,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.displayNameInput,
                    onValueChange = viewModel::onDisplayNameChanged,
                    placeholder = { Text("e.g. NIGHTHAWK_07", color = RunColors.Faint, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            tint = RunColors.Faint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = RunColors.CyanDeep,
                        unfocusedBorderColor = RunColors.GlassBorder,
                        focusedTextColor = RunColors.Ink,
                        unfocusedTextColor = RunColors.Ink
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .testTag("register_username_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Encrypted Email
                Text(
                    text = "ENCRYPTED EMAIL",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = RunColors.Body,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.emailInput,
                    onValueChange = viewModel::onEmailChanged,
                    placeholder = { Text("operative@sector.io", color = RunColors.Faint, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = RunColors.Faint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = RunColors.CyanDeep,
                        unfocusedBorderColor = RunColors.GlassBorder,
                        focusedTextColor = RunColors.Ink,
                        unfocusedTextColor = RunColors.Ink
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .testTag("register_email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Password
                Text(
                    text = "PASSWORD",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = RunColors.Body,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = viewModel::onPasswordChanged,
                    placeholder = { Text("••••••••", color = RunColors.Faint, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = RunColors.Faint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = RunColors.Faint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = RunColors.LimeDeep,
                        unfocusedBorderColor = RunColors.GlassBorder,
                        focusedTextColor = RunColors.Ink,
                        unfocusedTextColor = RunColors.Ink
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .testTag("register_password_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Confirm Password
                Text(
                    text = "CONFIRM PASSWORD",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = RunColors.Body,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.confirmPasswordInput,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    placeholder = { Text("••••••••", color = RunColors.Faint, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = RunColors.Faint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = RunColors.Faint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = RunColors.LimeDeep,
                        unfocusedBorderColor = RunColors.GlassBorder,
                        focusedTextColor = RunColors.Ink,
                        unfocusedTextColor = RunColors.Ink
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .testTag("register_confirm_password_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Faction Assignment Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = RunColors.Divider)
                    Text(
                        text = "FACTION ASSIGNMENT",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = RunColors.Faint
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = RunColors.Divider)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT FACTION",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = RunColors.Body,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 3 Faction Cards
                FactionSelectCard(
                    title = "Apex Vanguard",
                    description = "Fast, aggressive, first to the frontier. Rewards speed and early capture.",
                    dotColor = RunColors.Vanguard,
                    isSelected = uiState.selectedFaction == Faction.APEX,
                    onClick = { viewModel.onFactionSelected(Faction.APEX) },
                    modifier = Modifier.testTag("faction_card_apex")
                )

                Spacer(modifier = Modifier.height(10.dp))

                FactionSelectCard(
                    title = "Cipher Syndicate",
                    description = "Precision and strategy. Rewards efficient, well-planned routes.",
                    dotColor = RunColors.Cipher,
                    isSelected = uiState.selectedFaction == Faction.CIPHER,
                    onClick = { viewModel.onFactionSelected(Faction.CIPHER) },
                    modifier = Modifier.testTag("faction_card_cipher")
                )

                Spacer(modifier = Modifier.height(10.dp))

                FactionSelectCard(
                    title = "Solaris Collective",
                    description = "Endurance and consistency. Rewards daily streaks and long holds.",
                    dotColor = RunColors.Solaris,
                    isSelected = uiState.selectedFaction == Faction.SOLARIS,
                    onClick = { viewModel.onFactionSelected(Faction.SOLARIS) },
                    modifier = Modifier.testTag("faction_card_solaris")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Switch to Login Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already enlisted? ",
                        style = TextStyle(fontSize = 12.5.sp, color = RunColors.Body)
                    )
                    Text(
                        text = "Sign In",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = RunColors.LimeDeep
                        ),
                        modifier = Modifier
                            .clickable { onNavigateToLogin() }
                            .testTag("register_login_link")
                    )
                }
            }

            // Fixed Bottom Bar with Gradient Fade
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                RunColors.Background.copy(alpha = 0f),
                                RunColors.Background.copy(alpha = 0.96f),
                                RunColors.Background
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.register()
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
                        .testTag("register_submit_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "DEPLOY OPERATIVE",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
