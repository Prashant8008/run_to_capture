# Run2Capture — Product Requirements Document (PRD)

## 1. Product Overview & Vision

**Run2Capture** is a competitive, real-time multiplayer territory-control mobile game for Android that turns physical running, walking, and jogging into a strategic geolocation conquest game.

Players physically traverse real-world environments (neighborhoods, parks, city blocks, running tracks). By recording contiguous GPS trails and creating closed geometric loops or sector traverses, players claim geographic territory polygons for their profile and aligned faction. Claimed territories appear live on an interactive global Leaflet/OpenStreetMap layer, allowing players to expand borders, capture contested zones from rival runners, defend sectors through maintenance runs, and climb local and global leaderboards.

---

## 2. Target Audience & Personas

1. **The Casual Fitness Runner (Alex, 26)**: Wants motivation to run daily routes, exploring new streets and parks to color the map and earn badges.
2. **The Competitive Territory Strategist (Maya, 31)**: Plans efficient closed-loop routes to encircle high-value sectors, challenge rival faction territories, and maintain leaderboard dominance.
3. **The Neighborhood Walker (Sam, 42)**: Takes evening walks to maintain defensive control over neighborhood green spaces and community parks.

---

## 3. Core Gameplay Loop

```
┌─────────────────────────────────────────────────────────────┐
│                      CORE GAME LOOP                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                 [ 1. Start Capture Session ]
                 • Verify High-Accuracy GPS
                 • Check Mock Provider & Anti-Cheat
                 • Initialize Live Route Canvas
                              │
                              ▼
                 [ 2. Physical Locomotion ]
                 • Real-time GPS Breadcrumb Collection
                 • Speed & Telemetry Validation (<25 km/h)
                 • Path Smoothing & Polyline Rendering
                              │
                              ▼
                 [ 3. Loop Closure Detection ]
                 • Real-time Ray-Casting & Intersection
                 • Identify Closed Polygon Boundary
                 • Validate Area & Complexity Constraints
                              │
                              ▼
                 [ 4. Territory Calculation & Claim ]
                 • Shoelace Geodesic Area Calculation (m²/km²)
                 • Resolve Overlaps with Existing Polygons
                 • Award Ownership & Faction Control Points
                              │
                              ▼
                 [ 5. Sync & Multiplayer Propagation ]
                 • Persist to Local Room Database
                 • Sync with Cloud Firestore / Remote Server
                 • Update Global Map & Live Leaderboards
```

---

## 4. Functional Requirements

### FR-1: Location & Telemetry Engine
- **FR-1.1**: Real-time GPS tracking via Android `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`.
- **FR-1.2**: Persistent background tracking via Android `ForegroundService` with notification channel and wakelock support.
- **FR-1.3**: GPS Accuracy Filtering: Exclude location samples with horizontal accuracy exceeding 20 meters.
- **FR-1.4**: Speed Gating & Telemetry: Real-time calculation of velocity, pacing (min/km), total distance (meters), elapsed time, and cadence.

### FR-2: Map & Spatial Visualization (Leaflet.js + Android Bridge)
- **FR-2.1**: Interactive hardware-accelerated WebView rendering OpenStreetMap vector/raster tiles via Leaflet.js 1.9+.
- **FR-2.2**: Real-time rendering of player location marker with heading bearing and accuracy radius circle.
- **FR-2.3**: Live active run polyline rendering with dynamic color coding based on current pace/faction.
- **FR-2.4**: GeoJSON Polygon rendering for captured territories with faction-specific fill colors, opacity, and borders.
- **FR-2.5**: Offline tile caching mechanism for standard zoom levels (13 to 18).

### FR-3: Geometric Capture & Validation Algorithm
- **FR-3.1**: **Loop Closure Detection**: Determine when the active GPS trail crosses or approaches a previous coordinate in the current session within a proximity threshold (e.g., 15 meters).
- **FR-3.2**: **Self-Intersection Filtering**: Detect and extract simple non-self-intersecting sub-polygons from complex multi-loop runs.
- **FR-3.3**: **Area Calculation**: Calculate precise geodesic polygon area on the WGS84 ellipsoid (using Spherical/Shoelace formula in square meters and square kilometers).
- **FR-3.4**: **Size Limits**: Minimum capture threshold (e.g., 500 m²) to prevent micro-spam; maximum threshold (e.g., 2,000,000 m² per single loop) to flag vehicle spoofing.

### FR-4: Multiplayer, Factions & Territory Mechanics
- **FR-4.1**: Three distinct player factions (e.g., *Apex Vanguard / Crimson*, *Solaris Collective / Amber*, *Cipher Syndicate / Cyan*).
- **FR-4.2**: Territory Ownership Model: Territory ID, Owner User ID, Faction ID, Bounding Polygon (GeoJSON), Area (m²), Timestamp, Defense Energy / Decay Level.
- **FR-4.3**: Overlap & Conquest: If a new captured polygon overlaps an existing rival polygon, the overlapping area is carved out or converted based on conquest rules.
- **FR-4.4**: Territory Decay & Maintenance: Territories lose defense energy over 7 days unless revisited by allied faction runners.

### FR-5: Authentication & User Profiles
- **FR-5.1**: Google Sign-In authentication via Jetpack `CredentialManager` and Firebase Auth.
- **FR-5.2**: User profile management (Display Name, Faction selection, Avatar, Level, Total Area Captured, Total Distance Ran).
- **FR-5.3**: Secure local credential state and token refresh.

### FR-6: Local Persistence & Offline Sync
- **FR-6.1**: Local Room database storing:
  - `LocationPointEntity` (Session breadcrumbs)
  - `TerritoryEntity` (Cached local and captured territories)
  - `RunSessionEntity` (Run summaries, duration, distance, pace, polygons)
  - `SyncQueueEntity` (Pending offline actions to sync upon reconnection)
- **FR-6.2**: Offline-first operational capability: runners can capture territory without cellular signal; captures sync once connectivity is restored.

### FR-7: Leaderboards & Social
- **FR-7.1**: Real-time Leaderboards:
  - Global / Local Area Controlled (km²)
  - Weekly Distance Covered (km)
  - Faction Territorial Dominance (% of map sectors controlled)
- **FR-7.2**: Activity Log & Territory Inspector: Tap any territory on the map to inspect owner, faction, capture date, and perimeter.

---

## 5. Non-Functional Requirements

- **Performance**: 60 FPS smooth map scrolling and UI animations; polygon rendering latency < 100ms for up to 500 active screen polygons.
- **Battery Efficiency**: Adaptive GPS polling (e.g., 2-3 seconds interval when moving, throttled when stationary); minimal CPU wake during screen-off foreground service.
- **Reliability & Data Integrity**: Zero loss of GPS breadcrumbs on unexpected app kill; atomic transactions in local Room database.
- **Security**: Strict anti-mock location detection, server-side polygon coordinate and speed verification.
- **Accessibility**: Support for high-contrast map themes, dynamic font scaling, and minimum 48dp touch targets.
