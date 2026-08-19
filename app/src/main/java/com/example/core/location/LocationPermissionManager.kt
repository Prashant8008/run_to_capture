package com.example.core.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.LocationPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationPermissionManager(
    private val context: Context
) {
    companion object {
        const val RATIONALE_TITLE = "OPERATIVE LOCATION TELEMETRY REQUIRED"
        const val RATIONALE_MESSAGE =
            "RUN2CAPTURE relies on continuous high-precision GPS to map your run path, establish claim corridors, detect territory closure, and enforce fair anti-spoofing velocity limits. Without precise location access, sector capture is offline."
        const val NOTIFICATION_RATIONALE_MESSAGE =
            "Notification permission allows RUN2CAPTURE to keep your run telemetry active in the background while your device is locked or in your pocket."

        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val ALL_RUN_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            LOCATION_PERMISSIONS
        }
    }

    private val _permissionState = MutableStateFlow(checkPermissionState())
    val permissionState: StateFlow<LocationPermissionState> = _permissionState.asStateFlow()

    private val _isGpsEnabled = MutableStateFlow(isGpsHardwareEnabled())
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    fun refreshStates(activity: Activity? = null) {
        _permissionState.value = checkPermissionState(activity)
        _isGpsEnabled.value = isGpsHardwareEnabled()
    }

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() || hasCoarseLocationPermission()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isGpsHardwareEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun checkPermissionState(activity: Activity? = null): LocationPermissionState {
        if (hasFineLocationPermission()) {
            return LocationPermissionState.GRANTED
        }

        if (activity != null) {
            val shouldShowFine = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val shouldShowCoarse = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (shouldShowFine || shouldShowCoarse) {
                return LocationPermissionState.RATIONALE_REQUIRED
            }
        }

        // If not granted, could be initial request (DENIED) or permanently denied if already rejected
        return if (hasCoarseLocationPermission()) {
            LocationPermissionState.RATIONALE_REQUIRED
        } else {
            LocationPermissionState.DENIED
        }
    }

    fun evaluateGpsAccuracy(accuracyMeters: Float): GpsSignalStatus {
        if (!isGpsHardwareEnabled()) {
            return GpsSignalStatus.DISABLED
        }
        return when {
            accuracyMeters <= 0f -> GpsSignalStatus.SEARCHING
            accuracyMeters <= 15f -> GpsSignalStatus.GOOD
            accuracyMeters <= 35f -> GpsSignalStatus.POOR
            else -> GpsSignalStatus.POOR
        }
    }

    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openAppSettings() {
        try {
            context.startActivity(createAppSettingsIntent())
        } catch (_: Exception) {}
    }

    fun createLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openLocationSettings() {
        try {
            context.startActivity(createLocationSettingsIntent())
        } catch (_: Exception) {}
    }

    fun createNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            createAppSettingsIntent()
        }
    }
}
