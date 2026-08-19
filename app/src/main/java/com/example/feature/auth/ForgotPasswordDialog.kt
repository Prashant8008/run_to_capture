package com.example.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.core.designsystem.components.RunPrimaryButton
import com.example.core.designsystem.components.RunSecondaryButton
import com.example.core.designsystem.components.RunTextField

@Composable
fun ForgotPasswordDialog(
    email: String,
    onEmailChanged: (String) -> Unit,
    isSent: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSent) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ColorElectricLime,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Reset instructions sent to $email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please check your inbox to create a new password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Enter your verified runner email and we'll send you tactical recovery instructions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RunTextField(
                        value = email,
                        onValueChange = onEmailChanged,
                        label = "Runner Email",
                        placeholder = "name@domain.com",
                        leadingIcon = Icons.Default.Email,
                        modifier = Modifier.testTag("forgot_password_email_input")
                    )
                }
            }
        },
        confirmButton = {
            if (isSent) {
                RunPrimaryButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier.testTag("forgot_password_done_button")
                )
            } else {
                RunPrimaryButton(
                    text = "Send Recovery Email",
                    onClick = onSubmit,
                    modifier = Modifier.testTag("forgot_password_submit_button")
                )
            }
        },
        dismissButton = {
            if (!isSent) {
                RunSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.testTag("forgot_password_cancel_button")
                )
            }
        }
    )
}
