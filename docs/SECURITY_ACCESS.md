# Run2Capture — Security & Access Control Document

## 1. Anti-Cheat & GPS Telemetry Integrity

Because Run2Capture is a multiplayer territory control game where real-world physical exertion grants strategic in-game territory, maintaining GPS telemetry integrity is critical.

### 1.1 Multi-Tier Anti-Spoofing Architecture

```
                                [ GPS Coordinate Ingestion ]
                                              │
                                              ▼
                                 [ 1. System Mock Check ]
                                 • Location.isFromMockProvider (API <31)
                                 • Location.isMock (API 31+)
                                              │ Pass
                                              ▼
                                [ 2. Kinematic Gating ]
                                • Max Speed Threshold: v ≤ 8.33 m/s (30 km/h)
                                • Max Acceleration: a ≤ 4.0 m/s²
                                • Max Instantaneous Jump: Δd ≤ 50 m in 2s
                                              │ Pass
                                              ▼
                               [ 3. Signal Quality Filtering ]
                               • Horizontal Accuracy: Acc ≤ 20.0 m
                               • Elapsed Realtime Drift Check
                                              │ Pass
                                              ▼
                              [ Accept Valid Run Coordinate ]
```

### 1.2 Telemetry Violation Actions
- **Level 1 (Transient Jitter / Poor Accuracy)**: Coordinate discarded; polyline paused until accuracy improves below 20m. No penalty applied.
- **Level 2 (Speed / Teleportation Anomaly)**: Coordinate flagged as invalid. If sustained > 5 seconds, the current active run capture session is invalidated and territory claiming is locked for the session.
- **Level 3 (Mock Location Flagged)**: Run session instantly aborted. User receives explicit warning: *"Mock location detected. Physical outdoor movement required."*

---

## 2. Android Runtime Permissions & Access Control

### 2.1 Required Android Permissions

| Permission | Manifest Level | Rationale & UX Flow |
| :--- | :--- | :--- |
| `ACCESS_FINE_LOCATION` | Dangerous / Runtime | Required for high-accuracy GPS tracking during active run sessions. Requested on onboarding or first run start. |
| `ACCESS_COARSE_LOCATION` | Dangerous / Runtime | Fallback for approximate location when GPS is acquiring satellite lock. |
| `ACCESS_BACKGROUND_LOCATION` | Dangerous / Runtime | Required for continuous tracking when user locks the screen during an active run. Requested progressively with detailed in-app justification. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION` | Normal / Runtime (API 34+) | Required to run the active GPS tracking service with a persistent notification. |
| `POST_NOTIFICATIONS` | Dangerous / Runtime (API 33+) | Displays real-time run stats (distance, pace, active territory) in the system notification drawer. |
| `INTERNET` | Normal | Required for OpenStreetMap tile fetching and multiplayer territory synchronization. |
| `ACCESS_NETWORK_STATE` | Normal | Required for detecting offline/online transitions for the sync queue. |

### 2.2 Progressive Permission Flow
1. **Initial Launch**: App displays map and general region using coarse location or default overview.
2. **"Start Run" Action**: Triggers in-app modal explaining why Precise Location is needed for territory capture.
3. **Background Run Request**: If user begins a run and minimizes the app, a distinct explainer prompt clarifies that background location is needed to record continuous route loops with the screen off.

---

## 3. Authentication & Cloud Security Policies

### 3.1 Authentication Strategy
- **Primary Auth**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) integrated with Firebase Auth.
- **Session Tokens**: Handled automatically via Firebase Auth SDK with rotating OAuth2 tokens.

### 3.2 Cloud Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    function isValidTerritory(data) {
      return data.areaSqMeters >= 500 
          && data.areaSqMeters <= 5000000
          && data.ownerUserId == request.auth.uid
          && data.capturedAt == request.time;
    }

    // User Profiles
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow create, update: if isOwner(userId);
      allow delete: if false; // Soft deletes only
    }

    // Territories
    match /territories/{territoryId} {
      allow read: if isAuthenticated();
      // Only authenticated owner can write newly claimed territory
      allow create: if isAuthenticated() && isValidTerritory(request.resource.data);
      // Updates allowed for defense decay or conquest resolution
      allow update: if isAuthenticated();
      allow delete: if false;
    }

    // Game Sessions & Logs
    match /sessions/{sessionId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update: if isOwner(resource.data.userId);
    }
  }
}
```

---

## 4. Local Data Protection & WebView Sandbox

### 4.1 Local Room Encryption & Integrity
- All location breadcrumbs and run logs are stored in the app-private SQLite database sandbox (`/data/data/com.aistudio.run2capture.app/databases/`).
- Integrity validation hash stored alongside completed run sessions to prevent tampering with local records prior to cloud sync.

### 4.2 WebView Security Constraints (Leaflet)
- `WebView` configured with:
  - `setAllowFileAccess(false)`
  - `setAllowContentAccess(false)`
  - `setGeolocationEnabled(false)` (Leaflet receives GPS coordinates purely via Android Native Bridge `evaluateJavascript`, never through arbitrary web APIs).
  - Strict Content Security Policy (CSP) headers on the local HTML container.
  - JavaScript bridge interface strictly restricted to public methods annotated with `@JavascriptInterface`.
