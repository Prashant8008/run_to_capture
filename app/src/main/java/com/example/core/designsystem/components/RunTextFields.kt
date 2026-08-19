package com.example.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CustomShapes
import com.example.core.designsystem.RunColors
import com.example.core.designsystem.RunTypography

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
    val visualTransformation = if (isPassword && !isPasswordVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    val trailingIconComposable: @Composable (() -> Unit)? = if (isPassword && onPasswordVisibilityToggle != null) {
        {
            IconButton(onClick = onPasswordVisibilityToggle) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                    tint = RunColors.OnSurfaceMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else null

    val leadingIconComposable: @Composable (() -> Unit)? = leadingIcon?.let {
        {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (isError) RunColors.Error else RunColors.OnSurfaceMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it, style = RunTypography.bodyMedium) } },
        placeholder = placeholder?.let { { Text(it, style = RunTypography.bodyMedium, color = RunColors.OnSurfaceSubtle) } },
        leadingIcon = leadingIconComposable,
        trailingIcon = trailingIconComposable,
        isError = isError || !errorMessage.isNullOrEmpty(),
        supportingText = errorMessage?.let { { Text(it, style = RunTypography.labelSmall, color = RunColors.Error) } },
        singleLine = singleLine,
        shape = CustomShapes.Card,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = RunColors.SurfaceVariant,
            unfocusedContainerColor = RunColors.Surface,
            disabledContainerColor = RunColors.Background,
            focusedBorderColor = RunColors.ElectricLime,
            unfocusedBorderColor = RunColors.CardBorder,
            errorBorderColor = RunColors.Error,
            focusedTextColor = RunColors.OnBackground,
            unfocusedTextColor = RunColors.OnBackground,
            focusedLabelColor = RunColors.ElectricLime,
            unfocusedLabelColor = RunColors.OnSurfaceMuted,
            cursorColor = RunColors.ElectricLime
        ),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        keyboardActions = keyboardActions
    )
}
