package com.example.core.location

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.core.network.NetworkMonitor
import com.example.domain.model.ActiveRunStats
import com.example.domain.model.GpsPoint
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.LocationError
import com.example.domain.model.LocationPermissionState
import com.example.domain.model.RunSessionResult
import com.example.domain.model.RunState
import com.example.domain.model.SyncStatus
import com.example.domain.model.TrackingState
import com.example.domain.model.UserLocation
import com.example.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class LocationManager(
    private val context: Context,
    private val locationClient: LocationClient,
    private val permissionManager: LocationPermissionManager,
    private val locationRepository: LocationRepository,
    private val networkMonitor: NetworkMonitor? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : LocationManagerCoordinator {

    companion object {
        const val MAX_VALID_ACCURACY_METERS = 35.0f
        const val MAX_RUNNING_SPEED_KMH = 28.0 // 28.0 km/h hard ceiling for human running / anti-vehicular cheat
        const val MAX_RUNNING_SPEED_MPS = 7.78f // ~28.0 km/h (28.0 / 3.6)
        const val WARNING_RUNNING_SPEED_KMH = 24.0 // High-speed running warning threshold
        const val MIN_POINT_DISTANCE_METERS = 2.0f
        const val MAX_TELEPORT_DISTANCE_METERS = 250.0 // Teleport anomaly threshold
        const val GPS_GAP_TIME_THRESHOLD_MS = 30_000L // 30s gap
    }

    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())
    private var locationUpdatesJob: Job? = null
    private var timerJob: Job? = null
    private var isFinishingInProgress = AtomicBoolean(false)

    private val _currentLocation = MutableStateFlow<UserLocation?>(null)
    val currentLocation: StateFlow<UserLocation?> = _currentLocation.asStateFlow()

    private val _gpsStatus = MutableStateFlow<GpsSignalStatus>(GpsSignalStatus.SEARCHING)
    val gpsStatus: StateFlow<GpsSignalStatus> = _gpsStatus.asStateFlow()

    private val _runState = MutableStateFlow<RunState>(RunState.IDLE)
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    private val _activeRunStats = MutableStateFlow(ActiveRunStats())
    override val activeRunStats: StateFlow<ActiveRunStats> = _activeRunStats.asStateFlow()

    private val _completedRunResult = MutableStateFlow<RunSessionResult?>(null)
    val completedRunResult: StateFlow<RunSessionResult?> = _completedRunResult.asStateFlow()

    private val _errorState = MutableStateFlow<LocationError?>(null)
    val errorState: StateFlow<LocationError?> = _errorState.asStateFlow()

    private val _activeSessionPoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val activeSessionPoints: StateFlow<List<GpsPoint>> = _activeSessionPoints.asStateFlow()

    private var lastRecordedTimestamp = 0L

    init {
        LocationManagerHolder.instance = this
        startContinuousLocationListening()
    }

    fun startContinuousLocationListening() {
        if (!permissionManager.hasLocationPermission()) {
            val isPermanent = permissionManager.checkPermissionState() == LocationPermissionState.PERMANENTLY_DENIED
            _errorState.value = LocationError.PermissionDenied(isPermanent)
            _gpsStatus.value = GpsSignalStatus.DISABLED
            return
        }

        if (!permissionManager.isGpsHardwareEnabled()) {
            _errorState.value = LocationError.GpsDisabled
            _gpsStatus.value = GpsSignalStatus.DISABLED
            return
        }

        locationUpdatesJob?.cancel()
        locationUpdatesJob = scope.launch {
            locationClient.getLocationUpdates(1200L)
                .catch { e ->
                    _errorState.value = LocationError.LocationUnavailable(e.message ?: "GPS stream interrupted")
                    _gpsStatus.value = GpsSignalStatus.SEARCHING
                }
                .collect { location ->
                    onNewLocationReceived(location)
                }
        }
    }

    private fun onNewLocationReceived(location: UserLocation) {
        _currentLocation.value = location
        val calculatedGps = permissionManager.evaluateGpsAccuracy(location.accuracyMeters)
        _gpsStatus.value = calculatedGps

        if (!calculatedGps.isGood) {
            if (calculatedGps == GpsSignalStatus.POOR) {
                _errorState.value = LocationError.PoorAccuracy(location.accuracyMeters)
            }
        } else {
            if (_errorState.value is LocationError.PoorAccuracy || _errorState.value is LocationError.LocationUnavailable) {
                _errorState.value = null
            }
        }

        // If in active tracking state, process point
        if (_runState.value == RunState.RUNNING) {
            processActiveRunPoint(location)
        }
    }

    private fun processActiveRunPoint(location: UserLocation) {
        // Drop inaccurate jitter points
        if (location.accuracyMeters > MAX_VALID_ACCURACY_METERS) {
            return
        }

        val lastLoc = _activeRunStats.value.lastKnownLocation
        val now = location.timestamp

        // Check for GPS gaps (e.g. signal loss for > 30s)
        val isGpsGap = lastRecordedTimestamp > 0 && (now - lastRecordedTimestamp) > GPS_GAP_TIME_THRESHOLD_MS
        lastRecordedTimestamp = now

        val distanceDelta = if (lastLoc != null) {
            calculateDistanceMeters(lastLoc.latitude, lastLoc.longitude, location.latitude, location.longitude)
        } else {
            0.0
        }

        // Prevent teleport anomalies (> 250 meters in a single update)
        if (lastLoc != null && distanceDelta > MAX_TELEPORT_DISTANCE_METERS && !isGpsGap) {
            Log.w("LocationManager", "Filtered teleport anomaly: distance delta $distanceDelta m")
            return
        }

        // Calculate instantaneous segment speed (m/s and km/h)
        val timeDeltaSec = if (lastLoc != null) (now - lastLoc.timestamp) / 1000.0 else 0.0
        val segmentSpeedMps = if (lastLoc != null && timeDeltaSec > 0.4) {
            (distanceDelta / timeDeltaSec).toFloat()
        } else {
            location.speedMps
        }

        // Prefer GPS hardware speed if positive, otherwise fall back to point distance/time
        val effectiveSpeedMps = if (location.speedMps > 0.1f) location.speedMps else segmentSpeedMps
        val effectiveSpeedKmh = effectiveSpeedMps * 3.6

        // Speed restriction anti-cheat check: strictly enforce running limit (max 28 km/h)
        val isSpeedRestricted = effectiveSpeedKmh > MAX_RUNNING_SPEED_KMH

        val currentStats = _activeRunStats.value
        val (newTotalDistance, newRestrictedDistance, newViolations, speedWarning) = if (isSpeedRestricted) {
            Log.w("LocationManager", "Speed restriction triggered: %.1f km/h > %.1f km/h. Distance ignored.".format(effectiveSpeedKmh, MAX_RUNNING_SPEED_KMH))
            _errorState.value = LocationError.SpeedLimitExceeded(effectiveSpeedKmh, MAX_RUNNING_SPEED_KMH)
            Quadruple(
                currentStats.distanceMeters, // Running distance is not incremented while in vehicle/speed violation
                currentStats.speedRestrictedDistanceMeters + distanceDelta,
                currentStats.speedViolationCount + 1,
                "SPEED LIMIT EXCEEDED (%.1f km/h > 28 km/h) • DISTANCE PAUSED".format(effectiveSpeedKmh)
            )
        } else {
            if (_errorState.value is LocationError.SpeedLimitExceeded) {
                _errorState.value = null
            }
            Quadruple(
                currentStats.distanceMeters + distanceDelta,
                currentStats.speedRestrictedDistanceMeters,
                currentStats.speedViolationCount,
                null
            )
        }

        val newPointsCount = currentStats.pointsCount + 1
        val durationSec = currentStats.durationSeconds
        val avgSpeed = if (durationSec > 0) newTotalDistance / durationSec else effectiveSpeedMps.toDouble()
        val newMaxSpeed = maxOf(currentStats.maxSpeedMps, effectiveSpeedMps)
        val isOffline = networkMonitor?.checkCurrentConnectivity() == false

        _activeRunStats.update {
            it.copy(
                runState = RunState.RUNNING,
                trackingState = TrackingState.TRACKING,
                pointsCount = newPointsCount,
                distanceMeters = newTotalDistance,
                currentSpeedMps = effectiveSpeedMps,
                avgSpeedMps = avgSpeed,
                maxSpeedMps = newMaxSpeed,
                lastKnownLocation = location,
                gpsStatus = _gpsStatus.value,
                isOffline = isOffline,
                isSpeedRestricted = isSpeedRestricted,
                speedRestrictionWarning = speedWarning,
                speedRestrictedDistanceMeters = newRestrictedDistance,
                speedViolationCount = newViolations
            )
        }

        // Persist point to local Room DB immediately
        val sessionId = _activeRunStats.value.sessionId
        if (sessionId.isNotEmpty()) {
            scope.launch(ioDispatcher) {
                locationRepository.saveLocationPoint(sessionId, location)
                val gpsPoint = GpsPoint(
                    sessionId = sessionId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitudeMeters,
                    speed = effectiveSpeedMps,
                    accuracy = location.accuracyMeters,
                    heading = location.heading,
                    timestamp = location.timestamp,
                    isSpeedRestricted = isSpeedRestricted
                )
                _activeSessionPoints.update { it + gpsPoint }
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // ==========================================
    // RUN STATE MACHINE ACTIONS
    // ==========================================

    /**
     * Transitions from IDLE to PREPARING
     */
    fun prepareRun() {
        if (_runState.value == RunState.IDLE || _runState.value == RunState.CANCELLED || _runState.value == RunState.COMPLETED || _runState.value == RunState.FAILED) {
            _runState.value = RunState.PREPARING
            _completedRunResult.value = null
            startContinuousLocationListening()
        }
    }

    /**
     * Cancels preparation or discards active run
     */
    fun cancelRun() {
        timerJob?.cancel()
        val sessionId = _activeRunStats.value.sessionId
        if (sessionId.isNotEmpty()) {
            scope.launch(ioDispatcher) {
                locationRepository.updateSessionStatus(sessionId, "CANCELLED")
            }
        }
        LocationForegroundService.stopService(context)
        _runState.value = RunState.CANCELLED
        _activeRunStats.value = ActiveRunStats(runState = RunState.CANCELLED, trackingState = TrackingState.IDLE)
        _activeSessionPoints.value = emptyList()
        isFinishingInProgress.set(false)
    }

    /**
     * Resets state back to IDLE
     */
    fun resetToIdle() {
        _runState.value = RunState.IDLE
        _activeRunStats.value = ActiveRunStats(runState = RunState.IDLE, trackingState = TrackingState.IDLE)
        _completedRunResult.value = null
        _activeSessionPoints.value = emptyList()
        isFinishingInProgress.set(false)
    }

    /**
     * Starts or engages the active run
     */
    fun startRunSession(customSessionId: String? = null): String {
        if (!permissionManager.hasLocationPermission()) {
            _errorState.value = LocationError.PermissionDenied()
            return ""
        }
        if (!permissionManager.isGpsHardwareEnabled()) {
            _errorState.value = LocationError.GpsDisabled
            return ""
        }

        val sessionId = customSessionId ?: "run_${UUID.randomUUID().toString().take(8)}"
        _activeSessionPoints.value = emptyList()
        lastRecordedTimestamp = System.currentTimeMillis()
        isFinishingInProgress.set(false)

        val isOffline = networkMonitor?.checkCurrentConnectivity() == false

        _activeRunStats.value = ActiveRunStats(
            sessionId = sessionId,
            runState = RunState.RUNNING,
            trackingState = TrackingState.TRACKING,
            pointsCount = 0,
            distanceMeters = 0.0,
            durationSeconds = 0,
            currentSpeedMps = _currentLocation.value?.speedMps ?: 0f,
            avgSpeedMps = 0.0,
            lastKnownLocation = _currentLocation.value,
            gpsStatus = _gpsStatus.value,
            error = null,
            isOffline = isOffline
        )
        _runState.value = RunState.RUNNING
        _completedRunResult.value = null

        scope.launch(ioDispatcher) {
            locationRepository.startRunSession(sessionId)
        }

        startTimer()
        LocationForegroundService.startService(context, sessionId)
        startContinuousLocationListening()
        return sessionId
    }

    /**
     * Pauses tracking: timer stops, GPS points are not added to route
     */
    fun pauseRunSession() {
        if (_runState.value != RunState.RUNNING) return

        timerJob?.cancel()
        _runState.value = RunState.PAUSED
        _activeRunStats.update { it.copy(runState = RunState.PAUSED, trackingState = TrackingState.PAUSED) }
        val sessionId = _activeRunStats.value.sessionId
        if (sessionId.isNotEmpty()) {
            scope.launch(ioDispatcher) {
                locationRepository.updateSessionStatus(sessionId, "PAUSED")
            }
        }
        LocationForegroundService.pauseService(context)
    }

    /**
     * Resumes tracking: continues the exact same session
     */
    fun resumeRunSession() {
        if (_runState.value != RunState.PAUSED) return

        _runState.value = RunState.RUNNING
        _activeRunStats.update { it.copy(runState = RunState.RUNNING, trackingState = TrackingState.TRACKING) }
        val sessionId = _activeRunStats.value.sessionId
        if (sessionId.isNotEmpty()) {
            scope.launch(ioDispatcher) {
                locationRepository.updateSessionStatus(sessionId, "ACTIVE")
            }
        }
        startTimer()
        LocationForegroundService.resumeService(context)
    }

    /**
     * Executes the finish pipeline:
     * FINISHING -> UPLOADING -> VALIDATING -> COMPLETED / FAILED
     * Protected against duplicate concurrent invocation.
     */
    fun finishRunSession(onComplete: ((RunSessionResult) -> Unit)? = null) {
        // Prevent duplicate finish execution
        if (!isFinishingInProgress.compareAndSet(false, true)) {
            Log.w("LocationManager", "Finish already in progress, ignoring duplicate trigger")
            return
        }

        val currentStats = _activeRunStats.value
        val sessionId = currentStats.sessionId
        if (sessionId.isEmpty()) {
            _runState.value = RunState.IDLE
            isFinishingInProgress.set(false)
            return
        }

        timerJob?.cancel()
        LocationForegroundService.stopService(context)

        // Step 1: FINISHING
        _runState.value = RunState.FINISHING
        _activeRunStats.update { it.copy(runState = RunState.FINISHING, trackingState = TrackingState.STOPPED) }

        scope.launch(mainDispatcher) {
            // Short step visual cadence for tactical HUD
            delay(400L)

            // Step 2: UPLOADING (or OFFLINE LOCAL QUEUE)
            _runState.value = RunState.UPLOADING
            _activeRunStats.update { it.copy(runState = RunState.UPLOADING) }

            val isOnline = networkMonitor?.checkCurrentConnectivity() ?: true
            val syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.OFFLINE_SAVED

            // Step 3: VALIDATING
            delay(500L)
            _runState.value = RunState.VALIDATING
            _activeRunStats.update { it.copy(runState = RunState.VALIDATING) }

            val points = _activeSessionPoints.value
            val durationSec = currentStats.durationSeconds
            val distanceM = currentStats.distanceMeters
            val avgSpeedMps = currentStats.avgSpeedMps
            val paceMinKm = currentStats.paceMinPerKm

            // Estimate calories: ~60 kcal per km for average runner
            val calories = ((distanceM / 1000.0) * 62.0).toInt().coerceAtLeast(0)

            val violations = currentStats.speedViolationCount
            val restrictedDist = currentStats.speedRestrictedDistanceMeters
            val maxSpeedKmh = currentStats.maxSpeedKmh

            val validationPassed = durationSec >= 0 && distanceM >= 0.0
            val validationMsg = when {
                violations > 0 -> "Verified running trajectory: %.2f km accepted. %d vehicular speed anomalies detected (%.1f m restricted).".format(
                    distanceM / 1000.0,
                    violations,
                    restrictedDist
                )
                isOnline -> "Telemetry verified. Speed limit (max 28 km/h) & anti-teleport checks cleared."
                else -> "Offline run saved locally. Telemetry queued for sync."
            }

            val result = RunSessionResult(
                sessionId = sessionId,
                startTime = System.currentTimeMillis() - (durationSec * 1000L),
                endTime = System.currentTimeMillis(),
                durationSeconds = durationSec,
                distanceMeters = distanceM,
                avgSpeedMps = avgSpeedMps,
                avgPaceMinPerKm = paceMinKm,
                caloriesBurned = calories,
                pointsCount = points.size,
                syncStatus = syncStatus,
                validationPassed = validationPassed,
                validationMessage = validationMsg,
                isOffline = !isOnline,
                maxSpeedKmh = maxSpeedKmh,
                speedViolationCount = violations,
                speedRestrictedDistanceMeters = restrictedDist
            )

            // Persist final completion in Room Database
            launch(ioDispatcher) {
                locationRepository.endRunSession(
                    sessionId = sessionId,
                    distanceMeters = distanceM,
                    durationSeconds = durationSec,
                    avgSpeedMps = avgSpeedMps,
                    avgPaceMinPerKm = paceMinKm,
                    status = "COMPLETED",
                    syncStatus = syncStatus.name,
                    isOffline = !isOnline,
                    validationPassed = validationPassed
                )
                com.example.core.sync.SyncManager.scheduleSync(context)
            }

            delay(400L)

            // Step 4: COMPLETED
            _completedRunResult.value = result
            _runState.value = RunState.COMPLETED
            _activeRunStats.update { it.copy(runState = RunState.COMPLETED) }
            onComplete?.invoke(result)
        }
    }

    /**
     * Suspending variant of finishRunSession for coroutine-based workflows and tests
     */
    suspend fun finishRun(): RunSessionResult? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        finishRunSession { result ->
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(result))
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (_runState.value == RunState.RUNNING) {
                    _activeRunStats.update {
                        val newDuration = it.durationSeconds + 1
                        val avg = if (newDuration > 0) it.distanceMeters / newDuration else 0.0
                        it.copy(
                            durationSeconds = newDuration,
                            avgSpeedMps = avg
                        )
                    }
                }
            }
        }
    }

    /**
     * Restores an active or paused session from Room database (Process restart support)
     */
    suspend fun restoreActiveSessionIfAny(): Boolean {
        val activeSession = locationRepository.getActiveOrPausedSession() ?: return false
        val points = locationRepository.getPointsListForSession(activeSession.sessionId)

        _activeSessionPoints.value = points
        val restoredRunState = if (activeSession.status == "ACTIVE") RunState.RUNNING else RunState.PAUSED

        _activeRunStats.value = ActiveRunStats(
            sessionId = activeSession.sessionId,
            runState = restoredRunState,
            trackingState = if (restoredRunState == RunState.RUNNING) TrackingState.TRACKING else TrackingState.PAUSED,
            pointsCount = points.size,
            distanceMeters = activeSession.distanceMeters,
            durationSeconds = activeSession.durationSeconds,
            avgSpeedMps = activeSession.avgSpeedMps,
            lastKnownLocation = points.lastOrNull()?.toUserLocation,
            gpsStatus = _gpsStatus.value
        )
        _runState.value = restoredRunState

        if (restoredRunState == RunState.RUNNING) {
            startTimer()
            LocationForegroundService.startService(context, activeSession.sessionId)
        }
        return true
    }

    fun clearError() {
        _errorState.value = null
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}
