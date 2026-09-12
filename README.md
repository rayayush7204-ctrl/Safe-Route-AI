# SafeRoute AI

SafeRoute AI is a personal safety journey application designed to accompany users during their daily travels with proactive, intelligent protection.

---

## Current Milestone: Milestone 2A - User Profile, Trusted Contacts & Consent Foundation

Milestone 2A implements a privacy-first, on-device local identity, trusted emergency contacts management, and explicit user consent foundation.

> [!NOTE]
> **Privacy & Permissions Guarantee**:
> * **Zero Runtime Permissions**: Milestone 2A requests zero Android permissions.
> * **Zero Location Tracking**: No GPS, network location, or location services are implemented in this milestone.
> * **Zero Audio Recording**: No microphone or acoustic monitoring is active.
> * **Zero Background Services**: The app operates purely while foregrounded by the user.
> * **Zero Cloud Telemetry**: All profile, contact, and consent data resides strictly on-device in local SQLite storage (Room) and DataStore Preferences.
> * **Explicit Consent**: Consent flags for location and emergency sharing default strictly to `false` and are never automatically enabled or inferred from adding contacts.

---

## Technology Stack

* **Language**: Kotlin 2.0.21
* **Symbol Processing**: KSP `2.0.21-1.0.28`
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
│   │   └── UserProfile.kt                      # Local user profile model
│   ├── repository/
│   │   ├── ConsentRepository.kt                # Contract for user consent flags
│   │   ├── JourneyRepository.kt                # Journey state management contract
│   │   ├── TrustedContactsRepository.kt        # Trusted contact CRUD contract
│   │   ├── UserProfileRepository.kt            # Profile persistence contract
│   │   └── [Future Repository Placeholders]    # Location, Audio, Risk, Incident, etc.
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
│       └── UpdateTrustedContactUseCase.kt      # Validates & updates contact
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
│   └── repository/
│       ├── DataStoreConsentRepository.kt       # DataStore backed consent repository
│       ├── InMemoryConsentRepository.kt        # Pure in-memory repository (for testing)
│       ├── InMemoryJourneyRepository.kt        # Thread-safe journey repository
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
│   │   ├── JourneyUiState.kt                   # Journey UI state model
│   │   └── JourneyViewModel.kt                 # Journey state ViewModel
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
└── MainActivity.kt                             # Entry point wiring local DB and ViewModels
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
   Executes 53 unit tests across domain models, use cases, view models, and validation rules.

2. **Assemble Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   Outputs the debug APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Future Roadmap

* **Milestone 2B**: User-Controlled Foreground Geolocation Services (gated by explicit location sharing consent and runtime permission).
* **Milestone 3**: Automated Transit & Activity Detection with Local System Notifications.
* **Milestone 4**: On-Device Acoustic Intelligence & Wake-Word Distress Detection.
* **Milestone 5**: Multi-Signal Risk Assessment & Dynamic Safety Tier Engine.
* **Milestone 6**: Emergency Protocol Dispatch, Incident Logging, and SOS Coordination.
