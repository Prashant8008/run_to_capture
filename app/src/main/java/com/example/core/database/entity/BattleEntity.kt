package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battles")
data class BattleEntity(
    @PrimaryKey val battleId: String,
    val territoryId: String,
    val territoryName: String,
    val defenderUserId: String,
    val defenderDisplayName: String,
    val defenderFaction: String,
    val defenderColorHex: String,
    val defenderFlagSvg: String? = null,
    val attackerUserId: String,
    val attackerDisplayName: String,
    val attackerFaction: String,
    val challengeType: String,
    val targetDistanceMeters: Double,
    val minPaceMinPerKm: Double,
    val timeLimitSeconds: Long,
    val challengeDescription: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long,
    val completedAt: Long? = null,
    val serverSignature: String = "",
    val territoryAreaSqMeters: Double = 25000.0
)
