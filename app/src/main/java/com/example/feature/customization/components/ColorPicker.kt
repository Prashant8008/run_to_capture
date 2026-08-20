package com.example.feature.customization.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.domain.model.MapContrastValidator
import com.example.domain.model.StandardTerritoryColor

/**
 * Tactical Territory Color Picker with standard swatch grid,
 * custom hex input, and real-time Map Contrast & Visibility Validator.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customHexInput by remember(selectedColor) {
        val standard = StandardTerritoryColor.fromId(selectedColor)
        mutableStateOf(if (standard == null) selectedColor.removePrefix("#") else "")
    }

    val activeColorObj = StandardTerritoryColor.parseColor(selectedColor)
    val mapValidation = remember(selectedColor, customHexInput) {
        val hexToTest = if (customHexInput.isNotBlank()) "#$customHexInput" else StandardTerritoryColor.getHexForColor(selectedColor)
        MapContrastValidator.validate(hexToTest)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFBF9).copy(alpha = 0.95f))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("territory_color_picker")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color(0xFF659900),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "TERRITORY CONQUEST COLOR",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Bold
                )
            }

            // Active Color Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F5F2))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(activeColorObj)
                )
                Text(
                    text = StandardTerritoryColor.getHexForColor(selectedColor),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Standard 9 Tactical Colors Palette
        Text(
            text = "STANDARD SECTOR PALETTE",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StandardTerritoryColor.entries.forEach { standardColor ->
                val isSelected = selectedColor.equals(standardColor.id, ignoreCase = true)
                
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(standardColor.color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF111827) else Color(0x33000000),
                            shape = CircleShape
                        )
                        .clickable {
                            customHexInput = ""
                            onColorSelected(standardColor.id)
                        }
                        .testTag("color_swatch_${standardColor.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected ${standardColor.displayName}",
                            tint = if (standardColor == StandardTerritoryColor.GOLD || standardColor == StandardTerritoryColor.CYAN || standardColor == StandardTerritoryColor.GREEN) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Hex Input with Live Map Validation
        Text(
            text = "CUSTOM COLOR (HEX)",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = customHexInput,
                onValueChange = { input ->
                    val sanitized = input.filter { it.isLetterOrDigit() }.take(6)
                    customHexInput = sanitized
                    if (sanitized.length == 6) {
                        val testHex = "#$sanitized"
                        val result = MapContrastValidator.validate(testHex)
                        if (result.isValid) {
                            onColorSelected(testHex.uppercase())
                        }
                    }
                },
                placeholder = { Text("00FF88", color = Color(0xFF94A3B8)) },
                prefix = { Text("#", color = Color(0xFF659900), fontWeight = FontWeight.Bold) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_hex_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (mapValidation.isValid) Color(0xFF8DC600) else Color(0xFFFF3B30),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Custom Color Sample Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (mapValidation.isValid && customHexInput.length == 6) Color(("FF$customHexInput").toLong(16)) else activeColorObj)
                    .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (customHexInput.length == 6 && mapValidation.isValid) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Valid Color",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Map Contrast & Visibility Validation Status Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (mapValidation.isValid) Color(0xFFEFFDF5) else Color(0xFFFEF2F2))
                .border(
                    width = 1.dp,
                    color = if (mapValidation.isValid) Color(0xFF86EFAC) else Color(0xFFFECACA),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (mapValidation.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (mapValidation.isValid) Color(0xFF16A34A) else Color(0xFFDC2626),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (mapValidation.isValid) "MAP VISIBILITY: OPTIMAL" else "MAP VISIBILITY: LOW CONTRAST",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mapValidation.isValid) Color(0xFF15803D) else Color(0xFFB91C1C),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mapValidation.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF334155),
                    fontSize = 11.sp
                )
            }
        }
    }
}
