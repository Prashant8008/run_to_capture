package com.example.domain.model

import java.time.Instant

enum class NotificationType {
    TERRITORY_ATTACKED,
    TERRITORY_CAPTURED,
    TERRITORY_LOST,
    DEFENSE_SUCCESSFUL,
    RECORD_BEATEN,
    ACHIEVEMENT_UNLOCKED,
    RANK_CHANGED,
    CHALLENGE_COMPLETED,
    SYSTEM_ALERT
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionUrl: String? = null // Deep link URL
)
