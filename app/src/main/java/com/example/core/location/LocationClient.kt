package com.example.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
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

    private val systemLocationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

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
        val lm = systemLocationManager

        val gpsLoc = try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val netLoc = try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val passLoc = try { lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }

        val bestLoc = listOfNotNull(gpsLoc, netLoc, passLoc).maxByOrNull { it.time }
        return bestLoc?.toUserLocation()
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(intervalMs: Long): Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        // 1. Immediately emit best cached location if available
        getLastKnownLocation()?.let { cached ->
            Log.d("LocationClient", "Emitting initial cached location: ${cached.latitude}, ${cached.longitude}")
            trySend(cached)
        }

        // 2. Query Google Play Services fused last location and current location immediately
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    Log.d("LocationClient", "Emitting fused lastLocation: ${it.latitude}, ${it.longitude}")
                    trySend(it.toUserLocation())
                }
            }
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc: Location? ->
                loc?.let {
                    Log.d("LocationClient", "Emitting fused getCurrentLocation: ${it.latitude}, ${it.longitude}")
                    trySend(it.toUserLocation())
                }
            }
        } catch (e: Exception) {
            Log.w("LocationClient", "Failed to query initial fused location: ${e.message}")
        }

        // 3. Setup continuous high-accuracy fused location request
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(0f) // 0m so stationary devices/emulators still get location stream
            .setWaitForAccurateLocation(false)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { loc: Location ->
                    trySend(loc.toUserLocation())
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.w("LocationClient", "FusedClient request failed, falling back to system LocationManager: ${e.message}")
        }

        // 4. Also register native system LocationManager listeners as fallback/redundancy
        val systemListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toUserLocation())
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        val lm = systemLocationManager
        if (lm != null) {
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        intervalMs,
                        0f,
                        systemListener,
                        Looper.getMainLooper()
                    )
                }
            } catch (e: Exception) {
                Log.w("LocationClient", "GPS_PROVIDER listener registration failed: ${e.message}")
            }

            try {
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        intervalMs,
                        0f,
                        systemListener,
                        Looper.getMainLooper()
                    )
                }
            } catch (e: Exception) {
                Log.w("LocationClient", "NETWORK_PROVIDER listener registration failed: ${e.message}")
            }
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(fusedCallback)
            } catch (_: Exception) {}
            try {
                lm?.removeUpdates(systemListener)
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

