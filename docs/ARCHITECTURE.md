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
       │ (InMemory State, Local DB, Network/Sensors [Future])    │
       └─────────────────────────────────────────────────────────┘
                                    │
       ┌────────────────────────────┴────────────────────────────┐
       │                      Core Layer                         │
       │          (Result Monad, Utilities, Dispatchers)         │
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
* **Entities**: `JourneyState`, `Journey`.
* **Use Cases**: Single-responsibility business actions (`StartJourneyUseCase`, `EndJourneyUseCase`, `GetJourneyStateUseCase`, `ResetJourneyUseCase`).
* **Repository Contracts**: Abstract interfaces declaring operational boundaries (`JourneyRepository`, `LocationRepository`, `AudioIntelligenceRepository`, etc.).
* **Boundary Rules**: Zero Android framework dependencies (`android.*` imports are prohibited in the domain layer).

### Data Layer (`com.saferouteai.data`)
* Coordinates data sources and implements domain repository contracts.
* In **Milestone 1**, state is managed purely through `InMemoryJourneyRepository` utilizing Kotlin `StateFlow` and coroutines `Mutex` locks for thread safety.
* In future milestones, persistent storage (Room/DataStore) and hardware drivers will reside here.

### Presentation Layer (`com.saferouteai.presentation`)
* Renders user experience through declarative Jetpack Compose and Material 3 design tokens.
* **MVVM**: `JourneyViewModel` transforms domain use case streams into an immutable `JourneyUiState` exposed via `StateFlow`.
* Composables are strictly stateless where possible, reacting to state emissions and dispatching user intents.
* **Navigation**: Jetpack Navigation Compose (`SafeRouteNavHost`, `Screen`).

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

### Valid Transitions for Milestone 1:
* `IDLE` ➔ `ACTIVE`
* `ACTIVE` ➔ `COMPLETED`
* `COMPLETED` ➔ `IDLE` / `ACTIVE`
* Any attempt to perform an unauthorized transition (e.g. `IDLE` ➔ `COMPLETED`) is rejected at both the domain model and repository boundary.

---

## 4. Future Module Boundaries & Isolation

SafeRoute AI isolates specialized safety subsystems behind explicit interfaces:

1. **Location Subsystem (`LocationRepository`)**:
   - Manages geofencing, breadcrumbs, and route deviation.
   - Strictly isolated from UI; communicates exclusively through domain use cases.
2. **Audio & Speech Intelligence (`AudioIntelligenceRepository`, `SpeechIntelligenceRepository`)**:
   - On-device acoustic anomaly and wake-word/distress classifiers.
   - Operates with strict user consent gates and hardware privacy indicators.
3. **Risk Assessment (`RiskAssessmentRepository`)**:
   - Sensor fusion engine aggregating temporal, spatial, and acoustic signals.
4. **Incident Management & SOS (`IncidentRepository`, `TrustedContactsRepository`)**:
   - SMS/call dispatch and emergency protocol coordination.

---

## 5. Security & Privacy Guarantees

* **Zero Runtime Permissions**: Milestone 1 declares and requests zero dangerous Android permissions.
* **Zero Telemetry / Analytics**: No hidden tracking or cloud pings.
* **Local Isolation**: All state transitions occur in local process memory.
