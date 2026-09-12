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

## 6. Future Module Boundaries & Isolation

1. **Local Safety Session & Anomaly Heuristics — Milestone 3**:
   - Session metadata persistence, timed checkpoints, stopped motion detection, and route deviation indicators.
2. **Audio & Speech Intelligence (`AudioIntelligenceRepository`, `SpeechIntelligenceRepository`) — Milestone 4**:
   - On-device acoustic anomaly and wake-word/distress classifiers.
3. **Risk Assessment (`RiskAssessmentRepository`) — Milestone 5**:
   - Sensor fusion engine aggregating temporal, spatial, and acoustic signals.
4. **Incident Management & SOS (`IncidentRepository`) — Milestone 6**:
   - SMS/call dispatch and emergency protocol coordination to enabled `TrustedContact` entities.
