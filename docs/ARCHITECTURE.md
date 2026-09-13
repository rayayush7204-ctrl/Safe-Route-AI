# SafeRoute AI - System Architecture Documentation

## 1. Architectural Philosophy

SafeRoute AI is designed around **Clean Architecture** and **Unidirectional Data Flow (MVVM)** to ensure that critical personal safety workflows remain testable, robust, and decoupled from hardware sensors, device drivers, and third-party cloud SDKs.

```
       ┌─────────────────────────────────────────────────────────┐
       │                   Presentation Layer                    │
       │  (Jetpack Compose, Material 3, StateFlow, ViewModel)    │
       └────────────────────────────┬────────────────────────────┘
                                    │ consumes
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │                      Domain Layer                       │
       │    (Entities, Use Cases, Repositories Contracts)        │
       └────────────────────────────▲────────────────────────────┘
                                    │ implements
       ┌────────────────────────────┴────────────────────────────┐
       │                      Data Layer                         │
       │  (Room SQLite DB, DataStore Preferences, In-Memory)     │
       └─────────────────────────────────────────────────────────┘
                                    │
       ┌────────────────────────────┴────────────────────────────┐
       │                      Core Layer                         │
       │          (Result Monad, Utilities, Primitives)          │
       └─────────────────────────────────────────────────────────┘
```

---

## 2. Layer Definitions & Responsibilities

### Core Layer (`com.saferouteai.core`)
* Provides cross-cutting functional utilities and primitives.
* `Result<T>`: Explicit typed result wrapper eliminating unchecked runtime exceptions across domain boundaries.
* Zero dependency on Android UI frameworks or higher business layers.

### Domain Layer (`com.saferouteai.domain`)
* The heart of the application containing pure Kotlin business rules, entities, and use cases.
* **Entities**:
  * `JourneyState` & `Journey`: Journey state machine and lifecycle model.
  * `UserProfile`: Local identity representation (Display name, phone, email, timestamps).
  * `TrustedContact`: Emergency contact entity with validation rules (Name, relationship, phone/email, enabled flag).
  * `ContactRelationship`: Neutral relationship taxonomy (`Parent`, `Sibling`, `Partner`, `Friend`, `Other`).
  * `UserConsent`: Explicit consent model (`locationSharingConsent`, `trustedContactSharingConsent` both default strictly to `false`).
* **Use Cases**: Single-responsibility domain actions (`GetUserProfileUseCase`, `SaveUserProfileUseCase`, `GetTrustedContactsUseCase`, `AddTrustedContactUseCase`, `UpdateTrustedContactUseCase`, `RemoveTrustedContactUseCase`, `SetContactEnabledUseCase`, `GetConsentUseCase`, `UpdateConsentUseCase`).
* **Repository Contracts**: Abstract interfaces declaring operational boundaries (`JourneyRepository`, `UserProfileRepository`, `TrustedContactsRepository`, `ConsentRepository`).
* **Boundary Rules**: Zero Android framework dependencies (`android.*` imports are prohibited in the domain layer).

### Data Layer (`com.saferouteai.data`)
* Coordinates data sources and implements domain repository contracts.
* **Room Database (`SafeRouteDatabase`)**:
  * SQLite persistence via AndroidX Room `2.8.5` and KSP `2.0.21-1.0.28`.
  * `UserProfileEntity` & `UserProfileDao`: Local profile storage.
  * `TrustedContactEntity` & `TrustedContactDao`: Local trusted contacts storage with real-time `Flow` streaming.
* **Preferences DataStore (`ConsentDataStore`)**:
  * AndroidX DataStore Preferences `1.2.1` for lightweight, asynchronous, atomic user consent storage.
* **In-Memory Test Doubles**:
  * `InMemoryUserProfileRepository`, `InMemoryTrustedContactsRepository`, `InMemoryConsentRepository`, `InMemoryJourneyRepository` provide fast, hermetic, pure-JVM unit testing without needing instrumented Android environments.

### Presentation Layer (`com.saferouteai.presentation`)
* Renders user experience through declarative Jetpack Compose and Material 3 design tokens.
* **MVVM**:
  * `JourneyViewModel`: StateFlow-driven journey dashboard.
  * `ProfileViewModel`: Local profile editing with input validation feedback.
  * `TrustedContactsViewModel`: Contact list, toggle active state, add/edit dialog, delete confirmations.
  * `ConsentViewModel`: Explicit opt-in toggles for location and contact sharing.
* Composables are strictly stateless where possible, reacting to state emissions and dispatching user intents.
* **Navigation**: Jetpack Navigation Compose (`SafeRouteNavHost`, `Screen`) handling `Home`, `Settings`, `Profile`, `TrustedContacts`, and `PrivacyConsent` routes.

---

## 3. Journey State Progression

The domain state machine is designed to safely scale across milestones:

| State | Milestone | Description |
|---|---|---|
| `IDLE` | **M1 (Current)** | System at rest. Zero background work. |
| `POSSIBLE_JOURNEY` | Future | Motion/heuristic pre-detection before user confirmation. |
| `AWAITING_CONFIRMATION` | Future | User prompted to confirm journey start. |
| `ACTIVE` | **M1 (Current)** | Journey underway; safety services attached. |
| `WATCH` | Future | Elevated caution (deviations, stop delays). |
| `HIGH_RISK` | Future | High probability threat detected; countdown initiated. |
| `INCIDENT` | Future | SOS triggered, emergency contact dispatch. |
| `RESOLVED` | Future | False alarm or resolved distress. |
| `COMPLETED` | **M1 (Current)** | Journey safely finished and concluded. |

---

## 4. User Consent & Privacy Architecture (Milestone 2A)

SafeRoute AI enforces strict privacy guarantees at both the domain model and repository levels:

### Explicit Opt-In Only
* Both `locationSharingConsent` and `trustedContactSharingConsent` **strictly default to `false`**.
* **Zero Automatic Inferences**: Adding a contact, starting a journey, or saving a profile will **never** automatically toggle consent to `true`.
* **Instant Revocation**: Users can revoke consent at any time from the Privacy & Consent screen; changes take effect immediately on-device.

---

## 5. Foreground Location Architecture & Dual-Gate Enforcement (Milestone 2B)

Milestone 2B introduces real-time geolocation strictly scoped to the foreground lifecycle of an active Safe Journey.

```
       ┌─────────────────────────────────────────────────────────┐
       │             Start / Resume Tracking Intent              │
       └────────────────────────────┬────────────────────────────┘
                                    │
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │                 Dual-Gate Verification                  │
       │  Gate 1: LocationPermissionStatus.isGranted (Android)   │
       │  Gate 2: UserConsent.locationSharingConsent == true     │
       └────────────────────────────┬────────────────────────────┘
                                    │
                     ┌──────────────┴──────────────┐
                     │ Both Satisfied?             │
                     │                             │
                YES  ▼                             ▼  NO
       ┌────────────────────────┐      ┌─────────────────────────┐
       │ LocationTrackingState  │      │ LocationTrackingState   │
       │      .TRACKING         │      │ .PERMISSION_REQUIRED or │
       │ (Streams UserLocation) │      │   .CONSENT_REQUIRED     │
       └────────────────────────┘      └─────────────────────────┘
```

### Strict Invariants & Guarantees
1. **Dual-Gate Requirement**:
   * Android Runtime Permission (`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` requested together).
   * Explicit local user consent (`locationSharingConsent == true`).
   * **Both must be true** before tracking can begin or resume. If either gate is revoked or absent, the use case rejects tracking with a typed `LocationError.DualGateNotSatisfied`.

2. **Strictly Foreground-Only**:
   * **Zero Background Tracking**: `ACCESS_BACKGROUND_LOCATION` is deliberately omitted. No foreground service or background polling worker is created.
   * **Lifecycle-Coupled State**:
     - When the app is in the foreground: `TRACKING`.
     - When the Activity/session leaves the foreground: automatically transitions to `PAUSED`.
     - While `PAUSED`: zero location updates are polled or emitted.
     - On return to foreground: restored to `TRACKING` only if both gates remain satisfied.

3. **Qualitative Approximate Accuracy Handling**:
   * Android's approximate-location behavior is treated as the source of truth.
   * No hardcoded `~2 km` or `2000 m` radii.
   * Display qualitative, user-friendly guidance: *"Approximate location active — some location features may be less accurate."*

4. **Volatile In-Memory Coordinates**:
   * Coordinates (`UserLocation`) are broadcast via Kotlin `StateFlow` strictly in-memory.
   * Zero Room persistence, zero disk caching, and zero cloud transmission of coordinates.
   * Ending the journey always transitions state to `STOPPED` and permanently clears volatile location data.

---

## 6. Safe Journey Session Engine & Local Safety Checkpoints (Milestone 3)

Milestone 3 establishes the **`JourneySession`** as the central, unified lifecycle object for personal safety monitoring.

```
                      ┌────────────────────────┐
                      │    Session Engine      │
                      │    (JourneySession)    │
                      └───────────┬────────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐      ┌─────────────────┐      ┌──────────────────┐
│ Session Status  │      │ Location Stream │      │ Checkpoint Engine│
│  State Machine  │      │  (Volatile RAM) │      │  (Configurable)  │
│(8 Strict States)│      │  (Dual-Gated)   │      │ (Non-Alarmist)   │
└─────────────────┘      └─────────────────┘      └──────────────────┘
```

### 1. Hard Location Start Gate
* A session **cannot enter `ACTIVE`** unless both location consent and Android runtime location permission are verified.
* If either gate fails, the use case returns a typed `StartJourneyError.ConsentRequired` or `StartJourneyError.PermissionRequired`.
* The application surfaces the preflight dialog and leaves the session in `IDLE`. Degraded "location-less" journeys are forbidden in Milestone 3.

### 2. Injectable Time Abstraction (`Clock`)
* All temporal calculations (`calculateDurationMs`, checkpoint intervals, expiry evaluations) are mediated by `Clock { fun nowEpochMs(): Long }`.
* Production uses `SystemClock`, while tests use `FakeClock(initialEpochMs)`.
* Enables 100% deterministic, instant JVM test runs with zero `Thread.sleep()` or coroutine delays.

### 3. Persistent Metadata vs. Volatile Coordinates Boundary
* **Room Database (`journey_sessions` table, `SafeRouteDatabase` v2)**:
  - Persists `sessionId`, `status`, `startedAtEpochMs`, `endedAtEpochMs`, `origin`, `destination`, `lastCheckpointEpochMs`, `nextCheckpointEpochMs`, and `checkpointState`.
  - On application process restart, uncompleted sessions can be restored from Room.
* **Transient In-Memory Coordinates**:
  - Real-time `UserLocation` (lat, lon, accuracy) is fused in RAM into the `activeSession` StateFlow stream from `LocationRepository`.
  - Coordinates are **never** persisted to SQLite, preventing location breadcrumb tracking.

### 4. Deterministic Session Status State Machine
Strict transitions enforced by `SessionStatus`:
* `IDLE -> STARTING -> ACTIVE`
* `ACTIVE -> CHECKPOINT_DUE -> CHECKPOINT_ACKNOWLEDGED -> ACTIVE`
* `ACTIVE / CHECKPOINT_* -> COMPLETING -> COMPLETED -> IDLE`
* `STARTING / ACTIVE / CHECKPOINT_* -> CANCELLED -> IDLE`

### 5. Local Safety Checkpoints
* Driven by `CheckpointPolicy(enabled, intervalMs, acknowledgementWindowMs)` (default: 15-minute interval, 3-minute grace window).
* Prompts user with a non-alarmist foreground UI: **"Safety Check-In"** -> **"I'm Okay"** or **"End Journey"**.
* Expiry without acknowledgment transitions checkpoint status to `MISSED` (records a local safety signal only; does **not** trigger external alerts, contact dispatch, or SOS).

---

## 7. Local Journey Anomaly Intelligence (Milestone 4)

Milestone 4 introduces a deterministic, explainable, and local anomaly-detection engine on top of the active `JourneySession` and foreground location foundation.

```
                  ┌─────────────────────────────────────┐
                  │    Active Safe Journey Session      │
                  │  (SessionStatus + Location Stream)  │
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │       JourneyAnomalyDetector        │
                  │     (Pure Kotlin Domain Engine)     │
                  │  - Quality Filtering (Accuracy Cap) │
                  │  - Prolonged Stop Heuristic         │
                  │  - Route Deviation (Corridor)       │
                  │  - Duration Anomaly Heuristic       │
                  │  - Unusual Movement / Sudden Speed  │
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │      Typed AnomalySignal Objects    │
                  │ (PROLONGED_STOP, ROUTE_DEVIATION,   │
                  │  DURATION_ANOMALY, UNUSUAL_MOVEMENT)│
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │      InMemoryAnomalyRepository      │
                  │ (Sliding Window RAM, Zero Disk I/O) │
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │         JourneySignalsCard          │
                  │  (Calm, Informative Presentation)   │
                  └─────────────────────────────────────┘
```

### Core Architecture & Privacy Invariants

1. **Deterministic & Explainable Baseline**:
   - Every anomaly is computed using transparent geometric and temporal rules.
   - Zero opaque machine learning models, cloud inference, or black-box heuristics.
   - Serves as the stable ground truth that future ML/AI models can build upon.

2. **Crucial Safety Distinctions (Invariants)**:
   - `ANOMALY != DANGER`
   - `ANOMALY != EMERGENCY`
   - `ANOMALY != AUTOMATIC ALERT`
   - Anomalies represent localized contextual observations for the user; they do **not** trigger SOS, contact dispatch, SMS broadcasts, or 911 calls.

3. **Pure Domain Geodesics (`GeoMath`)**:
   - Pure Kotlin spherical mathematics (Haversine distance, great-circle forward bearing, angular difference, perpendicular point-to-polyline corridor distance).
   - Zero Android framework dependencies (`android.location.Location` is completely absent from the domain layer).

4. **Transient In-Memory Repository (`InMemoryAnomalyRepository`)**:
   - Anomalies and recent observation histories are maintained strictly in volatile RAM using a thread-safe sliding window (default: max 30 observations).
   - Zero Room persistence, zero disk caching, and zero historical breadcrumbs.
   - Cleared permanently when a journey ends, cancels, or resets.

5. **Data Quality & Accuracy Gating**:
   - Locations with horizontal accuracy exceeding `maxAccuracyThresholdMeters` (default: 50 m) or without speed readings are filtered or flagged as low-confidence to eliminate false positives in urban canyons.

6. **Calm, Non-Alarmist UI (`JourneySignalsCard`)**:
   - Surfaces active observations calmly with informative descriptions (e.g., *"Journey duration has exceeded the estimated arrival window"* or *"Movement has remained stationary for 10 minutes"*).
   - Does not employ flashing red alarms, screeching sirens, or anxiety-inducing dialogs.

### Heuristic Detection Rules & Default Policies

| Heuristic | Type | Default Threshold | Severity | Description |
|---|---|---|---|---|
| **Prolonged Stop** | `PROLONGED_STOP` | Stationary (< 35m) for ≥ 10 min with speed < 0.5 m/s | `MEDIUM` | Detects when progress ceases unexpectedly during travel. |
| **Route Deviation** | `ROUTE_DEVIATION` | Distance from polyline > 100 m | `HIGH` if > 250m, `MEDIUM` otherwise | Flags departure from the pre-agreed travel corridor. |
| **Duration Anomaly** | `DURATION_ANOMALY` | Elapsed time > 1.5x expected journey duration | `MEDIUM` | Flags journeys significantly overrunning the estimated arrival. |
| **Unusual Movement** | `UNUSUAL_MOVEMENT` | Speed spike > 30 m/s (~108 km/h) or > 120° bearing delta at speed | `MEDIUM` | Flags unnatural velocity shifts or sudden directional reversals. |

---

## 8. Future Module Boundaries & Isolation

1. **Audio & Speech Intelligence (`AudioIntelligenceRepository`, `SpeechIntelligenceRepository`) — Milestone 5**:
   - On-device acoustic anomaly and wake-word/distress classifiers.
2. **Multi-Signal Risk Assessment (`RiskAssessmentRepository`) — Milestone 6**:
   - Multi-signal sensor fusion engine aggregating temporal, spatial, and acoustic signals.
3. **Emergency Incident Management & SOS (`IncidentRepository`) — Milestone 7**:
   - SMS/call dispatch and emergency protocol coordination to enabled `TrustedContact` entities.

