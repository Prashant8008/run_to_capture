package com.example.data.repository

import com.example.domain.model.AppNotification
import com.example.domain.model.NotificationType
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NotificationRepositoryImpl : NotificationRepository {
    private val _notifications = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.SYSTEM_ALERT,
                title = "Welcome to Run2Capture",
                message = "The war for the grid has begun. Choose your faction and capture territory.",
                timestamp = System.currentTimeMillis() - 86400000,
                isRead = false,
                actionUrl = "run2capture://identity"
            )
        )
    )

    override val notifications: Flow<List<AppNotification>> = _notifications
    
    override val unreadCount: Flow<Int> = _notifications.map { list ->
        list.count { !it.isRead }
    }

    override suspend fun sendNotification(
        type: NotificationType,
        title: String,
        message: String,
        actionUrl: String?
    ) {
        val newNotification = AppNotification(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionUrl = actionUrl
        )
        _notifications.value = listOf(newNotification) + _notifications.value
    }

    override suspend fun markAsRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
    }

    override suspend fun markAllAsRead() {
        _notifications.value = _notifications.value.map {
            it.copy(isRead = true)
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        _notifications.value = _notifications.value.filter { it.id != notificationId }
    }
}
