package com.example.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.UserLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface LocationClient {
    fun getLocationUpdates(intervalMs: Long = 2000L): Flow<UserLocation>
    fun getLastKnownLocation(): UserLocation?
    fun hasLocationPermission(): Boolean
    fun isGpsEnabled(): Boolean
    fun getGpsStatus(accuracyMeters: Float): GpsSignalStatus
}

class DefaultLocationClient(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : LocationClient {

    // Retained strictly for querying whether GPS/location hardware is enabled on device
    private val systemLocationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    @Volatile
    private var lastKnownLocationCache: UserLocation? = null

    override fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun isGpsEnabled(): Boolean {
        val lm = systemLocationManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: Exception) {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    override fun getGpsStatus(accuracyMeters: Float): GpsSignalStatus {
        if (!isGpsEnabled()) return GpsSignalStatus.DISABLED
        return when {
            accuracyMeters <= 0f -> GpsSignalStatus.SEARCHING
            accuracyMeters <= 15f -> GpsSignalStatus.GOOD
            accuracyMeters <= 35f -> GpsSignalStatus.POOR
            else -> GpsSignalStatus.POOR
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): UserLocation? {
        if (!hasLocationPermission()) return null
        return lastKnownLocationCache
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(intervalMs: Long): Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        // Setup single-source continuous high-accuracy fused location tracking (Fresh GPS fixes only)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { loc: Location ->
                    val userLoc = loc.toUserLocation()
                    lastKnownLocationCache = userLoc
                    trySend(userLoc)
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("LocationClient", "FusedClient requestLocationUpdates failed: ${e.message}")
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(fusedCallback)
            } catch (_: Exception) {}
        }
    }

    private fun Location.toUserLocation(): UserLocation {
        return UserLocation(
            latitude = this.latitude,
            longitude = this.longitude,
            accuracyMeters = this.accuracy,
            altitudeMeters = this.altitude,
            speedMps = this.speed,
            bearingDegrees = this.bearing,
            timestamp = this.time
        )
    }
}

