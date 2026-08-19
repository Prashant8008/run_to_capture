# Run2Capture — Feature Ticket List

## Epic 1: Project Foundation, Permissions & Map Architecture

### [T-001] Dependency & Manifest Permissions Configuration
- **Priority**: P0 (Critical)
- **Description**: Configure Gradle dependencies (`play-services-location`, `androidx-navigation-compose`, `androidx-room`, `firebase-auth`, `credentials`) and update `AndroidManifest.xml` with permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`, `INTERNET`).
- **Dependencies**: None
- **Acceptance Criteria**:
  1. Build succeeds with zero missing dependency errors.
  2. AndroidManifest declares all location, service, and network permissions.
  3. Dynamic Compose permission request handler implemented.
- **Verification**: `compile_applet` succeeds; Robolectric test validates permission state transitions.

### [T-002] Leaflet WebView Map Engine & JavaScript Bridge
- **Priority**: P0 (Critical)
- **Description**: Create local `leaflet_map.html` asset containing Leaflet 1.9.4 and OpenStreetMap tile configuration. Implement `LeafletMapView` composable with hardware acceleration and bidirectional `@JavascriptInterface` bridge.
- **Dependencies**: T-001
- **Acceptance Criteria**:
  1. WebView loads `leaflet_map.html` cleanly with fallback offline tiles/background.
  2. Android can invoke `setPlayerLocation`, `appendPathCoordinate`, and `setTerritories`.
  3. JS bridge emits `onMapInitialized` and `onTerritoryTapped` events back to Kotlin.
- **Verification**: Map initializes in Composable preview and responds to mock coordinate bridge calls.

---

## Epic 2: Location Tracking, Telemetry & Anti-Cheat

### [T-003] Foreground Location Service & High-Accuracy GPS
- **Priority**: P0 (Critical)
- **Description**: Implement `RunTrackingService` running as an Android `ForegroundService` with notification channel (`POST_NOTIFICATIONS`), managing `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`.
- **Dependencies**: T-001
- **Acceptance Criteria**:
  1. Service starts with a sticky notification displaying live run distance and pace.
  2. Location updates stream via Kotlin `StateFlow<LocationData>`.
  3. Service handles pause, resume, and stop commands cleanly without memory leaks.
- **Verification**: Unit test verifies Service lifecycle intents and LocationCallback flow.

### [T-004] Anti-Cheat, Speed Gating & Telemetry Sanitization
- **Priority**: P0 (Critical)
- **Description**: Implement `ValidateGpsPurityUseCase` to filter out inaccurate coordinates ($Acc > 20\text{m}$), mock locations (`isFromMockProvider` / `isMock`), and unnatural velocities ($v > 30\text{ km/h}$).
- **Dependencies**: T-003
- **Acceptance Criteria**:
  1. Mock coordinates are flagged and rejected immediately.
  2. Jumps $> 50\text{m}$ in $\le 2\text{s}$ are suppressed.
  3. Clean coordinates pass to the active run coordinate buffer.
- **Verification**: Unit tests feed simulated jitter, mock coordinates, and vehicle speeds to verify rejection.

---

## Epic 3: Geometrical Capture Engine & Local Persistence

### [T-005] Geometric Loop Closure & Area Calculation Engine
- **Priority**: P0 (Critical)
- **Description**: Implement `DetectLoopClosureUseCase` and `CalculatePolygonAreaUseCase` using geodesic Spherical Shoelace formula and 2D line segment intersection algorithms.
- **Dependencies**: T-004
- **Acceptance Criteria**:
  1. Detects loop closures when running trail returns within $\le 15\text{m}$ of an earlier path segment.
  2. Accurately calculates polygon area in $m^2$ and $km^2$ on the WGS84 sphere.
  3. Discards invalid polygons ($< 500\text{ m}^2$ or complex self-intersecting figures).
- **Verification**: Unit tests with known GPS loop datasets (e.g., standard 400m track loop = ~10,000 m²) verify calculated area within 2% margin of error.

### [T-006] Local Room Persistence for Sessions & Territories
- **Priority**: P1 (High)
- **Description**: Implement `AppDatabase` with `LocationPointEntity`, `TerritoryEntity`, `RunSessionEntity`, and `SyncQueueEntity` along with DAOs and Repository implementation.
- **Dependencies**: T-001, T-005
- **Acceptance Criteria**:
  1. Breadcrumbs, completed runs, and captured polygons persist offline atomically.
  2. Reactive DAOs provide `Flow<List<TerritoryEntity>>` for map rendering.
- **Verification**: Robolectric in-memory Room database tests for insert, query, and bounding-box queries.

---

## Epic 4: Cloud Sync & Multiplayer Factions

### [T-007] Authentication & Profile Management
- **Priority**: P1 (High)
- **Description**: Implement Google Sign-In via `CredentialManager` and Firebase Auth repository, allowing users to choose a Faction (*Apex*, *Solaris*, *Cipher*).
- **Dependencies**: T-001
- **Acceptance Criteria**:
  1. User can sign in, select a faction, and update runner profile name.
  2. Auth state streams through `AuthRepository`.
- **Verification**: Unit tests verify authentication state transitions and credential handling.

### [T-008] Cloud Territory Synchronization & Conflict Resolution
- **Priority**: P1 (High)
- **Description**: Implement `TerritoryRepository` sync logic with Firestore / REST backend, synchronizing newly claimed territories and resolving overlapping boundaries.
- **Dependencies**: T-006, T-007
- **Acceptance Criteria**:
  1. Newly captured territories sync to cloud database when online.
  2. Offline captures are queued in `SyncQueueEntity` and dispatched upon network availability.
  3. Neighboring and contested territories are fetched and rendered on map.
- **Verification**: Unit test validates sync queue dispatch on network reconnect.

---

## Epic 5: User Interface & Experience

### [T-009] Live Map HUD Screen & Run Controls
- **Priority**: P0 (Critical)
- **Description**: Build `MapScreen.kt` featuring full-screen `LeafletMapView`, top signal & faction indicators, bottom telemetry HUD (distance, pace, time, loop status), and floating action buttons.
- **Dependencies**: T-002, T-003, T-005
- **Acceptance Criteria**:
  1. HUD updates in real-time as runner moves.
  2. Loop closure status alerts user visually when a capture polygon is formed.
  3. Start / Pause / Finish controls transition run states seamlessly.
- **Verification**: Roborazzi screenshot test & UI unit tests.

### [T-010] Run Summary & Conquest Claim Screen
- **Priority**: P1 (High)
- **Description**: Build `RunSummaryScreen.kt` presenting run map thumbnail, total territory claimed ($m^2$), pace, distance, and claim submission CTA.
- **Dependencies**: T-005, T-006
- **Acceptance Criteria**:
  1. Displays clear breakdown of route, polygon area, and faction points earned.
  2. Saving run persists to Room and triggers cloud sync.
- **Verification**: Roborazzi screenshot test.

### [T-011] Leaderboard, Factions & Profile Screens
- **Priority**: P2 (Medium)
- **Description**: Build `LeaderboardScreen.kt`, `ProfileScreen.kt`, and `HistoryScreen.kt` with faction dominance bars, career stats, and run logs.
- **Dependencies**: T-006, T-008
- **Acceptance Criteria**:
  1. Faction control bar shows real-time percentage breakdown.
  2. Profile screen displays career metrics and earned achievements.
  3. History screen lists all past sessions with status indicators.
- **Verification**: UI unit tests and Compose preview verification.
