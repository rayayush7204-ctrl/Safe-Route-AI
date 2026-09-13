# SafeRoute AI

SafeRoute AI is a personal safety journey application designed to accompany users during their daily travels with proactive, intelligent protection.

---

## Current Milestone: Milestone 3 - Safe Journey Session Engine & Local Safety Checkpoints

Milestone 3 implements the central architectural foundation for an active **Safe Journey Session** in SafeRoute AI, integrating the dual-gated foreground location tracking foundation (from Milestone 2B) with a deterministic 8-state session state machine, Room session persistence, and local, non-alarmist safety checkpoints.

> [!NOTE]
> **Key Architecture & Privacy Invariants in Milestone 3**:
> * **Hard Location Start Gate**: A Safe Journey Session strictly requires verified location readiness before entering `ACTIVE`. If explicit location consent (`locationSharingConsent == true`) or Android runtime location permission is missing, the session does not silently start in a degraded mode; it returns a typed `StartJourneyError` and leaves the session in `IDLE`.
> * **Injectable Clock Abstraction**: Domain time operations, durations, and checkpoint calculations depend on `Clock { fun nowEpochMs(): Long }`. Production uses `SystemClock`, while unit tests use `FakeClock` with `advanceTimeBy()` for 100% deterministic, zero-sleep testing.
> * **Strict Persistent vs. Volatile Memory Boundary**:
>   - **Room Database (`SafeRouteDatabase` v2)** persists high-level session metadata only (`sessionId`, `status`, start/end timestamps, checkpoint timestamps) via `JourneySessionEntity`.
>   - **Volatile In-Memory RAM**: Real-time `UserLocation` coordinates are dynamically streamed through `StateFlow` and **never** persisted to SQLite or written as historical breadcrumb tracks.
> * **Local, Non-Alarmist Safety Checkpoints**: Configurable periodic check-ins (`CheckpointPolicy`) prompt the user via the foreground UI ("Safety Check-In" -> "I'm Okay" / "End Journey"). Missed checkpoints record a local safety signal only and do **NOT** trigger external alerts, SMS broadcasts, or 911 calls.
> * **Strict Non-Goals Preserved**: Zero automatic journey detection, route deviation, audio AI, speech recognition, risk scoring, cloud sync, Firebase, background location, or external telemetry.

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
* **Testing**: JUnit 4 & `kotlinx-coroutines-test` (113 unit tests, 0 failures)

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
│   ├── model/
│   │   ├── ContactRelationship.kt              # Neutral relationship taxonomy (Parent, Sibling, Partner, etc.)
│   │   ├── Journey.kt                          # Core domain journey entity
│   │   ├── JourneyState.kt                     # Foundation state machine (IDLE, ACTIVE, COMPLETED)
│   │   ├── TrustedContact.kt                   # Contact model with validation rules
│   │   ├── UserConsent.kt                      # Explicit consent model (defaults false)
│   │   ├── UserProfile.kt                      # Local user profile model
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
│   │   ├── ConsentRepository.kt                # Consent storage interface
│   │   ├── JourneyRepository.kt                # Foundation journey repository contract
│   │   ├── JourneySessionRepository.kt         # Milestone 3 Safe Journey session engine contract
│   │   ├── LocationPermissionChecker.kt        # Abstract runtime permission checker
│   │   ├── LocationRepository.kt               # Location tracking repository contract
│   │   ├── TrustedContactsRepository.kt        # Trusted contacts repository contract
│   │   └── UserProfileRepository.kt            # Profile repository contract
│   └── usecase/
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
│   │   ├── JourneyUiState.kt                   # UI state with session, duration, and checkpoint state
│   │   ├── JourneyViewModel.kt                 # Coordinates session lifecycle & dual gates
│   │   └── components/
│   │       ├── CheckpointStatusCard.kt         # Visual card showing check-in status / countdown
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
   Executes **113 unit tests** (0 failures) covering domain state machines, fake-clock temporal scheduling, hard-gated session use cases, Room DAO entities, and ViewModels.

2. **Assemble Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   Outputs the debug APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Future Roadmap

* **Milestone 4**: On-Device Acoustic Intelligence & Wake-Word Distress Detection.
* **Milestone 5**: Multi-Signal Risk Assessment & Dynamic Safety Tier Engine.
* **Milestone 6**: Emergency Protocol Dispatch, Incident Logging, and SOS Coordination.
