package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class LocationPermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    RATIONALE_REQUIRED
}

enum class TrackingState {
    IDLE,
    STARTING,
    TRACKING,
    PAUSED,
    STOPPED
}

enum class RunState {
    IDLE,
    PREPARING,
    RUNNING,
    PAUSED,
    FINISHING,
    UPLOADING,
    VALIDATING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    OFFLINE_SAVED,
    FAILED
}

sealed class LocationError {
    data class PermissionDenied(val isPermanent: Boolean = false) : LocationError()
    object GpsDisabled : LocationError()
    data class PoorAccuracy(val accuracyMeters: Float) : LocationError()
    data class LocationUnavailable(val message: String = "Location signal lost") : LocationError()
}

@JsonClass(generateAdapter = true)
data class GpsPoint(
    val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val heading: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val toLatLng: LatLng get() = LatLng(latitude, longitude)
    val toUserLocation: UserLocation get() = UserLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        altitudeMeters = altitude,
        speedMps = speed,
        bearingDegrees = heading,
        timestamp = timestamp
    )
}

data class ActiveRunStats(
    val sessionId: String = "",
    val runState: RunState = RunState.IDLE,
    val trackingState: TrackingState = TrackingState.IDLE,
    val pointsCount: Int = 0,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentSpeedMps: Float = 0f,
    val avgSpeedMps: Double = 0.0,
    val lastKnownLocation: UserLocation? = null,
    val gpsStatus: GpsSignalStatus = GpsSignalStatus.SEARCHING,
    val error: LocationError? = null,
    val isOffline: Boolean = false
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val formattedDistance: String get() = "%.2f km".format(distanceKm)
    
    val formattedDuration: String get() {
        val hrs = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60
        return if (hrs > 0) {
            "%02d:%02d:%02d".format(hrs, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    val paceMinPerKm: Double get() {
        if (distanceKm <= 0.001 || durationSeconds <= 0) return 0.0
        return (durationSeconds / 60.0) / distanceKm
    }

    val formattedPace: String get() {
        if (distanceKm <= 0.001 || durationSeconds <= 0) return "--'--\" /km"
        val totalSecsPerKm = (durationSeconds.toDouble() / distanceKm).toInt()
        val mins = totalSecsPerKm / 60
        val secs = totalSecsPerKm % 60
        if (mins >= 60) return "--'--\" /km"
        return "%d'%02d\" /km".format(mins, secs)
    }

    val speedKmh: Double get() = currentSpeedMps * 3.6
    val formattedSpeed: String get() = "%.1f km/h".format(speedKmh)
}

data class RunSessionResult(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val avgSpeedMps: Double,
    val avgPaceMinPerKm: Double,
    val caloriesBurned: Int,
    val pointsCount: Int,
    val syncStatus: SyncStatus,
    val validationPassed: Boolean,
    val validationMessage: String = "All trajectory and integrity checks passed",
    val isOffline: Boolean = false,
    val capturedTerritoriesCount: Int = 0
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val formattedDistance: String get() = "%.2f km".format(distanceKm)
    val formattedDuration: String get() {
        val hrs = durationSeconds / 3600
        val mins = (durationSeconds % 3600) / 60
        val secs = durationSeconds % 60
        return if (hrs > 0) {
            "%02d:%02d:%02d".format(hrs, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }
    val formattedPace: String get() {
        if (distanceKm <= 0.001 || durationSeconds <= 0) return "--'--\" /km"
        val totalSecsPerKm = (durationSeconds.toDouble() / distanceKm).toInt()
        val mins = totalSecsPerKm / 60
        val secs = totalSecsPerKm % 60
        return "%d'%02d\" /km".format(mins, secs)
    }
}

