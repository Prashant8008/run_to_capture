package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager as AndroidLocationManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.location.LocationManager
import com.example.core.location.LocationPermissionManager
import com.example.core.network.NetworkMonitor
import com.example.data.repository.LocationRepositoryImpl
import com.example.domain.model.ActiveRunStats
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.LocationError
import com.example.domain.model.RunState
import com.example.domain.model.SyncStatus
import com.example.domain.model.TrackingState
import com.example.domain.model.UserLocation
import com.example.domain.repository.LocationRepository
import com.example.feature.map.WorldMapScreen
import com.example.feature.map.WorldMapViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    override fun checkCurrentConnectivity(): Boolean = _isOnline.value
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationSubsystemRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var locationRepository: LocationRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        context = app

        // Grant location permissions in Robolectric
        shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Enable GPS provider in Robolectric
        val androidLocManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
        shadowOf(androidLocManager).setProviderEnabled(AndroidLocationManager.GPS_PROVIDER, true)
        shadowOf(androidLocManager).setProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER, true)

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        locationRepository = LocationRepositoryImpl(
            locationPointDao = db.locationPointDao(),
            runSessionDao = db.runSessionDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test LocationPermissionManager evaluates GPS accuracy and generates intents`() {
        val permissionManager = LocationPermissionManager(context)

        // Evaluate signal accuracy levels
        assertEquals(GpsSignalStatus.GOOD, permissionManager.evaluateGpsAccuracy(8.0f))
        assertEquals(GpsSignalStatus.GOOD, permissionManager.evaluateGpsAccuracy(15.0f))
        assertEquals(GpsSignalStatus.POOR, permissionManager.evaluateGpsAccuracy(25.0f))
        assertEquals(GpsSignalStatus.SEARCHING, permissionManager.evaluateGpsAccuracy(-1.0f))

        // Check intent creators
        val appSettingsIntent = permissionManager.createAppSettingsIntent()
        assertNotNull(appSettingsIntent)
        assertEquals("package:${context.packageName}", appSettingsIntent.dataString)

        val locationSettingsIntent = permissionManager.createLocationSettingsIntent()
        assertNotNull(locationSettingsIntent)
    }

    @Test
    fun `test complete Run State Machine start pause resume finish and validation`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        // 1. Initial state must be IDLE
        assertEquals(RunState.IDLE, locationManager.runState.value)

        // 2. Prepare Run
        locationManager.prepareRun()
        assertEquals(RunState.PREPARING, locationManager.runState.value)

        // 3. Start Run -> RUNNING
        val sessionId = locationManager.startRunSession("phase7-session")
        assertEquals("phase7-session", sessionId)
        assertEquals(RunState.RUNNING, locationManager.runState.value)
        assertEquals(TrackingState.TRACKING, locationManager.activeRunStats.value.trackingState)

        // Ingest Point 1
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = 5.0f,
                speedMps = 3.0f,
                timestamp = 10000L
            )
        )
        advanceUntilIdle()

        // Ingest Point 2 (~100m away)
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7758,
                longitude = -122.4194,
                accuracyMeters = 5.0f,
                speedMps = 3.2f,
                timestamp = 25000L
            )
        )
        advanceUntilIdle()

        assertEquals(2, locationManager.activeRunStats.value.pointsCount)
        assertTrue(locationManager.activeRunStats.value.distanceMeters > 50.0)

        // 4. Pause Run -> PAUSED
        locationManager.pauseRunSession()
        advanceUntilIdle()
        assertEquals(RunState.PAUSED, locationManager.runState.value)
        assertEquals(TrackingState.PAUSED, locationManager.activeRunStats.value.trackingState)

        // Ingest Point during PAUSE (should not add distance)
        val distanceBefore = locationManager.activeRunStats.value.distanceMeters
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7765,
                longitude = -122.4194,
                accuracyMeters = 5.0f,
                speedMps = 3.0f,
                timestamp = 35000L
            )
        )
        advanceUntilIdle()
        assertEquals(distanceBefore, locationManager.activeRunStats.value.distanceMeters, 0.001)

        // 5. Resume Run -> RUNNING
        locationManager.resumeRunSession()
        advanceUntilIdle()
        assertEquals(RunState.RUNNING, locationManager.runState.value)

        // 6. Finish Run -> FINISHING -> UPLOADING -> VALIDATING -> COMPLETED
        val result = locationManager.finishRun()
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals(RunState.COMPLETED, locationManager.runState.value)
        assertEquals("phase7-session", result!!.sessionId)
        assertTrue(result.validationPassed)
        assertEquals(SyncStatus.SYNCED, result.syncStatus)

        // Check local DB entity
        val dbSession = locationRepository.getSessionById("phase7-session")
        assertNotNull(dbSession)
        assertEquals("COMPLETED", dbSession!!.status)
        assertEquals("SYNCED", dbSession.syncStatus)
        assertTrue(dbSession.validationPassed)
    }

    @Test
    fun `test duplicate finish protection guarded by atomic state`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        locationManager.startRunSession("duplicate-finish-test")
        advanceUntilIdle()

        // Call finish concurrently twice
        val res1 = locationManager.finishRun()
        val res2 = locationManager.finishRun()
        advanceUntilIdle()

        assertNotNull(res1)
        // res2 should be the same completed result or handled gracefully without throwing
        assertEquals(RunState.COMPLETED, locationManager.runState.value)
    }

    @Test
    fun `test network loss and offline persistence during run`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = false) // OFFLINE
        val permissionManager = LocationPermissionManager(context)

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val sessionId = "offline-run-001"
        locationManager.startRunSession(sessionId)
        advanceUntilIdle()

        assertTrue(locationManager.activeRunStats.value.isOffline)

        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = 5.0f,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()

        val result = locationManager.finishRun()
        advanceUntilIdle()

        assertNotNull(result)
        assertTrue(result!!.isOffline)
        assertEquals(SyncStatus.OFFLINE_SAVED, result.syncStatus)

        // Query pending sync items
        val pendingSessions = locationRepository.getPendingSyncSessions()
        assertTrue(pendingSessions.any { it.sessionId == sessionId })
    }

    @Test
    fun `test GPS gap handling and teleport filtering`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        locationManager.startRunSession("teleport-gap-session")
        advanceUntilIdle()

        // Point 1: Valid
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = 4.0f,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()
        assertEquals(1, locationManager.activeRunStats.value.pointsCount)

        // Point 2: Teleport anomaly (>250m away in 1 second, impossible running speed)
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.8500, // ~8 km away
                longitude = -122.4194,
                accuracyMeters = 4.0f,
                timestamp = 2000L
            )
        )
        advanceUntilIdle()

        // Teleport point must be filtered out
        assertEquals(1, locationManager.activeRunStats.value.pointsCount)
        assertEquals(0.0, locationManager.activeRunStats.value.distanceMeters, 0.01)

        // Point 3: Legitimate point after normal movement
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7752,
                longitude = -122.4194,
                accuracyMeters = 4.0f,
                timestamp = 8000L
            )
        )
        advanceUntilIdle()
        assertEquals(2, locationManager.activeRunStats.value.pointsCount)
        assertTrue(locationManager.activeRunStats.value.distanceMeters > 20.0)

        // Point 4: GPS Gap (>30 seconds without fix, e.g. tunnel)
        fakeClient.locationFlow.emit(
            UserLocation(
                latitude = 37.7760,
                longitude = -122.4194,
                accuracyMeters = 4.0f,
                timestamp = 50000L // 42 seconds later
            )
        )
        advanceUntilIdle()

        // Point accepted but delta distance ignored to prevent tunnel teleport spikes
        assertEquals(3, locationManager.activeRunStats.value.pointsCount)
    }

    @Test
    fun `test process restart unclosed run restoration into PAUSED state`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)

        val sessionId = "crashed-process-session"
        locationRepository.startRunSession(sessionId)
        locationRepository.saveLocationPoint(
            sessionId,
            UserLocation(
                latitude = 37.7800,
                longitude = -122.4100,
                accuracyMeters = 5.0f,
                altitudeMeters = 15.0,
                speedMps = 4.0f,
                bearingDegrees = 45.0f
            )
        )

        val newLocationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val restored = newLocationManager.restoreActiveSessionIfAny()
        assertTrue(restored)
        assertEquals(RunState.PAUSED, newLocationManager.runState.value)
        assertEquals(TrackingState.PAUSED, newLocationManager.activeRunStats.value.trackingState)
        assertEquals(sessionId, newLocationManager.activeRunStats.value.sessionId)
        assertEquals(1, newLocationManager.activeRunStats.value.pointsCount)
    }

    @Test
    fun `test Active Run HUD and Preparation Dialog in WorldMapScreen UI`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val viewModel = WorldMapViewModel(
            locationClient = fakeClient,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom,
            locationManager = locationManager,
            permissionManager = permissionManager
        )

        composeTestRule.setContent {
            WorldMapScreen(
                viewModel = viewModel,
                onNavigateToCustomization = {},
                onNavigateToIdentity = {}
            )
        }
        
        composeTestRule.waitForIdle()

        // Before start: bottom hud with start button
        composeTestRule.onNodeWithTag("start_run_button").assertIsDisplayed()

        // Click START RUN -> opens Run Preparation dialog
        composeTestRule.onNodeWithTag("start_run_button").performClick()
        advanceUntilIdle()

        composeTestRule.onNodeWithTag("confirm_start_run_button").assertIsDisplayed()

        // Confirm Start in Preparation dialog
        composeTestRule.onNodeWithTag("confirm_start_run_button").performClick()
        advanceUntilIdle()

        // Active run HUD is displayed
        composeTestRule.onNodeWithTag("active_run_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pause_run_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("finish_run_button").assertIsDisplayed()

        // Click Pause
        composeTestRule.onNodeWithTag("pause_run_button").performClick()
        advanceUntilIdle()
        composeTestRule.onNodeWithTag("resume_run_button").assertIsDisplayed()

        // Click Finish -> opens confirmation dialog
        composeTestRule.onNodeWithTag("finish_run_button").performClick()
        advanceUntilIdle()

        composeTestRule.onNodeWithTag("confirm_finish_run_button").assertIsDisplayed()

        // Confirm Finish -> completes pipeline & opens summary
        composeTestRule.onNodeWithTag("confirm_finish_run_button").performClick()
        advanceUntilIdle()

        composeTestRule.onNodeWithTag("return_to_radar_button").assertIsDisplayed()

        // Dismiss summary -> back to radar
        composeTestRule.onNodeWithTag("return_to_radar_button").performClick()
        advanceUntilIdle()

        composeTestRule.onNodeWithTag("start_run_button").assertIsDisplayed()
    }
}
