# VigiPro - Professional Security Camera Monitoring

## Project Overview

VigiPro is a comprehensive, multi-module Android application designed for professional security camera monitoring (CCTV/ONVIF). It leverages modern Android development practices, including Jetpack Compose for UI, Orbit MVI for state management, and Supabase as a backend service. The application supports features such as RTSP video playback, PTZ control, device discovery via ONVIF, and QR code-based access sharing.

## Tech Stack & Architecture

### Core Technologies
*   **Language:** Kotlin 2.1.10
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Architecture:** Clean Architecture with Multi-Module setup
*   **Dependency Injection:** Dagger Hilt
*   **State Management:** Orbit MVI
*   **Networking:** Ktor + Supabase SDK (Auth, PostgREST, Realtime, Storage)
*   **Local Persistence:** Room Database + DataStore Preferences
*   **Video Playback:** Media3 / ExoPlayer (RTSP support)
*   **Camera Protocol:** ONVIF (`com.seanproctor:onvifcamera`)
*   **Image Loading:** Coil 3
*   **Build System:** Gradle 8.12.1 + Version Catalogs (`libs.versions.toml`) + Convention Plugins

### Module Structure
The project follows a standard modular architecture:

*   **`app`**: The application entry point (`VigiProApp`, `MainActivity`, `NavHost`). Connects features.
*   **`core/`**: Fundamental components used across features.
    *   `core-model`: Domain models (pure Kotlin, `kotlinx.serialization`).
    *   `core-network`: Network clients (Supabase, Ktor). **Configuration required here.**
    *   `core-data`: Repositories, Room database, DataStore.
    *   `core-ui`: Reusable Compose components, Theme, Design System.
*   **`feature/`**: Functional features of the app.
    *   `feature-auth`: Login/Register flows.
    *   `feature-dashboard`: Camera grid and monitoring dashboard.
    *   `feature-player`: Video player with PTZ controls.
    *   `feature-devices`: Device management and ONVIF discovery.
    *   `feature-access-control`: Access management and QR code generation.
    *   `feature-settings`: Application settings.
*   **`build-logic/`**: Custom Gradle convention plugins to standardize build configuration across modules.

## Setup & Configuration

### Prerequisites
*   **JDK:** Java 21 is required for building this project.
*   **Android SDK:** Compile SDK 36, Min SDK 26.

### Environment Configuration
The application connects to a Supabase backend. You must configure the API credentials.

1.  Open `core/core-network/build.gradle.kts`.
2.  Update the `defaultConfig` block with your actual Supabase URL and Anon Key:

```kotlin
defaultConfig {
    buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
    buildConfigField("String", "SUPABASE_KEY", "\"your-anon-key\"")
}
```

## Build & Run Commands

Use the following Gradle wrapper commands:

| Action | Command |
| :--- | :--- |
| **Build Debug APK** | `./gradlew assembleDebug` |
| **Build Release APK** | `./gradlew assembleRelease` |
| **Run Unit Tests** | `./gradlew test` |
| **Run Module Tests** | `./gradlew :feature:feature-auth:testDebugUnitTest` |
| **Lint Check** | `./gradlew lint` |
| **Clean Build** | `./gradlew clean` |
| **Sync Dependencies** | `./gradlew dependencies` |

## Development Conventions

### Code Style & Patterns
*   **Composables:** Use PascalCase. The `modifier: Modifier = Modifier` should always be the last parameter.
*   **State Management:** Use Orbit MVI. Features should implement `ContainerHost` in their ViewModels.
*   **Navigation:** Use Jetpack Navigation Compose with string-based routes (e.g., `"login"`, `"player/$cameraId"`).
*   **Serialization:** Use `kotlinx.serialization`. API models use `@SerialName` for snake_case mapping.
*   **Resources:** Strings are in Brazilian Portuguese (pt-BR).

### Dependency Injection
*   **App Module:** Annotated with `@HiltAndroidApp`.
*   **Activities:** Annotated with `@AndroidEntryPoint`.
*   **Modules:** Use `@Module` and `@InstallIn(SingletonComponent::class)` for global dependencies.

### Database
*   **Room:** Schemas are exported to `core/core-data/schemas/`. Ensure schema versioning is maintained when modifying entities.

## Key Files
*   `settings.gradle.kts`: Module inclusion and plugin management.
*   `gradle/libs.versions.toml`: Central version catalog for all dependencies.
*   `build-logic/convention/src/main/kotlin/`: Convention plugins definitions.
*   `CLAUDE.md`: Additional AI-assistant specific context (highly recommended reading).
