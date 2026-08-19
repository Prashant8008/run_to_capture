package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val ownerDisplayName: String,
    val faction: String,
    val geoJsonCoordinates: String,
    val areaSqMeters: Double,
    val h3HexIndexes: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val defenseLevel: Int = 100,
    val serverSignature: String = "",
    val isAuthoritative: Boolean = true,
    val isSynced: Boolean = false
)
