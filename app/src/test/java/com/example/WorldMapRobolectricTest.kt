package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.location.LocationClient
import com.example.domain.model.AuthState
import com.example.domain.model.AuthUser
import com.example.domain.model.DevTerritory
import com.example.domain.model.Faction
import com.example.domain.model.FlagConfig
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.LatLng
import com.example.domain.model.PlayerCustomization
import com.example.domain.model.UserLocation
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CustomizationRepository
import com.example.feature.map.MapConfig
import com.example.feature.map.WorldMapScreen
import com.example.feature.map.WorldMapViewModel
import com.example.feature.map.bridge.LeafletBridge
import com.example.feature.map.bridge.LeafletMapController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeLocationClient : LocationClient {
    val locationFlow = MutableSharedFlow<UserLocation>(replay = 1)
    var hasPermission = true
    var gpsEnabled = true
    var cachedLocation: UserLocation? = null

    override fun getLocationUpdates(intervalMs: Long): Flow<UserLocation> = locationFlow
    override fun getLastKnownLocation(): UserLocation? = cachedLocation
    override fun hasLocationPermission(): Boolean = hasPermission
    override fun isGpsEnabled(): Boolean = gpsEnabled
    override fun getGpsStatus(accuracyMeters: Float): GpsSignalStatus {
        if (!gpsEnabled) return GpsSignalStatus.DISABLED
        return when {
            accuracyMeters <= 0f -> GpsSignalStatus.SEARCHING
            accuracyMeters <= 15f -> GpsSignalStatus.GOOD
            else -> GpsSignalStatus.POOR
        }
    }
}

class FakeAuthRepository(initialUser: AuthUser? = null) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(
        if (initialUser != null) AuthState.Authenticated(initialUser)
        else AuthState.Authenticated(
            AuthUser(
                id = "usr-test-1",
                email = "apex.runner@sector.io",
                displayName = "VANGUARD_01",
                faction = Faction.APEX,
                territoryColor = "red",
                totalAreaSqMeters = 14500.0,
                totalDistanceMeters = 24300.0,
                territoriesCount = 5
            )
        )
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun checkSession() = throw NotImplementedError()
    override suspend fun registerWithEmail(email: String, password: String, displayName: String, faction: Faction) = throw NotImplementedError()
    override suspend fun loginWithEmail(email: String, password: String) = throw NotImplementedError()
    override suspend fun loginWithGoogle(idToken: String, displayName: String?, faction: Faction?) = throw NotImplementedError()
    override suspend fun refreshToken() = throw NotImplementedError()
    override suspend fun logout(): com.example.domain.model.AuthResult<Unit> = com.example.domain.model.AuthResult.Success(Unit)
    override fun clearError() {}
    
    override suspend fun awardXpAndArea(areaSqMeters: Double) {}
    override suspend fun awardProgression(
        sources: List<Pair<com.example.core.progression.XpSource, Int>>, 
        newAreaSqMeters: Double, 
        newDistanceMeters: Double, 
        territoriesCaptured: Int
    ) {}
}

class FakeCustomizationRepository : CustomizationRepository {
    private val _customState = MutableStateFlow(
        PlayerCustomization(
            territoryColor = "red",
            flag = FlagConfig(background = "crimson", pattern = "cross", emblem = "wolf", border = "gold")
        )
    )
    override val customizationState: StateFlow<PlayerCustomization> = _customState.asStateFlow()

    override suspend fun loadCustomization(): Result<PlayerCustomization> = Result.success(_customState.value)
    
    override suspend fun saveCustomization(territoryColor: String, flag: FlagConfig): Result<PlayerCustomization> {
        val updated = PlayerCustomization(territoryColor, flag)
        _customState.value = updated
        return Result.success(updated)
    }

    override fun setLocalCustomization(customization: PlayerCustomization) {
        _customState.value = customization
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorldMapRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test world map view model initialization and initial states`() = runTest {
        val fakeLocation = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()

        val viewModel = WorldMapViewModel(
            locationClient = fakeLocation,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom
        )

        val state = viewModel.uiState.value
        assertEquals(Faction.APEX, state.faction)
        assertEquals("VANGUARD_01", state.username)
        assertEquals("#FF3B30", state.territoryColorHex)
        assertTrue(state.isFollowingUser)
        assertEquals(MapConfig.DEFAULT_ZOOM, state.currentZoom)
    }

    @Test
    fun `test GPS location updates and signal status`() = runTest {
        val fakeLocation = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()

        val viewModel = WorldMapViewModel(
            locationClient = fakeLocation,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom
        )

        // Emit GPS location update
        val mockLocation = UserLocation(
            latitude = 37.7799,
            longitude = -122.4148,
            accuracyMeters = 5.2f,
            altitudeMeters = 15.0,
            speedMps = 3.5f,
            bearingDegrees = 90f
        )
        fakeLocation.locationFlow.emit(mockLocation)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.userLocation)
        assertEquals(37.7799, state.userLocation!!.latitude, 0.0001)
        assertEquals(-122.4148, state.userLocation!!.longitude, 0.0001)
        assertEquals(GpsSignalStatus.GOOD, state.gpsStatus)
    }

    @Test
    fun `test map event listener callbacks and bridge interaction`() = runTest {
        val fakeLocation = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()

        val viewModel = WorldMapViewModel(
            locationClient = fakeLocation,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom
        )

        // Simulate Leaflet mapReady event
        viewModel.onMapReady()
        assertTrue(viewModel.uiState.value.isMapReady)
        assertFalse(viewModel.uiState.value.isTileLoading)
        assertNull(viewModel.uiState.value.tileError)

        // Simulate Map Move
        viewModel.onMapMoved(37.7800, -122.4200)
        assertEquals(37.7800, viewModel.uiState.value.centerLocation.latitude, 0.0001)
        assertEquals(-122.4200, viewModel.uiState.value.centerLocation.longitude, 0.0001)

        // Simulate Zoom Change
        viewModel.onMapZoomChanged(18)
        assertEquals(18, viewModel.uiState.value.currentZoom)

        // Simulate Tile error and retry
        viewModel.onTileError("Tile timeout")
        assertEquals("Tile timeout", viewModel.uiState.value.tileError)
        assertFalse(viewModel.uiState.value.isTileLoading)

        viewModel.onRetryMapLoad()
        assertNull(viewModel.uiState.value.tileError)
        assertTrue(viewModel.uiState.value.isTileLoading)

        viewModel.onTileLoaded()
        assertFalse(viewModel.uiState.value.isTileLoading)
        assertNull(viewModel.uiState.value.tileError)
    }

    @Test
    fun `test dev territory toggling and polygon inspection`() = runTest {
        val fakeLocation = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()

        val viewModel = WorldMapViewModel(
            locationClient = fakeLocation,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom
        )

        // Dev territory overlay is active by default for immediate testing
        assertTrue(viewModel.uiState.value.isDevTerritoryOverlayActive)

        // Initialize map
        viewModel.onMapReady()
        assertEquals(6, viewModel.uiState.value.devTerritories.size)

        // Click on sector
        val sectorId = viewModel.uiState.value.devTerritories.first().id
        viewModel.onTerritoryClicked(sectorId)
        assertNotNull(viewModel.uiState.value.selectedDevTerritory)
        assertEquals(sectorId, viewModel.uiState.value.selectedDevTerritory!!.id)

        // Dismiss
        viewModel.dismissSelectedTerritory()
        assertNull(viewModel.uiState.value.selectedDevTerritory)

        // Toggle off
        viewModel.toggleDevTerritories()
        assertFalse(viewModel.uiState.value.isDevTerritoryOverlayActive)
        assertEquals(0, viewModel.uiState.value.devTerritories.size)

        // Toggle back on
        viewModel.toggleDevTerritories()
        assertTrue(viewModel.uiState.value.isDevTerritoryOverlayActive)
        assertEquals(6, viewModel.uiState.value.devTerritories.size)
    }

    @Test
    fun `test world map UI elements and buttons render in Compose`() {
        val fakeLocation = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()

        val viewModel = WorldMapViewModel(
            locationClient = fakeLocation,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom
        )

        composeTestRule.setContent {
            WorldMapScreen(
                viewModel = viewModel,
                onNavigateToCustomization = {},
                onNavigateToIdentity = {}
            )
        }

        // Verify Top HUD
        composeTestRule.onNodeWithTag("world_map_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_map_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_level_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notification_button").assertIsDisplayed()

        // Verify Map Controls
        composeTestRule.onNodeWithTag("map_layer_selector_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dev_territories_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recenter_location_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("map_zoom_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("map_zoom_out_button").assertIsDisplayed()

        // Verify Bottom Navigation & START RUN CTA
        composeTestRule.onNodeWithTag("bottom_map_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_run_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_forge_tab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_operative_tab").assertIsDisplayed()
    }

    @Test
    fun `test map layer switcher opens and allows switching from street to satellite`() = runTest {
        val fakeLocation = FakeLocationClient()
        val fakeAuth = FakeAuthRepository()
        val fakeCustom = FakeCustomizationRepository()

        val viewModel = WorldMapViewModel(
            locationClient = fakeLocation,
            authRepository = fakeAuth,
            customizationRepository = fakeCustom
        )

        // Verify default layer is Street View
        assertEquals(com.example.feature.map.MapLayerType.STREET, viewModel.uiState.value.selectedMapLayer)
        assertFalse(viewModel.uiState.value.showLayerSelectorModal)

        // Toggle layer selector
        viewModel.toggleLayerSelector()
        assertTrue(viewModel.uiState.value.showLayerSelectorModal)

        // Select Satellite layer
        viewModel.selectMapLayer(com.example.feature.map.MapLayerType.SATELLITE)
        assertEquals(com.example.feature.map.MapLayerType.SATELLITE, viewModel.uiState.value.selectedMapLayer)
        assertFalse(viewModel.uiState.value.showLayerSelectorModal)
    }
}
