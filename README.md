# SafeRoute AI

SafeRoute AI is a personal safety journey application designed to accompany users during their daily travels with proactive, intelligent protection.

---

## Current Milestone: Milestone 2B - Foreground Location Foundation & User-Controlled Tracking

Milestone 2B implements a strictly foreground-only, dual-gated geolocation tracking foundation during active Safe Journeys.

> [!NOTE]
> **Privacy & Location Tracking Invariants**:
> * **Strict Dual-Gate Invariant**: Location tracking **strictly requires BOTH** Android runtime location permission (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) AND explicit `locationSharingConsent == true` in UserConsent. Neither condition alone is sufficient.
> * **Strictly Foreground-Only**: Zero background tracking (`ACCESS_BACKGROUND_LOCATION` is prohibited). Tracking operates purely while the application Activity is in the foreground (`TRACKING`). When the Activity leaves the foreground, tracking automatically transitions to `PAUSED` and ceases emissions.
> * **Qualitative Accuracy UI**: Uses Android's approximate-location behavior as the source of truth, presenting qualitative feedback (*"Approximate location active — some location features may be less accurate."*) rather than arbitrary hardcoded radius numbers.
> * **Volatile In-Memory Coordinates**: Live coordinates are streamed via StateFlow in volatile memory only. Coordinates are **never** persisted to Room, written to disk, or transmitted to any backend. Ending a journey always transitions tracking to `STOPPED` and permanently clears volatile location state.
> * **Zero Cloud Telemetry**: Zero Firebase, WebSockets, cloud transmission, audio recording, AI inference, or background services.

---

## Technology Stack

* **Language**: Kotlin 2.0.21
* **Symbol Processing**: KSP `2.0.21-1.0.28`
* **Google Play Services Location**: `21.3.0` (FusedLocationProviderClient, Priority.PRIORITY_HIGH_ACCURACY)
* **Local Database**: AndroidX Room `2.8.5` (Entities, DAOs, Flow streams)
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
* **Testing**: JUnit 4 & `kotlinx-coroutines-test`

---

## Package Structure

Root package: `com.saferouteai`

```
app/src/main/java/com/saferouteai/
├── core/
│   └── result/
│       └── Result.kt                           # Sealed Result monad for typed success/failure
├── domain/
│   ├── model/
│   │   ├── ContactRelationship.kt              # Neutral relationship taxonomy (Parent, Sibling, Partner, Friend, Other)
│   │   ├── Journey.kt                          # Core domain journey entity
│   │   ├── JourneyState.kt                     # Domain state machine (IDLE, ACTIVE, COMPLETED)
│   │   ├── TrustedContact.kt                   # Contact model with validation rules
│   │   ├── UserConsent.kt                      # Explicit consent model (defaults false)
│   │   ├── UserProfile.kt                      # Local user profile model
│   │   └── location/
│   │       ├── LocationError.kt                # Location domain errors (Gates, Provider, etc.)
│   │       ├── LocationPermissionStatus.kt     # Fine / Coarse / Denied status
│   │       ├── LocationTrackingState.kt        # IDLE, READY, TRACKING, PAUSED, ERROR, STOPPED
│   │       └── UserLocation.kt                 # Volatile in-memory location model
│   ├── repository/
│   │   ├── ConsentRepository.kt                # Contract for user consent flags
│   │   ├── JourneyRepository.kt                # Journey state management contract
│   │   ├── LocationPermissionChecker.kt        # Contract for runtime permission inspection
│   │   ├── LocationRepository.kt               # Contract for foreground tracking & state
│   │   ├── TrustedContactsRepository.kt        # Trusted contact CRUD contract
│   │   └── UserProfileRepository.kt            # Profile persistence contract
│   └── usecase/
│       ├── AddTrustedContactUseCase.kt         # Validates & creates trusted contact
│       ├── EndJourneyUseCase.kt                # Concludes active journey
│       ├── GetConsentUseCase.kt                # Observes consent flow
│       ├── GetJourneyStateUseCase.kt           # Observes journey state flow
│       ├── GetTrustedContactsUseCase.kt        # Observes contact list flow
│       ├── GetUserProfileUseCase.kt            # Observes user profile flow
│       ├── RemoveTrustedContactUseCase.kt      # Deletes contact by ID
│       ├── ResetJourneyUseCase.kt              # Resets journey to IDLE
│       ├── SaveUserProfileUseCase.kt           # Validates & saves profile
│       ├── SetContactEnabledUseCase.kt         # Toggles contact active state
│       ├── StartJourneyUseCase.kt              # Initiates journey
│       ├── UpdateConsentUseCase.kt             # Explicit consent mutator
│       ├── UpdateTrustedContactUseCase.kt      # Validates & updates contact
│       └── location/
│           ├── GetLocationTrackingStateUseCase.kt # Observes tracking state flow
│           ├── GetLocationUpdatesUseCase.kt       # Observes location stream
│           ├── PauseLocationTrackingUseCase.kt    # Pauses tracking on background
│           ├── ResumeLocationTrackingUseCase.kt   # Dual-gate validated resume
│           ├── StartLocationTrackingUseCase.kt    # Dual-gate validated start
│           └── StopLocationTrackingUseCase.kt     # Halts tracking & clears coordinates
├── data/
│   ├── local/
│   │   ├── SafeRouteDatabase.kt                # Room database (Room 2.8.5)
│   │   ├── dao/
│   │   │   ├── TrustedContactDao.kt            # Room DAO for contacts
│   │   │   └── UserProfileDao.kt               # Room DAO for profile
│   │   ├── datastore/
│   │   │   └── ConsentDataStore.kt             # DataStore Preferences wrapper
│   │   └── entity/
│   │       ├── TrustedContactEntity.kt         # Room entity for trusted_contacts
│   │       └── UserProfileEntity.kt            # Room entity for user_profile
│   ├── location/
│   │   ├── AndroidLocationPermissionChecker.kt # Context-based permission inspector
│   │   ├── FusedLocationDataSource.kt          # Play Services FusedLocation callback wrapper
│   │   └── LocationMapper.kt                   # Android Location -> UserLocation mapper
│   └── repository/
│       ├── DataStoreConsentRepository.kt       # DataStore backed consent repository
│       ├── FusedLocationRepository.kt          # Play Services backed location repository
│       ├── InMemoryConsentRepository.kt        # Pure in-memory repository (for testing)
│       ├── InMemoryJourneyRepository.kt        # Thread-safe journey repository
│       ├── InMemoryLocationRepository.kt       # Pure in-memory location repository (for testing)
│       ├── InMemoryTrustedContactsRepository.kt# Pure in-memory repository (for testing)
│       ├── InMemoryUserProfileRepository.kt    # Pure in-memory repository (for testing)
│       ├── RoomTrustedContactsRepository.kt    # Room backed contacts repository
│       └── RoomUserProfileRepository.kt        # Room backed profile repository
├── presentation/
│   ├── common/
│   │   ├── SectionCard.kt                      # M3 surface card component
│   │   └── StatusBadge.kt                      # Journey status indicator pill
│   ├── consent/
│   │   ├── ConsentUiState.kt                   # State model for consent screen
│   │   ├── ConsentViewModel.kt                 # Consent toggle ViewModel
│   │   └── PrivacyConsentScreen.kt             # Transparent consent & privacy screen
│   ├── contacts/
│   │   ├── AddEditContactDialog.kt             # Dialog for adding/editing contacts
│   │   ├── TrustedContactsScreen.kt            # Contact list & management screen
│   │   ├── TrustedContactsUiState.kt           # Contacts UI state model
│   │   └── TrustedContactsViewModel.kt         # Contacts ViewModel
│   ├── home/
│   │   ├── HomeScreen.kt                       # Primary journey dashboard
│   │   ├── JourneyUiState.kt                   # Journey UI state model with location
│   │   ├── JourneyViewModel.kt                 # Journey & location tracking coordinator
│   │   ├── LocationPermissionLauncher.kt       # Rememberable launcher for dual permissions
│   │   ├── LocationPreflightDialog.kt          # Preflight explanation dialog
│   │   └── LocationStatusCard.kt               # Live location card & qualitative accuracy
│   ├── profile/
│   │   ├── ProfileScreen.kt                    # Profile editor screen
│   │   ├── ProfileUiState.kt                   # Profile UI state model
│   │   └── ProfileViewModel.kt                 # Profile ViewModel
│   ├── settings/
│   │   └── SettingsScreen.kt                   # Settings hub linking to sub-flows
│   ├── navigation/
│   │   ├── SafeRouteNavHost.kt                 # Navigation host with all routes
│   │   └── Screen.kt                           # Route definitions (Home, Settings, Profile, Contacts, Consent)
│   └── theme/
│       ├── Color.kt                            # Material 3 color system
│       ├── Theme.kt                            # Material 3 Theme wrapper
│       └── Type.kt                             # Typography scale
└── MainActivity.kt                             # Activity with onStart/onStop lifecycle hooks
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
   Executes 74 unit tests across domain models, use cases, view models, lifecycle transitions, and dual-gate security validations.

2. **Assemble Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   Outputs the debug APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Future Roadmap

* **Milestone 3**: Local Safety Session & Anomaly Heuristics (local session persistence, timed checkpoints, deviation/stopped-motion alerts).
* **Milestone 4**: On-Device Acoustic Intelligence & Wake-Word Distress Detection.
* **Milestone 5**: Multi-Signal Risk Assessment & Dynamic Safety Tier Engine.
* **Milestone 6**: Emergency Protocol Dispatch, Incident Logging, and SOS Coordination.
