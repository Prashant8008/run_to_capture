package com.example.data.realtime

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

sealed class RealtimeEvent {
    data class TerritoryUpdate(val cellId: String, val newFactionId: String) : RealtimeEvent()
    data class BattleStatusUpdate(val battleId: String, val status: String) : RealtimeEvent()
    data class PushNotificationReceived(
        val type: String,
        val title: String,
        val message: String,
        val actionUrl: String?
    ) : RealtimeEvent()
}

/**
 * Mock WebSocket / Realtime client architecture.
 * Supports connection states, reconnects, offline behavior, and event streaming.
 * Includes a fallback polling mode simulation.
 */
class RealtimeClient {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: Flow<RealtimeEvent> = _events.asSharedFlow()

    private var isPollingFallback = false

    suspend fun connect() {
        _connectionState.update { ConnectionState.CONNECTING }
        delay(800) // Simulate network delay
        _connectionState.update { ConnectionState.CONNECTED }
        isPollingFallback = false
    }

    suspend fun disconnect() {
        _connectionState.update { ConnectionState.DISCONNECTED }
    }

    suspend fun simulateDisconnect() {
        _connectionState.update { ConnectionState.DISCONNECTED }
        delay(1000)
        _connectionState.update { ConnectionState.RECONNECTING }
        delay(1500)
        // If reconnect fails, fallback to polling
        isPollingFallback = true
        _connectionState.update { ConnectionState.CONNECTED }
    }

    fun publish(event: RealtimeEvent) {
        _events.tryEmit(event)
    }
}
