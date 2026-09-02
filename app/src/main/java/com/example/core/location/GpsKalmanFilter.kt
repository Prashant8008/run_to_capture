package com.example.core.location

import com.example.domain.model.UserLocation
import kotlin.math.sqrt

/**
 * 2D Kalman Filter specifically tuned for human running GPS tracking.
 *
 * Smooths out piecewise GPS jitter, multipath noise, and micro-oscillations that
 * otherwise accumulate into false kilometers (e.g. 800m actual -> 3+ km recorded).
 */
class GpsKalmanFilter(
    private val processNoiseQ: Double = 3.0 // Model speed variance (m/s^2)
) {
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var variance: Double = -1.0 // P matrix diagonal representation (-1 indicates uninitialized)
    private var lastTimestampMs: Long = 0L

    val isInitialized: Boolean
        get() = variance >= 0.0

    /**
     * Resets the filter state (e.g., when starting a new run or resuming from pause).
     */
    fun reset() {
        variance = -1.0
        lastTimestampMs = 0L
        lat = 0.0
        lng = 0.0
    }

    /**
     * Estimates smoothed location without mutating committed internal state.
     */
    fun preview(rawLocation: UserLocation): UserLocation {
        val accuracy = rawLocation.accuracyMeters.toDouble().coerceAtLeast(3.0)
        val timestamp = rawLocation.timestamp

        if (variance < 0.0) {
            return rawLocation
        }

        val dtSec = ((timestamp - lastTimestampMs) / 1000.0).coerceAtLeast(0.0)
        var tempVariance = variance
        if (dtSec > 0.0) {
            tempVariance += dtSec * processNoiseQ * processNoiseQ
        }

        val measurementVariance = accuracy * accuracy
        val kalmanGain = tempVariance / (tempVariance + measurementVariance)

        val smoothedLat = lat + kalmanGain * (rawLocation.latitude - lat)
        val smoothedLng = lng + kalmanGain * (rawLocation.longitude - lng)
        val smoothedAccuracy = sqrt((1.0 - kalmanGain) * tempVariance).toFloat()

        return rawLocation.copy(
            latitude = smoothedLat,
            longitude = smoothedLng,
            accuracyMeters = smoothedAccuracy
        )
    }

    /**
     * Commits accepted measurement into the filter state.
     */
    fun commit(rawLocation: UserLocation): UserLocation {
        val accuracy = rawLocation.accuracyMeters.toDouble().coerceAtLeast(3.0)
        val timestamp = rawLocation.timestamp

        if (variance < 0.0) {
            lat = rawLocation.latitude
            lng = rawLocation.longitude
            variance = accuracy * accuracy
            lastTimestampMs = timestamp
            return rawLocation
        }

        val dtSec = ((timestamp - lastTimestampMs) / 1000.0).coerceAtLeast(0.0)
        lastTimestampMs = timestamp

        if (dtSec > 0.0) {
            variance += dtSec * processNoiseQ * processNoiseQ
        }

        val measurementVariance = accuracy * accuracy
        val kalmanGain = variance / (variance + measurementVariance)

        lat += kalmanGain * (rawLocation.latitude - lat)
        lng += kalmanGain * (rawLocation.longitude - lng)
        variance = (1.0 - kalmanGain) * variance

        val smoothedAccuracy = sqrt(variance).toFloat()

        return rawLocation.copy(
            latitude = lat,
            longitude = lng,
            accuracyMeters = smoothedAccuracy
        )
    }

    fun getEstimatedPosition(): Pair<Double, Double>? {
        return if (isInitialized) Pair(lat, lng) else null
    }
}
