package com.example.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorElectricLime


import com.example.domain.model.AttackValidationResult
import com.example.domain.model.BattleChallengeEvaluation
import com.example.domain.model.BattleSession
import com.example.domain.model.DevTerritory
import com.example.domain.model.Faction

@Composable
fun BattleEvaluationModal(
    evaluation: BattleChallengeEvaluation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkBackground,
        titleContentColor = if (evaluation.isPassed) ColorElectricLime else Color(0xFFFF2D55),
        textContentColor = Color.White,
        title = {
            Text(
                text = if (evaluation.isPassed) "SECTOR CAPTURED" else "ATTACK FAILED",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column {
                Text(
                    text = evaluation.summaryNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DISTANCE:", color = Color.White.copy(alpha = 0.5f))
                    Text("%.2f km".format(evaluation.distanceCompletedMeters / 1000.0), color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PACE:", color = Color.White.copy(alpha = 0.5f))
                    Text(
                        "%d:%02d /km".format(
                            evaluation.paceAchievedMinPerKm.toInt(),
                            ((evaluation.paceAchievedMinPerKm - evaluation.paceAchievedMinPerKm.toInt()) * 60).toInt()
                        ), 
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TIME:", color = Color.White.copy(alpha = 0.5f))
                    Text(
                        "%02d:%02d".format(evaluation.elapsedSeconds / 60, evaluation.elapsedSeconds % 60),
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (evaluation.isPassed) ColorElectricLime else Color(0xFFFF2D55)
                )
            ) {
                Text(
                    text = "ACKNOWLEDGE",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerritoryDetailsModal(
    territory: DevTerritory,
    eligibility: AttackValidationResult?,
    onDismiss: () -> Unit,
    onInitiateAttack: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val factionColor = Color(android.graphics.Color.parseColor(territory.colorHex))
            
            // Faction badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(factionColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .border(2.dp, factionColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Faction.fromId(territory.factionId).name.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = factionColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = territory.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "HELD BY RIVAL OPERATIVE",
                style = MaterialTheme.typography.bodyMedium,
                color = factionColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(label = "AREA", value = "%.1f ha".format(territory.areaSqMeters / 10000.0), color = Color.White)
                StatColumn(label = "SHIELD", value = "${territory.defenseLevel}%", color = ColorElectricLime)
                StatColumn(label = "FACTION", value = Faction.fromId(territory.factionId).name, color = factionColor)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (eligibility != null) {
                if (eligibility.isEligible) {
                    Button(
                        onClick = onInitiateAttack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)), // Attack red
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "INITIATE ATTACK",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = eligibility.rejectionReason?.label ?: "UNAVAILABLE",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = eligibility.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = ColorElectricLime, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackPreparationModal(
    battle: BattleSession,
    onDismiss: () -> Unit,
    onStartBattle: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ATTACK PROTOCOL INITIALIZED",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF2D55),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "TARGET: ${battle.territoryName}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Challenge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFF2D55).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = battle.challenge.type.icon,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = battle.challenge.type.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = battle.challenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ChallengeRequirementItem(
                            label = "DISTANCE",
                            value = battle.challenge.formattedTargetDistance
                        )
                        ChallengeRequirementItem(
                            label = "MIN PACE",
                            value = battle.challenge.formattedPaceRequirement
                        )
                        ChallengeRequirementItem(
                            label = "TIME LIMIT",
                            value = battle.challenge.formattedTimeLimit
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onStartBattle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorElectricLime),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "START DEPLOYMENT",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onDismiss) {
                Text("ABORT MISSION", color = Color.White.copy(alpha = 0.5f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ChallengeRequirementItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = ColorElectricLime,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
