# SafeRoute AI

SafeRoute AI is a personal safety journey application designed to accompany users during their daily travels with proactive, intelligent protection.

---

## Current Milestone: Milestone 1 - Android Application Foundation

Milestone 1 establishes the foundational Clean Architecture, state machine, Jetpack Compose Material 3 presentation layer, and automated command-line testing pipeline.

> [!NOTE]
> **Privacy & Sensor Notice**: Milestone 1 is purely an architectural and state demonstration foundation. **This version does NOT collect location data, record audio via microphone, initiate background tracking, or send telemetry to any external server.** Zero runtime permissions are requested or declared.

---

## Technology Stack

* **Language**: Kotlin 2.0.21
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
│       └── Result.kt                       # Sealed Result monad for typed success/failure
├── domain/
│   ├── model/
│   │   ├── Journey.kt                      # Core domain journey entity
│   │   └── JourneyState.kt                 # Domain state machine (IDLE, ACTIVE, COMPLETED)
│   ├── repository/
│   │   ├── JourneyRepository.kt            # Core contract for journey lifecycle management
│   │   ├── LocationRepository.kt           # Future contract placeholder (M2+)
│   │   ├── JourneyDetectionRepository.kt   # Future contract placeholder (M3+)
│   │   ├── AudioIntelligenceRepository.kt  # Future contract placeholder (M4+)
│   │   ├── SpeechIntelligenceRepository.kt # Future contract placeholder (M4+)
│   │   ├── RiskAssessmentRepository.kt     # Future contract placeholder (M5+)
│   │   ├── IncidentRepository.kt           # Future contract placeholder (M6+)
│   │   ├── NotificationRepository.kt       # Future contract placeholder (M3+)
│   │   └── TrustedContactsRepository.kt    # Future contract placeholder (M2+)
│   └── usecase/
│       ├── GetJourneyStateUseCase.kt       # Observes current state flow
│       ├── StartJourneyUseCase.kt          # Validates & initiates journey
│       ├── EndJourneyUseCase.kt            # Concludes active journey
│       └── ResetJourneyUseCase.kt          # Resets state back to IDLE
├── data/
│   └── repository/
│       └── InMemoryJourneyRepository.kt    # Thread-safe in-memory state repository
├── presentation/
│   ├── common/
│   │   ├── SectionCard.kt                  # M3 surface card component
│   │   └── StatusBadge.kt                  # Journey status indicator pill
│   ├── home/
│   │   ├── HomeScreen.kt                   # Primary journey monitoring dashboard
│   │   ├── JourneyUiState.kt               # Immutable UI state model
│   │   └── JourneyViewModel.kt             # Unidirectional StateFlow ViewModel
│   ├── settings/
│   │   └── SettingsScreen.kt               # Settings view (Profile, Contacts, Privacy)
│   ├── navigation/
│   │   ├── SafeRouteNavHost.kt             # Navigation host and route transitions
│   │   └── Screen.kt                       # Route definitions
│   └── theme/
│       ├── Color.kt                        # Accessible Light/Dark color palette
│       ├── Theme.kt                        # Material 3 Theme wrapper
│       └── Type.kt                         # Typography scale
└── MainActivity.kt                         # Main application launcher Activity
```

---

## Local Build Instructions

This project is configured to build entirely via the Gradle command line without requiring Android Studio.

### Prerequisites
* JDK 17 configured in PATH
* Android SDK Platform 36 and Build Tools 36.0.0 installed (default: `C:\Android`)

### Build Commands (Windows PowerShell)

1. **Verify / Compile Debug Build**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The compiled debug APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

2. **Clean Project**:
   ```powershell
   .\gradlew.bat clean
   ```

---

## Testing Instructions

Unit tests are written to run entirely in local JVM memory without requiring an Android emulator or physical device.

Execute unit tests:
```powershell
.\gradlew.bat testDebugUnitTest
```

Tested scenarios include:
* Domain `JourneyState` allowed and forbidden state transitions.
* `JourneyUseCases` initial state, journey start, journey completion, invalid transition rejection, and state reset.
* `JourneyViewModel` reactive `StateFlow` emissions for `IDLE` ➔ `ACTIVE` ➔ `COMPLETED` and error handling.

---

## Future Roadmap

* **Milestone 2**: User Profile, Trusted Contacts, and Privacy-Preserving Geolocation Tracking.
* **Milestone 3**: Automated Transit & Activity Detection with Local System Notifications.
* **Milestone 4**: On-Device Acoustic Intelligence & Wake-Word Distress Detection.
* **Milestone 5**: Multi-Signal Risk Assessment & Dynamic Safety Tier Engine.
* **Milestone 6**: Emergency Protocol Dispatch, Incident Logging, and SOS Coordination.
