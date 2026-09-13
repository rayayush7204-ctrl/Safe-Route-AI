# SafeRoute AI

SafeRoute AI is a personal safety journey application designed to accompany users during their daily travels with proactive, intelligent protection.

---

## Current Milestone: Milestone 4 - Local Journey Anomaly Intelligence

Milestone 4 implements a deterministic, explainable, and local anomaly-detection engine on top of the active `JourneySession` and foreground location foundation.

> [!NOTE]
> **Key Architecture & Privacy Invariants in Milestone 4**:
> * **Deterministic & Explainable Baseline**: Every anomaly is derived from pure, verifiable geometric and temporal heuristics (`JourneyAnomalyDetector`). Zero black-box ML models, cloud inference, or unpredictable heuristics.
> * **Strict Safety Invariants**:
>   - `ANOMALY != DANGER`
>   - `ANOMALY != EMERGENCY`
>   - `ANOMALY != AUTOMATIC ALERT`
>   - Anomalies represent localized contextual observations for the user; they do **not** trigger SOS, contact dispatch, SMS broadcasts, or 911 calls.
> * **Pure Domain Geodesics (`GeoMath`)**: Haversine distance, initial bearing, angular delta, and cross-track polyline distance calculations are implemented in pure Kotlin with zero Android framework dependencies (`android.location.Location` is prohibited in domain).
> * **Volatile In-Memory Repository (`InMemoryAnomalyRepository`)**: Observations and sliding-window histories (max 30 items) exist solely in RAM. Zero Room persistence, zero disk caching, and zero historical breadcrumbs. Cleared permanently when a journey ends, cancels, or resets.
> * **Data Quality & Accuracy Gating**: Locations with horizontal accuracy > 50m are filtered out to prevent false positives in high-reflection or urban-canyon environments.
> * **Calm, Non-Alarmist Presentation (`JourneySignalsCard`)**: Informative, low-anxiety indicators surface signals cleanly within the active journey dashboard.
> * **Strict Non-Goals Preserved**: Zero audio/microphone processing, speech recognition, risk scoring, cloud sync, Firebase, background location, or external telemetry.

---

## Technology Stack

* **Language**: Kotlin 2.0.21
* **Symbol Processing**: KSP `2.0.21-1.0.28`
* **Google Play Services Location**: `21.3.0` (FusedLocationProviderClient, Priority.PRIORITY_HIGH_ACCURACY)
* **Local Database**: AndroidX Room `2.8.5` (Entities, DAOs, Flow streams) - Schema Version 2
* **Lightweight Storage**: AndroidX DataStore Preferences `1.2.1`
* **UI Toolkit**: Jetpack Compose with Material 3
* **Compose Compiler**: Gradle Plugin (`org.jetbrains.kotlin.plugin.compose:2.0.21`)
* **Target & Compile SDK**: Android API 36
* **Min SDK**: API 26 (Android 8.0 Oreo)
* **Build System**: Gradle 8.11.1
* **Android Gradle Plugin (AGP)**: 8.10.1
* **JDK Compatibility**: Java 17
* **Architecture**: Clean Architecture + Unidirectional Data Flow (MVVM)
* **Asynchronous Streams**: Kotlin Coroutines & `StateFlow`
* **Lifecycle & Navigation**: AndroidX Lifecycle ViewModel & Navigation Compose
* **Testing**: JUnit 4 & `kotlinx-coroutines-test` (138 unit tests, 0 failures)

---

## Package Structure

Root package: `com.saferouteai`

```
app/src/main/java/com/saferouteai/
├── core/
│   └── result/
│       └── Result.kt                           # Sealed Result monad for typed success/failure
├── domain/
│   ├── time/
│   │   └── Clock.kt                            # Injectable Clock interface (nowEpochMs)
│   ├── anomaly/
│   │   └── JourneyAnomalyDetector.kt           # Pure deterministic anomaly engine
│   ├── model/
│   │   ├── ContactRelationship.kt              # Neutral relationship taxonomy (Parent, Sibling, Partner, etc.)
│   │   ├── Journey.kt                          # Core domain journey entity
│   │   ├── JourneyState.kt                     # Foundation state machine (IDLE, ACTIVE, COMPLETED)
│   │   ├── TrustedContact.kt                   # Contact model with validation rules
│   │   ├── UserConsent.kt                      # Explicit consent model (defaults false)
│   │   ├── UserProfile.kt                      # Local user profile model
│   │   ├── anomaly/
│   │   │   ├── AnomalyDetectionPolicy.kt       # Configurable anomaly policy thresholds
│   │   │   ├── AnomalySeverity.kt              # LOW, MEDIUM, HIGH severity enum
│   │   │   ├── AnomalySignal.kt                # Pure domain anomaly signal entity
│   │   │   ├── AnomalyType.kt                  # PROLONGED_STOP, ROUTE_DEVIATION, etc.
│   │   │   ├── ExpectedRoute.kt                # Route polyline & corridor model
│   │   │   └── GeoMath.kt                      # Pure Kotlin spherical math utilities
│   │   ├── location/
│   │   │   ├── LocationError.kt                # Location domain errors (Gates, Provider, etc.)
│   │   │   ├── LocationPermissionStatus.kt     # Permission status enum (NOT_REQUESTED, DENIED, etc.)
│   │   │   ├── LocationTrackingState.kt        # Foreground tracking state machine (IDLE, TRACKING, etc.)
│   │   │   └── UserLocation.kt                 # Pure domain geographic location model
│   │   └── session/
│   │       ├── CheckpointPolicy.kt             # Configurable local checkpoint timing policy
│   │       ├── CheckpointState.kt              # Checkpoint status (SCHEDULED, DUE, ACKNOWLEDGED, MISSED)
│   │       ├── JourneySession.kt               # Central Safe Journey Session domain model
│   │       ├── SessionStatus.kt                # Strict 8-state machine enum
│   │       └── StartJourneyError.kt            # Typed start-gate domain error hierarchy
│   ├── repository/
│   │   ├── AnomalyRepository.kt                # Milestone 4 anomaly storage contract
│   │   ├── ConsentRepository.kt                # Consent storage interface
│   │   ├── JourneyRepository.kt                # Foundation journey repository contract
│   │   ├── JourneySessionRepository.kt         # Milestone 3 Safe Journey session engine contract
│   │   ├── LocationPermissionChecker.kt        # Abstract runtime permission checker
│   │   ├── LocationRepository.kt               # Location tracking repository contract
│   │   ├── TrustedContactsRepository.kt        # Trusted contacts repository contract
│   │   └── UserProfileRepository.kt            # Profile repository contract
│   └── usecase/
│       ├── anomaly/
│       │   ├── ClearAnomaliesUseCase.kt        # Reset/purge active anomaly state
│       │   ├── EvaluateJourneyAnomaliesUseCase.kt# Evaluate current session & location
│       │   └── GetActiveAnomaliesUseCase.kt    # Flow stream of active anomalies
│       ├── location/
│       │   ├── GetLocationTrackingStateUseCase.kt
│       │   ├── GetLocationUpdatesUseCase.kt
│       │   ├── PauseLocationTrackingUseCase.kt
│       │   ├── ResumeLocationTrackingUseCase.kt
│       │   ├── StartLocationTrackingUseCase.kt
│       │   └── StopLocationTrackingUseCase.kt
│       └── session/
│           ├── AcknowledgeCheckpointUseCase.kt # "I'm Okay" checkpoint acknowledgement
│           ├── CancelJourneySessionUseCase.kt  # User-initiated journey cancellation
│           ├── EndJourneySessionUseCase.kt     # Graceful completion & location shutdown
│           ├── GetActiveJourneySessionUseCase.kt
│           ├── ResetJourneySessionUseCase.kt   # Terminal state reset to IDLE
│           ├── StartJourneySessionUseCase.kt   # Hard-gated session start
│           └── TriggerCheckpointDueUseCase.kt  # Checkpoint due trigger
├── data/
│   ├── time/
│   │   └── SystemClock.kt                      # Production Clock implementation
│   ├── local/
│   │   ├── SafeRouteDatabase.kt                # Room Database (v2)
│   │   ├── dao/
│   │   │   ├── JourneySessionDao.kt            # Session metadata DAO
│   │   │   ├── TrustedContactDao.kt            # Contacts DAO
│   │   │   └── UserProfileDao.kt               # Profile DAO
│   │   ├── datastore/
│   │   │   └── ConsentDataStore.kt             # DataStore Preferences wrapper
│   │   └── entity/
│   │       ├── JourneySessionEntity.kt         # Session metadata entity (no coordinates)
│   │       ├── TrustedContactEntity.kt         # Contacts SQLite entity
│   │       └── UserProfileEntity.kt            # Profile SQLite entity
│   ├── location/
│   │   ├── AndroidLocationPermissionChecker.kt # Android context-backed permission checker
│   │   ├── FusedLocationDataSource.kt          # Play Services FusedLocationProviderClient wrapper
│   │   └── LocationMapper.kt                   # Maps android.location.Location -> UserLocation
│   └── repository/
│       ├── DataStoreConsentRepository.kt       # DataStore backed consent repository
│       ├── FusedLocationRepository.kt          # Fused location tracking repository
│       ├── InMemoryAnomalyRepository.kt        # Thread-safe volatile sliding window
│       ├── InMemoryConsentRepository.kt        # Test double
│       ├── InMemoryJourneyRepository.kt        # Test double
│       ├── InMemoryJourneySessionRepository.kt # Pure JVM test double with FakeClock
│       ├── InMemoryLocationRepository.kt       # Test double
│       ├── InMemoryTrustedContactsRepository.kt# Test double
│       ├── InMemoryUserProfileRepository.kt    # Test double
│       ├── RoomJourneySessionRepository.kt     # Production session repository (Room + volatile coords)
│       ├── RoomTrustedContactsRepository.kt    # Production Room contacts repository
│       └── RoomUserProfileRepository.kt        # Production Room profile repository
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt                       # Safe Journey dashboard with session & checkpoints
│   │   ├── JourneyUiState.kt                   # UI state with session, checkpoints & active anomalies
│   │   ├── JourneyViewModel.kt                 # Coordinates session lifecycle, location & anomalies
│   │   └── components/
│   │       ├── CheckpointStatusCard.kt         # Visual card showing check-in status / countdown
│   │       ├── JourneySignalsCard.kt           # Calm, non-alarmist active anomaly card
│   │       ├── LocationPreflightDialog.kt      # Dual-gate permission & consent explanation dialog
│   │       ├── LocationStatusCard.kt           # Real-time coordinates & qualitative accuracy
│   │       └── SafetyCheckInDialog.kt          # Foreground check-in modal ("I'm Okay" / "End Journey")
│   ├── navigation/
│   │   ├── SafeRouteNavHost.kt                 # Navigation host with all routes
│   │   └── Screen.kt                           # Route definitions
│   └── MainActivity.kt                         # Entry point with DI wiring & lifecycle forwarding
```

---

## Local Build & Run Instructions

This project is configured to build entirely via the Gradle command line without requiring Android Studio.

### Prerequisites
* JDK 17 configured in PATH
* Android SDK Platform 36 and Build Tools 36.0.0 installed (`C:\Android`)

### Build Commands (Windows PowerShell)

1. **Run Full Unit Test Suite**:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
   Executes **138 unit tests** (0 failures) covering pure spherical geodesics, anomaly detector heuristics, sliding window retention, dual-gate location verification, session state machines, Room DAO entities, and ViewModels.

2. **Assemble Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   Outputs the debug APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Future Roadmap

* **Milestone 5**: On-Device Acoustic Intelligence & Wake-Word Distress Detection.
* **Milestone 6**: Multi-Signal Risk Assessment & Dynamic Safety Tier Engine.
* **Milestone 7**: Emergency Protocol Dispatch, Incident Logging, and SOS Coordination.

