package com.example.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.core.di.AppModule
import com.example.domain.model.ActiveRunStats
import com.example.domain.model.TrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "run2capture_active_run_channel"
        const val CHANNEL_NAME = "Active Run Tracking"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.example.location.ACTION_START"
        const val ACTION_PAUSE = "com.example.location.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.location.ACTION_RESUME"
        const val ACTION_STOP = "com.example.location.ACTION_STOP"
        const val EXTRA_SESSION_ID = "extra_session_id"

        fun startService(context: Context, sessionId: String) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var statsCollectorJob: Job? = null
    private var periodicSyncJob: Job? = null

    private lateinit var notificationManager: NotificationManager

    inner class LocalBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)

        when (action) {
            ACTION_START -> {
                val initialNotification = buildNotification("Run active", "Initializing tactical GPS telemetry...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
                observeLocationManagerStats()
                startPeriodicBatchSync()
            }

            ACTION_PAUSE -> {
                updateNotification("Run paused", "Tactical telemetry paused")
            }

            ACTION_RESUME -> {
                updateNotification("Run active", "Tactical GPS telemetry resumed")
            }

            ACTION_STOP -> {
                stopForegroundTracking()
            }
        }

        return START_STICKY
    }

    private fun observeLocationManagerStats() {
        statsCollectorJob?.cancel()
        statsCollectorJob = serviceScope.launch {
            LocationManagerHolder.instance?.activeRunStats?.collectLatest { stats ->
                val contentText = buildNotificationText(stats)
                val title = if (stats.trackingState == TrackingState.PAUSED) "Run paused" else "Run active"
                updateNotification(title, contentText)
            }
        }
    }

    private fun buildNotificationText(stats: ActiveRunStats): String {
        val km = stats.distanceMeters / 1000.0
        val minutes = stats.durationSeconds / 60
        val seconds = stats.durationSeconds % 60
        val formattedTime = "%02d:%02d".format(minutes, seconds)
        return "%.2f km • %s • %d pts".format(km, formattedTime, stats.pointsCount)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time location telemetry during active runs"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(title: String, content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Run2Capture")
            .setContentText(content)
            .setSubText(title)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startPeriodicBatchSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = serviceScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(90_000L) // Batch push every 90 seconds (1.5 minutes)
                try {
                    com.example.core.sync.SyncManager.scheduleSync(applicationContext)
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopForegroundTracking() {
        statsCollectorJob?.cancel()
        periodicSyncJob?.cancel()
        // Run a final sync flush when the run completes
        try {
            com.example.core.sync.SyncManager.scheduleSync(applicationContext)
        } catch (_: Exception) {}
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

/**
 * Singleton holder allowing LocationForegroundService to safely stream stats from the active LocationManager
 */
object LocationManagerHolder {
    @Volatile
    var instance: LocationManagerCoordinator? = null
}

interface LocationManagerCoordinator {
    val activeRunStats: kotlinx.coroutines.flow.StateFlow<ActiveRunStats>
}
