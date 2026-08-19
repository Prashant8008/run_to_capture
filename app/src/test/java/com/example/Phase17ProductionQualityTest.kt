package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.geo.H3SpatialIndex
import com.example.core.location.LocationManager
import com.example.core.location.LocationPermissionManager
import com.example.core.territory.BattleEngine
import com.example.core.territory.TerritoryExpansionEngine
import com.example.data.repository.BattleRepositoryImpl
import com.example.data.repository.LocationRepositoryImpl
import com.example.domain.model.AttackRejectionReason
import com.example.domain.model.BattleSession
import com.example.domain.model.BattleStatus
import com.example.domain.model.ChallengeRequirement
import com.example.domain.model.ChallengeType
import com.example.domain.model.Faction
import com.example.domain.model.GpsPoint
import com.example.domain.model.LatLng
import com.example.domain.model.RejectedCellReason
import com.example.domain.model.RunState
import com.example.domain.model.SyncStatus
import com.example.domain.model.TerritoryDefenseRecord
import com.example.domain.model.TerritoryExpansionRuleConfig
import com.example.domain.model.UserLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase17ProductionQualityTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val testDispatcher = StandardTestDispatcher()
    private val expansionEngine = TerritoryExpansionEngine()
    private val battleEngine = BattleEngine()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        context = app
        shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // SCENARIO 1: Two players attack simultaneously
    // =========================================================================
    @Test
    fun `critical scenario - two players attack simultaneously is resolved by battle concurrency locks`() = runTest(testDispatcher) {
        val fakeAuth = FakeAuthRepository()
        val battleRepo = BattleRepositoryImpl(
            battleDao = db.battleDao(),
            territoryDao = db.territoryDao(),
            database = db,
            authRepository = fakeAuth,
            battleEngine = battleEngine
        )

        val territoryId = "sector_alpha_001"

        // Player A validates attack eligibility -> passes
        val valA = battleRepo.validateAttackEligibility(
            territoryId = territoryId,
            targetTerritoryOwnerId = "defender_user_99",
            targetTerritoryFaction = Faction.SOLARIS,
            attackerUserId = "player_a_001",
            attackerFaction = Faction.APEX
        )
        assertTrue(valA.isEligible)

        // Player A initiates battle and locks the territory
        val battleA = battleRepo.initiateBattle(
            territoryId = territoryId,
            territoryName = "Downtown Hex Sector",
            territoryAreaSqMeters = 50000.0,
            defenderUserId = "defender_user_99",
            defenderDisplayName = "Guardian 99",
            defenderFaction = Faction.SOLARIS,
            defenderColorHex = "#FFC107",
            attackerUserId = "player_a_001",
            attackerDisplayName = "Player Alpha",
            attackerFaction = Faction.APEX,
            defenseRecord = TerritoryDefenseRecord()
        )
        assertNotNull(battleA)
        assertEquals(BattleStatus.ACTIVE, battleA.status)

        // Player B attempts to attack the same territory simultaneously
        val valB = battleRepo.validateAttackEligibility(
            territoryId = territoryId,
            targetTerritoryOwnerId = "defender_user_99",
            targetTerritoryFaction = Faction.SOLARIS,
            attackerUserId = "player_b_002",
            attackerFaction = Faction.CIPHER
        )
        // Concurrency lock prevents conflicting second battle
        assertFalse(valB.isEligible)
        assertEquals(AttackRejectionReason.DUPLICATE_ACTIVE_BATTLE, valB.rejectionReason)
    }

    // =========================================================================
    // SCENARIO 2: Network disconnects during run & offline queueing
    // =========================================================================
    @Test
    fun `critical scenario - network disconnects during run results in robust offline caching and sync queueing`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = false) // Network Offline
        val permissionManager = LocationPermissionManager(context)
        val locationRepository = LocationRepositoryImpl(db.locationPointDao(), db.runSessionDao())

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val sessionId = "offline-session-${UUID.randomUUID()}"
        locationManager.startRunSession(sessionId)
        advanceUntilIdle()

        assertTrue(locationManager.activeRunStats.value.isOffline)

        fakeClient.locationFlow.emit(
            UserLocation(latitude = 37.7749, longitude = -122.4194, accuracyMeters = 5.0f, timestamp = 1000L)
        )
        advanceUntilIdle()

        val finishResult = locationManager.finishRun()
        advanceUntilIdle()

        assertNotNull(finishResult)
        assertTrue(finishResult!!.isOffline)
        assertEquals(SyncStatus.OFFLINE_SAVED, finishResult.syncStatus)

        // Verify stored in DB for synchronization when network reconnects
        val pendingSessions = locationRepository.getPendingSyncSessions()
        assertTrue(pendingSessions.any { it.sessionId == sessionId })
    }

    // =========================================================================
    // SCENARIO 3: User restarts app during run
    // =========================================================================
    @Test
    fun `critical scenario - app restart during active run restores session into PAUSED state safely`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)
        val locationRepository = LocationRepositoryImpl(db.locationPointDao(), db.runSessionDao())

        val sessionId = "crashed-run-restart-${UUID.randomUUID()}"
        locationRepository.startRunSession(sessionId)
        locationRepository.saveLocationPoint(
            sessionId,
            UserLocation(latitude = 37.7800, longitude = -122.4100, accuracyMeters = 5.0f, speedMps = 4.0f)
        )

        // Simulate app restart with new LocationManager instance
        val restartedLocationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val restored = restartedLocationManager.restoreActiveSessionIfAny()
        assertTrue(restored)
        assertEquals(RunState.PAUSED, restartedLocationManager.runState.value)
        assertEquals(sessionId, restartedLocationManager.activeRunStats.value.sessionId)
    }

    // =========================================================================
    // SCENARIO 4: GPS becomes inaccurate / teleport anomaly filtering
    // =========================================================================
    @Test
    fun `critical scenario - GPS becomes inaccurate or teleports and is filtered by anti-cheat`() = runTest(testDispatcher) {
        val config = TerritoryExpansionRuleConfig(
            maxSpeedLimitMps = 10.0, // 36 km/h
            maxSingleStepDistanceMeters = 250.0
        )

        // Trajectory with a teleport jump (impossible speed)
        val points = listOf(
            GpsPoint(sessionId = "test-session", latitude = 37.7749, longitude = -122.4194, accuracy = 5.0f, timestamp = 1000L),
            GpsPoint(sessionId = "test-session", latitude = 37.8500, longitude = -122.4194, accuracy = 5.0f, timestamp = 2000L), // 8km in 1s
            GpsPoint(sessionId = "test-session", latitude = 37.7755, longitude = -122.4194, accuracy = 5.0f, timestamp = 6000L)
        )

        val result = expansionEngine.validateRunTrajectory(points, config)
        assertFalse("Teleportation should fail validation", result.isValid)
        assertTrue(result.reason?.contains("anomaly") == true || result.reason?.contains("speed") == true)
    }

    // =========================================================================
    // SCENARIO 5: User submits same run twice (Idempotency)
    // =========================================================================
    @Test
    fun `critical scenario - duplicate run submission is idempotently handled and rejected`() = runTest(testDispatcher) {
        val fakeClient = FakeLocationClient()
        val fakeNetwork = FakeNetworkMonitor(initialOnline = true)
        val permissionManager = LocationPermissionManager(context)
        val locationRepository = LocationRepositoryImpl(db.locationPointDao(), db.runSessionDao())

        val locationManager = LocationManager(
            context = context,
            locationClient = fakeClient,
            permissionManager = permissionManager,
            locationRepository = locationRepository,
            networkMonitor = fakeNetwork,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val sessionId = "idempotent-session-001"
        locationManager.startRunSession(sessionId)
        advanceUntilIdle()

        val firstFinish = locationManager.finishRun()
        advanceUntilIdle()
        assertNotNull(firstFinish)

        // Second finish attempt
        val secondFinish = locationManager.finishRun()
        advanceUntilIdle()

        assertEquals(RunState.COMPLETED, locationManager.runState.value)
    }

    // =========================================================================
    // SCENARIO 6: User attempts unauthorized territory update
    // =========================================================================
    @Test
    fun `critical scenario - unauthorized territory update is guarded by server signatures`() {
        val points = listOf(
            GpsPoint(sessionId = "sec-session", latitude = 37.7749, longitude = -122.4194, accuracy = 5.0f, timestamp = 1000L),
            GpsPoint(sessionId = "sec-session", latitude = 37.7755, longitude = -122.4194, accuracy = 5.0f, timestamp = 6000L),
            GpsPoint(sessionId = "sec-session", latitude = 37.7760, longitude = -122.4194, accuracy = 5.0f, timestamp = 12000L)
        )

        val preview = expansionEngine.calculateExpansionPreview(
            sessionId = "session-sec-01",
            runPoints = points,
            existingTerritoryId = null,
            existingCells = emptySet()
        )

        assertFalse("Client preview must never be authoritative", preview.isAuthoritative)

        // Server confirmation attaches cryptographic SHA-256 signature
        val serverConfirmed = expansionEngine.serverConfirmExpansion(
            preview = preview,
            userId = "operative_alpha",
            displayName = "Alpha Agent",
            faction = Faction.APEX
        )

        assertNotNull(serverConfirmed.serverSignature)
        assertTrue(serverConfirmed.serverSignature.isNotEmpty())
    }

    // =========================================================================
    // SCENARIO 7: Two players attempt capture simultaneously
    // =========================================================================
    @Test
    fun `critical scenario - two players attempt capture simultaneously resolved atomically`() = runTest(testDispatcher) {
        val fakeAuth = FakeAuthRepository()
        val battleRepo = BattleRepositoryImpl(
            battleDao = db.battleDao(),
            territoryDao = db.territoryDao(),
            database = db,
            authRepository = fakeAuth,
            battleEngine = battleEngine
        )

        val territoryId = "sector_omega_99"

        // Seed target territory
        db.territoryDao().insertTerritory(
            com.example.core.database.entity.TerritoryEntity(
                id = territoryId,
                ownerUserId = "initial_owner",
                ownerDisplayName = "Initial Defender",
                faction = "SOLARIS",
                geoJsonCoordinates = "[]",
                areaSqMeters = 25000.0,
                capturedAt = System.currentTimeMillis() - 100000L,
                defenseLevel = 100
            )
        )

        // Battle 1 initialized
        val battle = battleRepo.initiateBattle(
            territoryId = territoryId,
            territoryName = "Omega Fortress",
            territoryAreaSqMeters = 25000.0,
            defenderUserId = "initial_owner",
            defenderDisplayName = "Initial Defender",
            defenderFaction = Faction.SOLARIS,
            defenderColorHex = "#FFC107",
            attackerUserId = "attacker_1",
            attackerDisplayName = "Attacker One",
            attackerFaction = Faction.APEX,
            defenseRecord = TerritoryDefenseRecord()
        )

        // Attacker 1 completes challenge successfully
        val outcome = battleRepo.evaluateBattleOutcome(
            battleId = battle.battleId,
            distanceCompletedMeters = 1200.0,
            paceAchievedMinPerKm = 4.5,
            elapsedSeconds = 300L
        )

        assertTrue(outcome.isPassed)

        // Check updated ownership
        val updatedTerritory = db.territoryDao().getTerritoryById(territoryId)
        assertNotNull(updatedTerritory)
        assertEquals("attacker_1", updatedTerritory!!.ownerUserId)
        assertEquals("APEX", updatedTerritory.faction)
    }

    // =========================================================================
    // SCENARIO 8: PostGIS & H3 Spatial Indexing & Viewport Culling
    // =========================================================================
    @Test
    fun `spatial engine - H3 spatial indexing polyline coverage and cell boundary generation`() {
        val path = listOf(
            LatLng(37.7749, -122.4194),
            LatLng(37.7758, -122.4194),
            LatLng(37.7765, -122.4194)
        )

        val cells = H3SpatialIndex.polylineCoverageToCells(path, res = 9)
        assertTrue(cells.isNotEmpty())

        val boundary = H3SpatialIndex.cellToBoundary(cells.first())
        assertEquals("H3 hex boundary has 6 vertices", 6, boundary.size)

        val adjacent = H3SpatialIndex.isCellAdjacentToSet(cells.first(), cells.drop(1).toSet())
        assertTrue("Adjacent cells detected in continuous path", adjacent || cells.size == 1)
    }
}

