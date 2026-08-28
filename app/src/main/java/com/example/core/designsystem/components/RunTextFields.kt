package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.RunColors
import com.example.core.designsystem.RunTypography

@Composable
fun RunTacticalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = RunColors.Body,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )

        val visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x0A000000),
                    spotColor = Color(0x10000000)
                ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = RunColors.Faint,
                    fontSize = 14.sp
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = RunColors.Faint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
                {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = RunColors.Faint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            singleLine = singleLine,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF6F8F3),
                unfocusedContainerColor = Color(0xFFF2F4EE),
                disabledContainerColor = Color(0xFFE8EBE3),
                focusedBorderColor = RunColors.Cyan,
                unfocusedBorderColor = Color(0xFFDDE1D7),
                errorBorderColor = RunColors.Error,
                focusedTextColor = RunColors.Ink,
                unfocusedTextColor = RunColors.Ink,
                cursorColor = RunColors.Cyan
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            keyboardActions = keyboardActions
        )

        if (!errorMessage.isNullOrEmpty() || isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage ?: "Invalid field",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = RunColors.Error,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun RunTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    if (label != null) {
        RunTacticalInputField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder ?: "",
            leadingIcon = leadingIcon,
            isPassword = isPassword,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            isError = isError,
            errorMessage = errorMessage,
            keyboardType = keyboardType,
            singleLine = singleLine,
            keyboardActions = keyboardActions
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = RunColors.Faint, fontSize = 14.sp) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = RunColors.Faint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            singleLine = singleLine,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF6F8F3),
                unfocusedContainerColor = Color(0xFFF2F4EE),
                focusedBorderColor = RunColors.Cyan,
                unfocusedBorderColor = Color(0xFFDDE1D7),
                focusedTextColor = RunColors.Ink,
                unfocusedTextColor = RunColors.Ink,
                cursorColor = RunColors.Cyan
            )
        )
    }
}

