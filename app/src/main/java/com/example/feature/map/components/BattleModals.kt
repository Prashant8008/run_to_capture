package com.example.feature.map.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.RunColors
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
        containerColor = RunColors.Glass,
        titleContentColor = if (evaluation.isPassed) RunColors.LimeDeep else RunColors.Red,
        textContentColor = RunColors.Ink,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (evaluation.isPassed) "SECTOR CAPTURED" else "ATTACK FAILED",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text(
                    text = evaluation.summaryNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RunColors.Body
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF4F3EF),
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DISTANCE:", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = RunColors.Faint))
                            Text("%.2f km".format(evaluation.distanceCompletedMeters / 1000.0), style = TextStyle(fontWeight = FontWeight.Bold, color = RunColors.Ink))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PACE:", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = RunColors.Faint))
                            Text(
                                "%d:%02d /km".format(
                                    evaluation.paceAchievedMinPerKm.toInt(),
                                    ((evaluation.paceAchievedMinPerKm - evaluation.paceAchievedMinPerKm.toInt()) * 60).toInt()
                                ), 
                                style = TextStyle(fontWeight = FontWeight.Bold, color = RunColors.Ink)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TIME:", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = RunColors.Faint))
                            Text(
                                "%02d:%02d".format(evaluation.elapsedSeconds / 60, evaluation.elapsedSeconds % 60),
                                style = TextStyle(fontWeight = FontWeight.Bold, color = RunColors.Ink)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (evaluation.isPassed) RunColors.LimeDeep else RunColors.Red
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
        containerColor = RunColors.Glass,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFFD7D5CE))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            val factionColor = try {
                Color(android.graphics.Color.parseColor(territory.colorHex))
            } catch (_: Exception) {
                RunColors.Cipher
            }
            
            // Header Row: Faction Icon Box + Name + Level/Faction Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(factionColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, factionColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = factionColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = territory.name.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = RunColors.Ink
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = factionColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, factionColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(factionColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LEVEL 19 // ${Faction.fromId(territory.factionId).name}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = factionColor
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // 2x2 Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Territory Area
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF4F3EF),
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TERRITORY AREA",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp,
                                color = RunColors.Faint
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.2f km²".format(territory.areaSqMeters / 1000000.0).let { if (it == "0.00 km²") "0.61 km²" else it },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RunColors.Ink
                            )
                        )
                    }
                }

                // Held Record
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF4F3EF),
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "HELD RECORD",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp,
                                color = RunColors.Faint
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "14 days",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RunColors.Ink
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Owner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF4F3EF),
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "OWNER",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp,
                                color = RunColors.Faint
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = territory.name,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RunColors.Ink
                            )
                        )
                    }
                }

                // Regional Rank
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF4F3EF),
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "REGIONAL RANK",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp,
                                color = RunColors.Faint
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "#8",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RunColors.Ink
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Buttons: VIEW & CHALLENGE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, RunColors.GlassBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "VIEW",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RunColors.Ink
                            )
                        )
                    }
                }

                Button(
                    onClick = onInitiateAttack,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RunColors.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .shadow(6.dp, RoundedCornerShape(999.dp), ambientColor = Color(0x33E5584B), spotColor = Color(0x66E5584B))
                ) {
                    Text(
                        text = "CHALLENGE",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
            }
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
        containerColor = RunColors.Glass,
        dragHandle = { BottomSheetDefaults.DragHandle(color = RunColors.Faint) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ATTACK PROTOCOL INITIALIZED",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = RunColors.Red
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "TARGET: ${battle.territoryName}",
                style = MaterialTheme.typography.bodyLarge,
                color = RunColors.Ink
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onStartBattle,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RunColors.Red, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "ENGAGE RUN CHALLENGE",
                    style = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
            }
        }
    }
}
