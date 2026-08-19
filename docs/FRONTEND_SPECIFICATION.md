# Run2Capture — Frontend Specification Document

## 1. Design System & Theme

Run2Capture is built with **Material Design 3 (M3)** using Jetpack Compose, infused with a vibrant tactical runner aesthetic: high-contrast dark and light palettes, crisp typography, and luminous faction neon accents.

### 1.1 Color Palette & Sleek Interface Tokens

| Token | Light Theme | Dark Theme | Purpose / Usage |
| :--- | :--- | :--- | :--- |
| `primary` | `#0061A4` (Executive Blue) | `#4FD8EB` (Neon Cyan) | Primary actions, user location marker, active trail |
| `primaryContainer` | `#D1E4FF` (Ice Blue) | `#004C6D` | Metric badges, active navigation pill |
| `secondary` | `#535F70` | `#B1CBD0` | Secondary buttons, inactive status |
| `secondaryContainer` | `#F2F0F4` | `#3F484B` | Audit status badge, subtle pill containers |
| `tertiary` | `#001D36` (Deep Navy) | `#DCBCE0` | Dark HUD panels, high-contrast badges |
| `background` | `#FDFBFF` (Crisp Off-White) | `#0E1416` (Deep Midnight) | Main screen canvas |
| `surface` | `#FFFFFF` | `#151E22` | Cards, dialogs, bottom sheets |
| `surfaceVariant` | `#E1E2E8` | `#3F484B` | Viewport container background, subtle borders |
| `outline` | `#E1E2E8` | `#44474E` | 1dp crisp card and badge borders |
| `pulse / alert` | `#FFB4AB` (Coral Pulse) | `#FFB4AB` | Glowing real-time status indicator dot |

---

## 2. Typography & Layout Hierarchy

- **Headline Large / Medium**: Displaying primary statistics (Distance: `4.25 km`, Area: `12,450 m²`).
- **Title Medium**: Section headers, Faction standings, run titles.
- **Body Medium / Large**: Detailed descriptions, coordinates, timestamps.
- **Label Medium / Large**: HUD indicators, speed readouts (`5:12 /km`), GPS accuracy pills (`±3m`).
- **Touch Target Standard**: Minimum **48dp × 48dp** for all buttons, FABs, and interactive map overlays.
- **Grid Density**: Standard **8dp** spacing grid (`8dp`, `16dp`, `24dp`, `32dp`).

---

## 3. Navigation & Screen Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    NAVIGATION GRAPH                         │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
      [ Live Map HUD ]   [ Leaderboard ]  [ Profile ]
       (Main Screen)     (Factions/Rank)  (Stats/Badges)
              │               ▲
       (Finish Run)           │
              │               │
              ▼               │
    [ Run Summary Screen ] ───┘
```

---

## 4. Screen Specifications

### Screen 1: Live Capture Map Screen (`MapScreen.kt`)
- **Main Viewport**: Full-bleed `LeafletMapView` rendering OpenStreetMap tiles with custom dark/light tilesets.
- **Top HUD Overlay**:
  - Left: Faction Badge & Current Territorial Rank.
  - Center: GPS Signal Pill (Green = High Accuracy $\le 5\text{m}$, Amber = Medium, Red = No Lock / Jitter).
  - Right: Map Layer Toggle / Recenter Button.
- **Bottom Floating Telemetry HUD**:
  - Live Distance (km with 2 decimals).
  - Live Pace (min:sec /km) & Instantaneous Speed (km/h).
  - Live Elapsed Time (`00:24:18`).
  - Active Loop Detection Status Indicator (*"Seeking Loop Closure..."* / *"Loop Closed! +14,200 m²"*).
- **Floating Action Controls**:
  - **Start Run** (Large glowing FAB, testTag: `start_run_button`).
  - **Pause / Resume** (Secondary FAB, testTag: `pause_resume_button`).
  - **Finish / Claim** (Primary Action FAB, testTag: `finish_run_button`).

### Screen 2: Capture Summary / Run Detail Screen (`RunSummaryScreen.kt`)
- **Header**: Territory Conquest Banner with celebratory particle visual and captured territory count.
- **Map Thumbnail**: Interactive snapshot showing the completed run route and colored claimed polygons.
- **Metrics Grid**:
  - Total Area Claimed ($m^2$ or $km^2$).
  - Distance Traversed ($km$).
  - Average Pace ($min/km$) & Active Time.
  - Estimated Energy Burned (kcal).
- **CTA Actions**:
  - `Save & Synchronize Conquest` (`Button`, testTag: `save_conquest_button`).
  - `Discard Session` (`OutlinedButton`, testTag: `discard_session_button`).

### Screen 3: Leaderboards & Factions Screen (`LeaderboardScreen.kt`)
- **Faction Control Heatmap Bar**: Displays percentage of total city/zone territory controlled by *Apex*, *Solaris*, and *Cipher*.
- **Tabs**: `Global Area`, `Weekly Runners`, `My Faction`.
- **Ranked List Cards**:
  - Rank (#1, #2, #3 with gold/silver/bronze badges).
  - Runner Avatar, Username, Faction Flag.
  - Total Area Claimed ($km^2$) & Number of Active Sectors.

### Screen 4: Runner Profile & Badges Screen (`ProfileScreen.kt`)
- **User Header**: Display Name, Runner Level, Selected Faction with option to switch seasons.
- **Career Metrics Cards**: Total Lifetime Distance, Total Lifetime Area Conquered, Longest Run, Defended Zones.
- **Achievements Grid**:
  - *First Conquest* (Claim your first 1,000 m²).
  - *Loop Master* (Complete a closed loop > 5 km).
  - *Apex Predator / Solaris Guardian / Cipher Infiltrator* (Faction achievements).

### Screen 5: History & Activity Log Screen (`HistoryScreen.kt`)
- **Chronological Run List**:
  - Date & Time stamp.
  - Distance, Duration, Area Captured.
  - Sync Status Indicator (Synced cloud icon vs. Pending local icon).

---

## 5. Leaflet HTML/JS Integration Structure

- Asset Location: `app/src/main/assets/leaflet_map.html`
- Bundled Resources:
  - `leaflet.js` (1.9.4) & `leaflet.css`
  - Custom SVG Marker Icons (Player position with dynamic rotation arrow, territory center pin)
  - Tile Layer: OpenStreetMap (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`) with cartographic dark/light options.
- JavaScript API Exposed to Android:
  ```javascript
  window.LeafletBridge = {
    setPlayerLocation(lat, lng, heading, accuracy),
    appendPathCoordinate(lat, lng),
    clearActivePath(),
    setTerritories(geoJsonCollection),
    highlightCandidateLoop(coordinates),
    recenterCamera(lat, lng, zoom)
  };
  ```
- Android Bridge Handlers:
  ```kotlin
  class LeafletJsBridge(private val onEvent: (MapEvent) -> Unit) {
    @JavascriptInterface
    fun onMapInitialized() { ... }
    @JavascriptInterface
    fun onTerritoryTapped(id: String) { ... }
  }
  ```
