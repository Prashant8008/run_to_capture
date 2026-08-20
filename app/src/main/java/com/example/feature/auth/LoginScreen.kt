package com.example.feature.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Hexagon
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.core.designsystem.RunColors
import com.example.domain.model.AuthState
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    fun launchGoogleSignIn() {
        coroutineScope.launch {
            try {
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = "1094002492576-run2capture.apps.googleusercontent.com"
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.loginWithGoogle(
                        idToken = googleIdTokenCredential.idToken,
                        displayName = googleIdTokenCredential.displayName
                    )
                }
            } catch (_: GetCredentialException) {
                // User cancelled or no credentials available
            } catch (_: Exception) {
            }
        }
    }

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
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Brand Mark: Hexagon with Lime accent
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFEDECE7), RoundedCornerShape(16.dp))
                        .border(1.dp, RunColors.GlassBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hexagon,
                        contentDescription = "Run2Capture Emblem",
                        tint = RunColors.LimeDeep,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status Badge: OPERATIVE AUTHENTICATION
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RunColors.LimeTint,
                    border = BorderStroke(1.dp, RunColors.Lime.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RunColors.LimeDeep)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OPERATIVE AUTHENTICATION",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = RunColors.LimeDeep
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Headline: Access Sector Uplink
                Text(
                    text = "Access Sector Uplink",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = RunColors.Ink
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Sign in to deploy to contested GPS sectors.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = RunColors.Body
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message
                val currentError = uiState.generalError ?: uiState.emailError ?: uiState.passwordError
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

                // 1. Email Input
                Text(
                    text = "RUNNER EMAIL",
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
                        focusedBorderColor = RunColors.LimeDeep,
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
                        .testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Password Input
                Text(
                    text = "PASSCODE",
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
                                contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
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
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .testTag("login_password_input")
                )

                // Forgot Password Link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Forgot password?",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = RunColors.LimeDeep
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setForgotPasswordDialogVisible(true) }
                            .testTag("forgot_password_button")
                    )
                }

                // 3. Primary Button: SIGN IN WITH SECTOR ID
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RunColors.Lime,
                        contentColor = RunColors.LimeText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = Color(0x33CFF23A),
                            spotColor = Color(0x66CFF23A)
                        )
                        .testTag("login_submit_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "SIGN IN WITH SECTOR ID",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // OR Tactical Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = RunColors.Divider)
                    Text(
                        text = "OR",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RunColors.Faint
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = RunColors.Divider)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Secondary Ghost Button: SIGN IN WITH GOOGLE
                Surface(
                    onClick = { launchGoogleSignIn() },
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(2.dp, RoundedCornerShape(999.dp))
                        .testTag("login_google_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4285F4)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SIGN IN WITH GOOGLE",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                color = RunColors.Ink
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Switch to Register Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New operative? ",
                        style = TextStyle(fontSize = 13.sp, color = RunColors.Body)
                    )
                    Text(
                        text = "Enlist Now",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RunColors.LimeDeep
                        ),
                        modifier = Modifier
                            .clickable { onNavigateToRegister() }
                            .testTag("login_register_link")
                    )
                }
            }
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
}
