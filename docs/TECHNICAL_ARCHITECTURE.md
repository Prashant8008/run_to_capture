# Run2Capture — Technical Architecture Document

## 1. Architectural Overview & Design Pattern

Run2Capture employs **Clean Architecture** paired with **Model-View-ViewModel (MVVM)** and **Unidirectional Data Flow (UDF)** in modern Kotlin and Jetpack Compose.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│                                                                        │
│  ┌─────────────────────────┐            ┌───────────────────────────┐  │
│  │   Jetpack Compose UI    │            │   Leaflet Map WebView     │  │
│  │  (Screens, HUD, Cards)  │            │ (OSM Tiles, Canvas Layers)│  │
│  └───────────▲─────────────┘            └─────────────▲─────────────┘  │
│              │ State / Events                         │ Bridge / JS    │
│  ┌───────────▼────────────────────────────────────────▼─────────────┐  │
│  │                     ViewModels (StateFlow / UDF)                 │  │
│  └──────────────────────────────────▲───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼──────────────────────────────────┐
│                             DOMAIN LAYER                               │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │                            Use Cases                             │  │
│  │  • TrackLocationUseCase        • ClaimTerritoryUseCase          │  │
│  │  • DetectLoopClosureUseCase    • ValidateGpsPurityUseCase        │  │
│  │  • CalculatePolygonAreaUseCase • SyncTerritoriesUseCase          │  │
│  └──────────────────────────────────▲───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼──────────────────────────────────┐
│                              DATA LAYER                                │
│                                     │                                  │
│  ┌──────────────────────────────────▼───────────────────────────────┐  │
│  │                           Repositories                           │  │
│  │  • LocationRepository          • TerritoryRepository             │  │
│  │  • AuthRepository              • SessionRepository               │  │
│  └──────────▲───────────────────────▲────────────────────────▲──────┘  │
│             │                       │                        │         │
│  ┌──────────▼──────────┐ ┌──────────▼──────────┐ ┌───────────▼──────┐  │
│  │ Android FusedLocation│ │   Local Room DB    │ │ Cloud Firestore /│  │
│  │ + ForegroundService │ │ (DAOs & Entities)  │ │ Remote Backend   │  │
│  └─────────────────────┘ └────────────────────┘ └───────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Breakdown

### 2.1 Presentation Layer (Jetpack Compose & Leaflet Map)
- **Compose Navigation**: Type-safe destination navigation (`@Serializable` routes: `MapRoute`, `SessionSummaryRoute`, `LeaderboardRoute`, `ProfileRoute`, `HistoryRoute`).
- **Interactive Leaflet WebView (`LeafletMapView.kt`)**:
  - Embedded Android `WebView` configured with hardware acceleration, asset loading via `AndroidAssetLoader` / local HTML bundling, and a JavaScript Bridge (`@JavascriptInterface`).
  - **Bidirectional Bridge Interface**:
    - *Android -> JS*: Updates user marker position (`updateUserLocation(lat, lng, heading, accuracy)`), appends polyline points (`appendPathPoint(lat, lng)`), renders polygon zones (`renderTerritories(geoJsonString)`), sets map camera view (`setCamera(lat, lng, zoom)`).
    - *JS -> Android*: Emits map click events (`onTerritoryClicked(territoryId)`), user drag/zoom changes (`onMapBoundsChanged(minLat, maxLat, minLng, maxLng)`), and webview readiness (`onMapReady()`).

### 2.2 Domain Layer (Use Cases & Math Engine)
- **`DetectLoopClosureUseCase`**:
  - Monitors the continuous stream of coordinates $(p_1, p_2, \dots, p_n)$.
  - When the latest point $p_n$ comes within proximity threshold $R \le 15\text{m}$ to an earlier coordinate $p_k$ (where $n - k \ge 8$ to prevent immediate self-closure), a candidate loop $[p_k, p_{k+1}, \dots, p_n, p_k]$ is extracted.
  - Detects line segment intersections using 2D cross-product line sweep:
    $$\text{ccw}(A,B,C) = (C_y - A_y)(B_x - A_x) > (B_y - A_y)(C_x - A_x)$$
    Two line segments $AB$ and $CD$ intersect if $\text{ccw}(A,C,D) \neq \text{ccw}(B,C,D)$ and $\text{ccw}(A,B,C) \neq \text{ccw}(A,B,D)$.

- **`CalculatePolygonAreaUseCase` (Spherical Shoelace Algorithm)**:
  - Computes the geodesic area on the WGS84 authalic sphere ($R = 6,371,000\text{ m}$):
    $$\text{Area} = \frac{R^2}{2} \left| \sum_{i=1}^{m} (\lambda_{i+1} - \lambda_{i-1}) \cdot \sin(\phi_i) \right|$$
    where $\phi$ is latitude in radians, $\lambda$ is longitude in radians, and the vertices are ordered cyclically.

- **`ValidateGpsPurityUseCase`**:
  - Validates instantaneous speed between successive points $v = \frac{\Delta d}{\Delta t} \le 8.33\text{ m/s}$ (approx. $30\text{ km/h}$).
  - Verifies horizontal accuracy $H_{acc} \le 20.0\text{ m}$.
  - Verifies `Location.isFromMockProvider` is false.

### 2.3 Data Layer (Persistence & Networking)

#### Room Database Architecture
- **`AppDatabase`** (Version 1):
  - **`LocationPointEntity`**:
    - `id: Long` (Auto-increment PK)
    - `sessionId: String` (FK)
    - `latitude: Double`, `longitude: Double`
    - `altitude: Double`, `speed: Float`, `accuracy: Float`
    - `timestamp: Long`
  - **`TerritoryEntity`**:
    - `id: String` (PK)
    - `ownerUserId: String`
    - `ownerDisplayName: String`
    - `faction: String`
    - `geoJsonCoordinates: String` (JSON array of `[lat, lng]` coordinates)
    - `areaSqMeters: Double`
    - `capturedAt: Long`
    - `defenseLevel: Int`
    - `isSynced: Boolean`
  - **`RunSessionEntity`**:
    - `sessionId: String` (PK)
    - `startTime: Long`, `endTime: Long?`
    - `distanceMeters: Double`
    - `durationSeconds: Long`
    - `avgSpeedMps: Double`
    - `territoriesCapturedCount: Int`
    - `status: String` (`ACTIVE`, `PAUSED`, `COMPLETED`, `DISCARDED`)

#### Cloud Synchronization Architecture (Firestore & Remote API)
- Remote collections:
  - `/users/{userId}`: Profile, faction, lifetime stats.
  - `/territories/{territoryId}`: GeoJSON polygon, faction, owner, area, timestamp, bounding box geo-hash for spatial querying.
  - `/leaderboards/global`: Aggregated leaderboards by faction and player.

---

## 3. Technology Stack & Dependencies

| Category | Component / Library | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2+ | Modern type-safe, asynchronous development |
| **UI Toolkit** | Jetpack Compose (BOM 2024.09.00) | Declarative UI, Material Design 3 |
| **Mapping Engine** | Leaflet.js 1.9.4 + OpenStreetMap via WebView | Interactive map rendering, vector styling, offline tiles |
| **Location Services** | Google Play Services Location (`21.3.0`) | High-accuracy `FusedLocationProviderClient` |
| **Local Persistence** | AndroidX Room (`2.7.0`) + KSP | Local SQL persistence for sessions & territories |
| **Async & Concurrency** | Kotlin Coroutines & Flow (`1.10.2`) | Reactive streams & non-blocking computations |
| **Networking & JSON** | Retrofit 2.12.0 + Moshi 1.15.2 + OkHttp | REST API & spatial GeoJSON parsing |
| **Cloud & Auth** | Firebase Auth (`34.17.0`) + Google Credential Manager | Authentication & cloud state synchronization |
| **Testing** | Robolectric (`4.16.1`) + Roborazzi | JVM-based UI and logic testing without emulator |

---

## 4. Data Flow Sequence: Capture Lifecycle

```
[User Running]
     │
[LocationService] ───(LocationCallback)───> [LocationRepository]
                                                    │
                                                    ▼
                                          [DetectLoopClosureUseCase]
                                                    │ (Loop Detected!)
                                                    ▼
                                          [CalculatePolygonAreaUseCase]
                                                    │
                                                    ▼
                                          [ClaimTerritoryUseCase]
                                         /                      \
                                        ▼                        ▼
                               [Room Database]          [Cloud Firestore]
                                        │                        │
                                        ▼                        ▼
                                [Active Session]        [Leaflet Web Map]
                               (Update HUD State)      (Draw New Polygon)
```
