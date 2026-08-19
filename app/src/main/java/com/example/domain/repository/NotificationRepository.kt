package com.example.domain.repository

import com.example.domain.model.AppNotification
import com.example.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notifications: Flow<List<AppNotification>>
    val unreadCount: Flow<Int>

    suspend fun sendNotification(
        type: NotificationType,
        title: String,
        message: String,
        actionUrl: String? = null
    )

    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(notificationId: String)
}
