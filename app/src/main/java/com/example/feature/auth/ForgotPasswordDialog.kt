package com.example.feature.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.RunColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    email: String,
    onEmailChanged: (String) -> Unit,
    isSent: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x1514171A),
                spotColor = Color(0x2514171A)
            )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = RunColors.Glass,
            border = BorderStroke(1.dp, RunColors.GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon tile with key
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFEDECE7), RoundedCornerShape(18.dp))
                        .border(1.dp, RunColors.GlassBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Security Key",
                        tint = RunColors.Ink,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECURE ACCESS RECOVERY label
                Text(
                    text = "SECURE ACCESS RECOVERY",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = RunColors.LimeDeep
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Headline: Reset Uplink
                Text(
                    text = "Reset Uplink",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.4).sp
                    ),
                    color = RunColors.Ink
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                Text(
                    text = if (isSent) {
                        "Reset link dispatched to $email. Follow uplink frequency instructions."
                    } else {
                        "Enter the email tied to your operative file. We'll transmit a reset code to re-establish your uplink."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    ),
                    color = RunColors.Body,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isSent) {
                    // Runner Email Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
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
                            value = email,
                            onValueChange = onEmailChanged,
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Button: SEND RESET CODE
                    Button(
                        onClick = onSubmit,
                        enabled = email.isNotBlank(),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RunColors.Lime,
                            contentColor = RunColors.LimeText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(999.dp))
                            .testTag("forgot_password_submit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SEND RESET CODE",
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
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Success",
                        tint = RunColors.Success,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ghost Button: BACK TO LOGIN
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("forgot_password_dismiss_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BACK TO LOGIN",
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
