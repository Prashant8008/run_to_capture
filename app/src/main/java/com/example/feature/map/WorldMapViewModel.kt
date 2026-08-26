package com.example.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.location.LocationClient
import com.example.core.location.LocationManager
import com.example.core.location.LocationPermissionManager
import com.example.domain.model.ActiveRunStats
import com.example.domain.model.DevTerritory
import com.example.domain.model.Faction
import com.example.domain.model.GpsPoint
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.LatLng
import com.example.domain.model.LocationError
import com.example.domain.model.LocationPermissionState
import com.example.domain.model.RunSessionResult
import com.example.domain.model.RunState
import com.example.domain.model.UserLocation
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CustomizationRepository
import com.example.feature.map.bridge.LeafletBridge
import com.example.feature.map.bridge.MapEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val isMapReady: Boolean = false,
    val isTileLoading: Boolean = true,
    val tileError: String? = null,
    val selectedMapLayer: MapLayerType = MapConfig.DEFAULT_LAYER,
    val showLayerSelectorModal: Boolean = false,
    val userLocation: UserLocation? = null,
    val gpsStatus: GpsSignalStatus = GpsSignalStatus.SEARCHING,
    val centerLocation: LatLng = LatLng(MapConfig.DEFAULT_LAT, MapConfig.DEFAULT_LNG),
    val currentZoom: Int = MapConfig.DEFAULT_ZOOM,
    val isFollowingUser: Boolean = true,
    val playerLevel: Int = 12,
    val playerXpProgress: Float = 0.68f,
    val faction: Faction = Faction.CIPHER,
    val territoryColorHex: String = "#00F0FF",
    val username: String = "OPERATIVE",
    val unreadNotificationsCount: Int = 2,
    val isDevTerritoryOverlayActive: Boolean = true,
    val devTerritories: List<DevTerritory> = emptyList(),
    val selectedDevTerritory: DevTerritory? = null,
    val showNotificationModal: Boolean = false,
    val showStartRunModal: Boolean = false,
    val showRunPreparationSheet: Boolean = false,
    val showFinishConfirmDialog: Boolean = false,
    // Active Location & Phase 7 Run State
    val runState: RunState = RunState.IDLE,
    val activeRunStats: ActiveRunStats = ActiveRunStats(),
    val completedRunResult: RunSessionResult? = null,
    val activePoints: List<GpsPoint> = emptyList(),
    val permissionState: LocationPermissionState = LocationPermissionState.GRANTED,
    val locationError: LocationError? = null,
    val showPermissionRationaleDialog: Boolean = false,
    val showPermanentlyDeniedDialog: Boolean = false,
    val showGpsDisabledDialog: Boolean = false,
    // Phase 9: Territory Expansion State
    val expansionPreview: com.example.domain.model.ExpansionPreviewResult? = null,
    val confirmedExpansion: com.example.domain.model.ServerConfirmedExpansion? = null,
    val showExpansionModal: Boolean = false,
    val isExpansionLoading: Boolean = false,
    val expansionError: String? = null,
    val isExpansionConfirmed: Boolean = false,
    val showExistingLayer: Boolean = true,
    val showNewCellsLayer: Boolean = true,
    val showPreviewMergedLayer: Boolean = true,
    // Phase 10: Territory Attacks
    val showTerritoryDetailsModal: Boolean = false,
    val attackEligibility: com.example.domain.model.AttackValidationResult? = null,
    val activeBattle: com.example.domain.model.BattleSession? = null,
    val showAttackPreparationModal: Boolean = false,
    val battleEvaluation: com.example.domain.model.BattleChallengeEvaluation? = null,
    val unreadNotificationCount: Int = 0,
    val showAcquisitionOverlay: Boolean = !WorldMapViewModel.hasCalibratedInitialLocation,
    val hasCalibratedLocation: Boolean = WorldMapViewModel.hasCalibratedInitialLocation
)

class WorldMapViewModel(
    private val locationClient: LocationClient,
    private val authRepository: AuthRepository,
    private val customizationRepository: CustomizationRepository,
    private val competitiveRepository: com.example.domain.repository.CompetitiveRepository? = null,
    private val notificationRepository: com.example.domain.repository.NotificationRepository? = null,
    val locationManager: LocationManager? = null,
    val permissionManager: LocationPermissionManager? = null,
    val territoryRepository: com.example.domain.repository.TerritoryRepository? = null,
    val battleRepository: com.example.domain.repository.BattleRepository? = null
) : ViewModel(), MapEventListener {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var mapController: LeafletBridge? = null

    init {
        observeUserData()
        observeLocationSubsystem()
        observeTerritories()
        startLocationTracking()
        restorePreviousRunIfAny()
    }

    private fun observeTerritories() {
        if (territoryRepository != null) {
            viewModelScope.launch {
                territoryRepository.observeAllDevTerritories().collect { territories ->
                    _uiState.update { it.copy(devTerritories = territories) }
                    if (_uiState.value.isMapReady && _uiState.value.isDevTerritoryOverlayActive) {
                        mapController?.renderTerritories(territories)
                    }
                }
            }
        }
    }

    private fun observeUserData() {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                if (authState is com.example.domain.model.AuthState.Authenticated) {
                    val user = authState.user
                    val level = user.level
                    val xp = if (user.nextLevelXp > 0) (user.xp.toFloat() / user.nextLevelXp.toFloat()) else 0f
                    
                    _uiState.update {
                        it.copy(
                            faction = user.faction,
                            username = user.displayName.ifEmpty { "OPERATIVE" },
                            playerLevel = level,
                            playerXpProgress = xp
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            customizationRepository.customizationState.collect { cust ->
                val hex = cust.territoryColorHex
                _uiState.update { it.copy(territoryColorHex = hex) }
                // Update marker color on map
                val loc = _uiState.value.userLocation
                if (loc != null && _uiState.value.isMapReady) {
                    mapController?.setUserLocation(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        accuracy = loc.accuracyMeters,
                        heading = loc.bearingDegrees,
                        colorHex = hex
                    )
                }
            }
        }
    }

    private fun observeLocationSubsystem() {
        if (locationManager != null) {
            viewModelScope.launch {
                locationManager.currentLocation.collect { loc ->
                    if (loc != null) {
                        val isFirstFix = _uiState.value.userLocation == null
                        _uiState.update { current ->
                            current.copy(
                                userLocation = loc,
                                centerLocation = if (current.isFollowingUser) LatLng(loc.latitude, loc.longitude) else current.centerLocation
                            )
                        }

                        if (_uiState.value.isMapReady) {
                            mapController?.setUserLocation(
                                lat = loc.latitude,
                                lng = loc.longitude,
                                accuracy = loc.accuracyMeters,
                                heading = loc.bearingDegrees,
                                colorHex = _uiState.value.territoryColorHex
                            )

                            if (_uiState.value.isFollowingUser) {
                                mapController?.setCenter(loc.latitude, loc.longitude, animated = true)
                            }
                            if (_uiState.value.isDevTerritoryOverlayActive && isFirstFix && _uiState.value.devTerritories.isEmpty()) {
                                val sampleSectors = generateDevTerritories(loc.latitude, loc.longitude)
                                viewModelScope.launch {
                                    territoryRepository?.seedMockTerritories(sampleSectors)
                                }
                            }
                        }
                    }
                }
            }

            viewModelScope.launch {
                locationManager.gpsStatus.collect { status ->
                    _uiState.update { it.copy(gpsStatus = status) }
                }
            }

            viewModelScope.launch {
                notificationRepository?.unreadCount?.collect { count ->
                    _uiState.update { it.copy(unreadNotificationCount = count) }
                }
            }

            viewModelScope.launch {
                locationManager.runState.collect { state ->
                    _uiState.update { it.copy(runState = state) }
                    if (state == RunState.IDLE || state == RunState.CANCELLED) {
                        mapController?.clearRoute()
                    }
                }
            }

            viewModelScope.launch {
                locationManager.activeRunStats.collect { stats ->
                    _uiState.update { it.copy(activeRunStats = stats) }
                }
            }

            viewModelScope.launch {
                locationManager.completedRunResult.collect { result ->
                    if (result != null) {
                        // Check if we were running a battle session
                        val activeBattle = _uiState.value.activeBattle
                        if (activeBattle != null && result.sessionId == activeBattle.battleId) {
                            val eval = battleRepository?.evaluateBattleOutcome(
                                battleId = activeBattle.battleId,
                                distanceCompletedMeters = result.distanceMeters,
                                paceAchievedMinPerKm = result.avgPaceMinPerKm,
                                elapsedSeconds = result.durationSeconds.toLong()
                            )
                            _uiState.update { it.copy(battleEvaluation = eval) }
                        } else {
                            // Standard run XP progression
                            if (result.validationPassed) {
                                val sources = mutableListOf(Pair(com.example.core.progression.XpSource.VALID_RUN, 1))
                                if (result.distanceMeters >= 1000) {
                                    sources.add(Pair(com.example.core.progression.XpSource.DISTANCE_KM, (result.distanceMeters / 1000).toInt()))
                                }
                                authRepository.awardProgression(
                                    sources = sources,
                                    newDistanceMeters = result.distanceMeters
                                )
                                
                                val userId = (authRepository.authState.value as? com.example.domain.model.AuthState.Authenticated)?.user?.id ?: "unknown"
                                competitiveRepository?.updateChallengeProgress(
                                    userId = userId,
                                    condition = com.example.domain.model.ChallengeCondition.DISTANCE_KM,
                                    amount = result.distanceMeters / 1000.0
                                )
                            }
                        }
                    }
                    _uiState.update { it.copy(completedRunResult = result) }
                }
            }

            viewModelScope.launch {
                locationManager.errorState.collect { err ->
                    _uiState.update { current ->
                        current.copy(
                            locationError = err,
                            showGpsDisabledDialog = err is LocationError.GpsDisabled,
                            showPermanentlyDeniedDialog = (err as? LocationError.PermissionDenied)?.isPermanent == true
                        )
                    }
                }
            }

            viewModelScope.launch {
                locationManager.activeSessionPoints.collect { points ->
                    _uiState.update { it.copy(activePoints = points) }
                    val currentRunState = _uiState.value.runState
                    if ((currentRunState == RunState.RUNNING || currentRunState == RunState.PAUSED) && points.size >= 2) {
                        val routeCoords = points.map { LatLng(it.latitude, it.longitude) }
                        mapController?.updateRoute(routeCoords, _uiState.value.territoryColorHex)
                    }
                }
            }
        }

        if (permissionManager != null) {
            viewModelScope.launch {
                permissionManager.permissionState.collect { state ->
                    _uiState.update {
                        it.copy(
                            permissionState = state,
                            showPermissionRationaleDialog = state == LocationPermissionState.RATIONALE_REQUIRED
                        )
                    }
                }
            }
        }
    }

    private fun restorePreviousRunIfAny() {
        viewModelScope.launch {
            locationManager?.restoreActiveSessionIfAny()
        }
    }

    fun bindMapController(controller: LeafletBridge) {
        this.mapController = controller
    }

    fun startLocationTracking() {
        if (locationManager != null) {
            locationManager.startContinuousLocationListening()
        } else {
            viewModelScope.launch {
                if (locationClient.hasLocationPermission()) {
                    locationClient.getLocationUpdates(2000L).collect { loc ->
                        val status = locationClient.getGpsStatus(loc.accuracyMeters)
                        _uiState.update { current ->
                            current.copy(
                                userLocation = loc,
                                gpsStatus = status
                            )
                        }

                        if (_uiState.value.isMapReady) {
                            mapController?.setUserLocation(
                                lat = loc.latitude,
                                lng = loc.longitude,
                                accuracy = loc.accuracyMeters,
                                heading = loc.bearingDegrees,
                                colorHex = _uiState.value.territoryColorHex
                            )

                            if (_uiState.value.isFollowingUser) {
                                mapController?.setCenter(loc.latitude, loc.longitude, animated = true)
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(gpsStatus = GpsSignalStatus.DISABLED)
                    }
                }
            }
        }
    }

    // --- Active Run Operations (Phase 7 State Machine) ---

    fun openRunPreparation() {
        if (permissionManager != null && !permissionManager.hasLocationPermission()) {
            val state = permissionManager.checkPermissionState()
            if (state == LocationPermissionState.RATIONALE_REQUIRED) {
                _uiState.update { it.copy(showPermissionRationaleDialog = true) }
            } else if (state == LocationPermissionState.PERMANENTLY_DENIED) {
                _uiState.update { it.copy(showPermanentlyDeniedDialog = true) }
            } else {
                _uiState.update { it.copy(showPermissionRationaleDialog = true) }
            }
            return
        }

        if (permissionManager != null && !permissionManager.isGpsHardwareEnabled()) {
            _uiState.update { it.copy(showGpsDisabledDialog = true) }
            return
        }

        locationManager?.prepareRun()
        _uiState.update { it.copy(showRunPreparationSheet = true, showStartRunModal = false) }
    }

    fun closeRunPreparation() {
        _uiState.update { it.copy(showRunPreparationSheet = false) }
        if (_uiState.value.runState == RunState.PREPARING) {
            locationManager?.resetToIdle()
        }
    }

    fun startRun() {
        _uiState.update { it.copy(showRunPreparationSheet = false, showStartRunModal = false) }
        locationManager?.startRunSession()
    }

    fun pauseRun() {
        locationManager?.pauseRunSession()
    }

    fun resumeRun() {
        locationManager?.resumeRunSession()
    }

    fun requestFinishRun() {
        _uiState.update { it.copy(showFinishConfirmDialog = true) }
    }

    fun dismissFinishConfirmDialog() {
        _uiState.update { it.copy(showFinishConfirmDialog = false) }
    }

    fun confirmFinishRun() {
        _uiState.update { it.copy(showFinishConfirmDialog = false) }
        locationManager?.finishRunSession()
    }

    fun cancelRun() {
        _uiState.update {
            it.copy(
                showFinishConfirmDialog = false,
                showRunPreparationSheet = false
            )
        }
        locationManager?.cancelRun()
        mapController?.clearRoute()
    }

    fun closeCompletedRunSummary() {
        locationManager?.resetToIdle()
        mapController?.clearRoute()
    }

    fun onPermissionResult(fineGranted: Boolean, coarseGranted: Boolean) {
        if (fineGranted || coarseGranted) {
            _uiState.update {
                it.copy(
                    permissionState = LocationPermissionState.GRANTED,
                    showPermissionRationaleDialog = false,
                    showPermanentlyDeniedDialog = false
                )
            }
            startLocationTracking()
        } else {
            val isPermanent = permissionManager?.checkPermissionState() == LocationPermissionState.PERMANENTLY_DENIED
            _uiState.update {
                it.copy(
                    permissionState = if (isPermanent) LocationPermissionState.PERMANENTLY_DENIED else LocationPermissionState.DENIED,
                    showPermissionRationaleDialog = false,
                    showPermanentlyDeniedDialog = isPermanent
                )
            }
        }
    }

    fun dismissRationaleDialog() {
        _uiState.update { it.copy(showPermissionRationaleDialog = false) }
    }

    fun dismissPermanentlyDeniedDialog() {
        _uiState.update { it.copy(showPermanentlyDeniedDialog = false) }
    }

    fun dismissGpsDisabledDialog() {
        _uiState.update { it.copy(showGpsDisabledDialog = false) }
    }

    fun dismissLocationError() {
        locationManager?.clearError()
        _uiState.update { it.copy(locationError = null) }
    }

    fun centerOnUser() {
        val userLoc = _uiState.value.userLocation
        _uiState.update { it.copy(isFollowingUser = true) }
        if (userLoc != null) {
            mapController?.setCenter(userLoc.latitude, userLoc.longitude, animated = true)
        } else {
            mapController?.setCenter(MapConfig.DEFAULT_LAT, MapConfig.DEFAULT_LNG, animated = true)
        }
    }

    fun flyToUser(zoom: Int = 16, durationSec: Double = 1.2) {
        val userLoc = _uiState.value.userLocation
        _uiState.update { it.copy(isFollowingUser = true) }
        if (userLoc != null) {
            mapController?.flyTo(userLoc.latitude, userLoc.longitude, zoom = zoom, durationSec = durationSec)
        } else {
            mapController?.flyTo(MapConfig.DEFAULT_LAT, MapConfig.DEFAULT_LNG, zoom = zoom, durationSec = durationSec)
        }
    }

    fun zoomIn() {
        mapController?.zoomIn()
    }

    fun zoomOut() {
        mapController?.zoomOut()
    }

    fun toggleDevTerritories() {
        val nextActive = !_uiState.value.isDevTerritoryOverlayActive
        val center = _uiState.value.userLocation?.toLatLng ?: _uiState.value.centerLocation
        val sampleSectors = generateDevTerritories(center.latitude, center.longitude)

        if (nextActive) {
            _uiState.update { it.copy(isDevTerritoryOverlayActive = true, devTerritories = sampleSectors) }
            if (territoryRepository != null) {
                viewModelScope.launch {
                    territoryRepository.seedMockTerritories(sampleSectors)
                }
            } else {
                mapController?.renderTerritories(sampleSectors)
            }
        } else {
            _uiState.update { it.copy(isDevTerritoryOverlayActive = false, devTerritories = emptyList(), selectedDevTerritory = null) }
            mapController?.clearTerritories()
        }
    }

    fun selectMapLayer(layer: MapLayerType) {
        _uiState.update { it.copy(selectedMapLayer = layer, showLayerSelectorModal = false) }
        mapController?.setTileUrl(
            url = layer.tileUrl,
            subdomains = layer.subdomains,
            maxZoom = layer.maxZoom,
            attribution = layer.attribution
        )
    }

    fun toggleLayerSelector() {
        _uiState.update { it.copy(showLayerSelectorModal = !it.showLayerSelectorModal) }
    }

    fun dismissLayerSelector() {
        _uiState.update { it.copy(showLayerSelectorModal = false) }
    }

    fun onRetryMapLoad() {
        val layer = _uiState.value.selectedMapLayer
        _uiState.update { it.copy(isTileLoading = true, tileError = null) }
        mapController?.setTileUrl(layer.tileUrl, layer.subdomains, layer.maxZoom, layer.attribution)
    }

    fun openNotificationModal() {
        _uiState.update { it.copy(showNotificationModal = true) }
    }

    fun closeNotificationModal() {
        _uiState.update { it.copy(showNotificationModal = false, unreadNotificationsCount = 0) }
    }

    fun openStartRunModal() {
        openRunPreparation()
    }

    fun closeStartRunModal() {
        _uiState.update { it.copy(showStartRunModal = false) }
    }

    fun dismissSelectedTerritory() {
        _uiState.update { it.copy(selectedDevTerritory = null) }
    }

    // --- MapEventListener Callbacks ---

    override fun onMapReady() {
        _uiState.update { it.copy(isMapReady = true, isTileLoading = false, tileError = null) }
        val loc = _uiState.value.userLocation
        val targetLat = loc?.latitude ?: _uiState.value.centerLocation.latitude
        val targetLng = loc?.longitude ?: _uiState.value.centerLocation.longitude

        if (loc != null) {
            mapController?.setUserLocation(
                lat = loc.latitude,
                lng = loc.longitude,
                accuracy = loc.accuracyMeters,
                heading = loc.bearingDegrees,
                colorHex = _uiState.value.territoryColorHex
            )
            mapController?.setCenter(loc.latitude, loc.longitude, animated = false)
        } else {
            mapController?.setCenter(MapConfig.DEFAULT_LAT, MapConfig.DEFAULT_LNG, animated = false)
        }

        if (_uiState.value.isDevTerritoryOverlayActive) {
            val sampleSectors = if (_uiState.value.devTerritories.isEmpty()) {
                generateDevTerritories(targetLat, targetLng)
            } else {
                _uiState.value.devTerritories
            }
            _uiState.update { it.copy(devTerritories = sampleSectors) }
            if (territoryRepository != null) {
                viewModelScope.launch {
                    territoryRepository.seedMockTerritories(sampleSectors)
                }
            } else {
                mapController?.renderTerritories(sampleSectors)
            }
        }
    }

    override fun onTerritoryClicked(territoryId: String) {
        val found = _uiState.value.devTerritories.find { it.id == territoryId }
        _uiState.update { it.copy(selectedDevTerritory = found, showTerritoryDetailsModal = true) }
        
        // Evaluate attack eligibility immediately
        if (found != null && battleRepository != null) {
            viewModelScope.launch {
                val currentAuth = authRepository.authState.value
                val attackerId = if (currentAuth is com.example.domain.model.AuthState.Authenticated) currentAuth.user.id else "operative_local"
                val result = battleRepository.validateAttackEligibility(
                    territoryId = found.id,
                    targetTerritoryOwnerId = "enemy_bot", // Mock enemy owner
                    targetTerritoryFaction = Faction.fromId(found.factionId),
                    attackerUserId = attackerId,
                    attackerFaction = _uiState.value.faction
                )
                _uiState.update { it.copy(attackEligibility = result) }
            }
        }
    }
    
    fun dismissTerritoryDetailsModal() {
        _uiState.update { 
            it.copy(
                showTerritoryDetailsModal = false,
                selectedDevTerritory = null,
                attackEligibility = null
            ) 
        }
    }
    
    fun initiateAttackOnSelectedTerritory() {
        val territory = _uiState.value.selectedDevTerritory ?: return
        val eligibility = _uiState.value.attackEligibility
        
        if (eligibility == null || !eligibility.isEligible) return
        
        viewModelScope.launch {
            val currentAuth = authRepository.authState.value
            val attackerId = if (currentAuth is com.example.domain.model.AuthState.Authenticated) currentAuth.user.id else "operative_local"
            val battle = battleRepository?.initiateBattle(
                territoryId = territory.id,
                territoryName = territory.name,
                territoryAreaSqMeters = territory.areaSqMeters,
                defenderUserId = "enemy_bot",
                defenderDisplayName = "RIVAL OPERATIVE",
                defenderFaction = Faction.fromId(territory.factionId),
                defenderColorHex = territory.colorHex,
                attackerUserId = attackerId,
                attackerDisplayName = _uiState.value.username,
                attackerFaction = _uiState.value.faction
            )
            
            if (battle != null) {
                _uiState.update { 
                    it.copy(
                        showTerritoryDetailsModal = false,
                        showAttackPreparationModal = true,
                        activeBattle = battle
                    ) 
                }
            }
        }
    }
    
    fun dismissAttackPreparationModal() {
        _uiState.update { it.copy(showAttackPreparationModal = false, activeBattle = null) }
    }
    
    fun startBattleRun() {
        val battle = _uiState.value.activeBattle ?: return
        _uiState.update { it.copy(showAttackPreparationModal = false) }
        // We start the session passing the battle ID so it's linked
        locationManager?.startRunSession(battle.battleId)
    }

    fun dismissBattleEvaluation() {
        _uiState.update { it.copy(battleEvaluation = null, activeBattle = null) }
        closeCompletedRunSummary() // Close the run summary as well, or just reset state
    }

    override fun onMapMoved(lat: Double, lng: Double) {
        val userLoc = _uiState.value.userLocation
        val isStillNearUser = if (userLoc != null) {
            val dLat = Math.abs(userLoc.latitude - lat)
            val dLng = Math.abs(userLoc.longitude - lng)
            dLat < 0.0005 && dLng < 0.0005
        } else false

        _uiState.update {
            it.copy(
                centerLocation = LatLng(lat, lng),
                isFollowingUser = isStillNearUser
            )
        }
    }

    override fun onMapZoomChanged(zoom: Int) {
        _uiState.update { it.copy(currentZoom = zoom) }
    }

    override fun onTileLoading() {
        _uiState.update { it.copy(isTileLoading = true) }
    }

    override fun onTileLoaded() {
        _uiState.update { it.copy(isTileLoading = false, tileError = null) }
    }

    override fun onTileError(errorMessage: String?) {
        _uiState.update {
            it.copy(
                isTileLoading = false,
                tileError = errorMessage ?: "Unable to fetch radar map tiles. Tap to retry."
            )
        }
    }

    private fun generateDevTerritories(centerLat: Double, centerLng: Double): List<DevTerritory> {
        return listOf(
            DevTerritory(
                id = "sec-cipher-01",
                name = "ALPHA NEXUS // CIPHER",
                factionId = Faction.CIPHER.id,
                colorHex = "#00F0FF",
                coordinates = listOf(
                    LatLng(centerLat + 0.0018, centerLng - 0.0035),
                    LatLng(centerLat + 0.0042, centerLng - 0.0012),
                    LatLng(centerLat + 0.0031, centerLng + 0.0018),
                    LatLng(centerLat + 0.0008, centerLng - 0.0005)
                ),
                areaSqMeters = 48500.0,
                defenseLevel = 95
            ),
            DevTerritory(
                id = "sec-apex-02",
                name = "BRAVO REDOUBT // APEX",
                factionId = Faction.APEX.id,
                colorHex = "#FF3B30",
                coordinates = listOf(
                    LatLng(centerLat + 0.0008, centerLng + 0.0015),
                    LatLng(centerLat + 0.0028, centerLng + 0.0048),
                    LatLng(centerLat - 0.0015, centerLng + 0.0052),
                    LatLng(centerLat - 0.0012, centerLng + 0.0018)
                ),
                areaSqMeters = 62100.0,
                defenseLevel = 88
            ),
            DevTerritory(
                id = "sec-solaris-03",
                name = "SOLAR OUTPOST // CONCORD",
                factionId = Faction.SOLARIS.id,
                colorHex = "#FF9500",
                coordinates = listOf(
                    LatLng(centerLat - 0.0018, centerLng - 0.0015),
                    LatLng(centerLat - 0.0015, centerLng + 0.0015),
                    LatLng(centerLat - 0.0045, centerLng + 0.0022),
                    LatLng(centerLat - 0.0042, centerLng - 0.0028)
                ),
                areaSqMeters = 41200.0,
                defenseLevel = 100
            ),
            DevTerritory(
                id = "sec-forge-04",
                name = "FORGE ENCLAVE // SYNDICATE",
                factionId = Faction.CIPHER.id,
                colorHex = "#CCFF00",
                coordinates = listOf(
                    LatLng(centerLat - 0.0005, centerLng - 0.0052),
                    LatLng(centerLat + 0.0015, centerLng - 0.0042),
                    LatLng(centerLat + 0.0005, centerLng - 0.0015),
                    LatLng(centerLat - 0.0025, centerLng - 0.0028)
                ),
                areaSqMeters = 33800.0,
                defenseLevel = 75
            ),
            DevTerritory(
                id = "sec-delta-05",
                name = "DELTA CITADEL // CONTESTED",
                factionId = Faction.APEX.id,
                colorHex = "#AF52DE",
                coordinates = listOf(
                    LatLng(centerLat + 0.0035, centerLng - 0.0058),
                    LatLng(centerLat + 0.0062, centerLng - 0.0038),
                    LatLng(centerLat + 0.0052, centerLng - 0.0005),
                    LatLng(centerLat + 0.0028, centerLng - 0.0025)
                ),
                areaSqMeters = 51900.0,
                defenseLevel = 45
            ),
            DevTerritory(
                id = "sec-omega-06",
                name = "OMEGA HARVESTER // SOLARIS",
                factionId = Faction.SOLARIS.id,
                colorHex = "#FF2D55",
                coordinates = listOf(
                    LatLng(centerLat - 0.0025, centerLng + 0.0032),
                    LatLng(centerLat - 0.0020, centerLng + 0.0065),
                    LatLng(centerLat - 0.0052, centerLng + 0.0058),
                    LatLng(centerLat - 0.0048, centerLng + 0.0025)
                ),
                areaSqMeters = 28400.0,
                defenseLevel = 80
            )
        )
    }

    fun dismissAcquisitionOverlay() {
        hasCalibratedInitialLocation = true
        _uiState.update {
            it.copy(
                showAcquisitionOverlay = false,
                hasCalibratedLocation = true
            )
        }
    }

    companion object {
        var hasCalibratedInitialLocation: Boolean = false
    }

    class Factory(
        private val locationClient: LocationClient,
        private val authRepository: AuthRepository,
        private val customizationRepository: CustomizationRepository,
        private val competitiveRepository: com.example.domain.repository.CompetitiveRepository? = null,
        private val notificationRepository: com.example.domain.repository.NotificationRepository? = null,
        private val locationManager: LocationManager? = null,
        private val permissionManager: LocationPermissionManager? = null,
        private val territoryRepository: com.example.domain.repository.TerritoryRepository? = null,
        private val battleRepository: com.example.domain.repository.BattleRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorldMapViewModel(
                locationClient = locationClient,
                authRepository = authRepository,
                customizationRepository = customizationRepository,
                competitiveRepository = competitiveRepository,
                notificationRepository = notificationRepository,
                locationManager = locationManager,
                permissionManager = permissionManager,
                territoryRepository = territoryRepository,
                battleRepository = battleRepository
            ) as T
        }
    }
}
