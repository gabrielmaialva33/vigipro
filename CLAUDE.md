# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VigiPro is a multi-module Android app for professional security camera monitoring (CCTV/ONVIF). Built with Kotlin, Jetpack Compose, Supabase backend, and Orbit MVI for state management. UI strings are in Brazilian Portuguese.

## Build & Run Commands

```bash
# Build the project
./gradlew assembleDebug

# Build release APK (minification + shrink enabled)
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew :feature:feature-auth:testDebugUnitTest
./gradlew :core:core-data:testDebugUnitTest
./gradlew :feature:feature-dashboard:testDebugUnitTest

# Lint check
./gradlew lint

# Clean build
./gradlew clean

# Supabase CLI (linked to project)
npx supabase db push --linked          # Apply migrations
npx supabase migration list            # List migrations
npx supabase functions deploy <name>   # Deploy Edge Function
```

## Build System

- **Gradle 8.12.1** with Kotlin DSL, configuration cache enabled, parallel builds
- **AGP 8.8.2**, **Kotlin 2.1.10**, **Java 21 toolchain**
- **compileSdk/targetSdk 36**, **minSdk 26**
- Version catalog at `gradle/libs.versions.toml`
- Convention plugins in `build-logic/convention/` (group: `com.vigipro.buildlogic`):
  - `vigipro.android.application` — app module setup
  - `vigipro.android.library` — library module setup
  - `vigipro.android.compose` — Compose BOM + dependencies
  - `vigipro.android.hilt` — KSP + Hilt wiring
  - `vigipro.android.feature` — applies library+compose+hilt, auto-adds `:core:core-ui`, `:core:core-model`, `:core:core-data`, Orbit MVI, navigation, lifecycle

## Architecture

### Module Structure

```
app/                    → Entry point (VigiProApp, MainActivity, NavHost)
core/
  core-model/           → Domain models (pure Kotlin + kotlinx.serialization)
  core-network/         → Supabase client (auth, postgrest, realtime, storage) via Ktor/OkHttp
  core-data/            → Room database + Supabase repositories + extensions
  core-ui/              → Material 3 theme, reusable Compose components + extensions
feature/
  feature-auth/         → Login/register with Supabase Auth
  feature-dashboard/    → Camera grid view with multi-site support
  feature-player/       → RTSP video player + PTZ control + fullscreen
  feature-devices/      → Device management, ONVIF discovery, QR scanning
  feature-access-control/ → Multi-tenant access control, invitations, QR codes
  feature-settings/     → App settings + account management
build-logic/convention/ → Gradle convention plugins
supabase/migrations/    → SQL migrations for Supabase
```

### Key Patterns

- **DI:** Dagger Hilt. `@HiltAndroidApp` on app, `@AndroidEntryPoint` on Activity. Hilt modules use `@InstallIn(SingletonComponent::class)`.
- **State Management:** Orbit MVI (orbit-core, orbit-compose, orbit-viewmodel) — auto-included in all feature modules via convention plugin.
- **Navigation:** Jetpack Navigation Compose with string routes. Start destination: `"login"`. Auth gate redirects to dashboard on valid session.
- **Networking:** Supabase SDK v3.1.2 over Ktor. `SUPABASE_URL` and `SUPABASE_KEY` injected via BuildConfig.
- **Persistence:** Room v2 (CameraEntity + SiteEntity, schema export to `core/core-data/schemas/`) + DataStore Preferences.
- **Offline-First:** Sites cached in Room, synced from Supabase Postgrest. Cameras remain local-only (RTSP credentials never leave device).
- **Serialization:** `kotlinx.serialization` with `@SerialName` for snake_case API field mapping.
- **Video:** Media3/ExoPlayer with RTSP support.
- **Camera Protocol:** ONVIF via `com.seanproctor:onvifcamera`.
- **QR:** ZXing for generation, ML Kit Barcode Scanning for reading.
- **Images:** Coil 3 with Ktor network backend.
- **Testing:** JUnit 4 + MockK + Turbine (Flow) + orbit-test (ViewModel MVI).

### Conventions

- Package: `com.vigipro.{layer}.{module}` (e.g., `com.vigipro.core.model`, `com.vigipro.feature.auth`)
- Feature modules only need `plugins { alias(libs.plugins.vigipro.android.feature) }` — convention plugin handles all wiring
- Composables: PascalCase, `modifier: Modifier = Modifier` as last parameter
- Trailing commas used consistently
- Non-transitive R classes enabled (`android.nonTransitiveRClass=true`)
- Edge-to-edge display enabled
- Deep linking configured for `https://vigipro.app/invite/`
- All UI strings hardcoded in Brazilian Portuguese

### Domain Model Hierarchy

```
Site → Camera (many), SiteMember (many)
SiteMember → CameraPermission (many), has Role (OWNER/ADMIN/VIEWER/TIME_RESTRICTED/GUEST)
Invitation → links to Site, defines time-scoped camera access via invite codes
Camera → has status (ONLINE/OFFLINE/ERROR), PTZ/audio capabilities
```

### Supabase Backend

- **Auth:** Email/password authentication with auto-session management
- **Postgrest:** REST API for sites, site_members, invitations tables
- **RLS:** Row Level Security policies enforce access control at database level
- **RPC:** `redeem_invitation(p_code)` — SECURITY DEFINER function for invite redemption
- **Migrations:** SQL files in `supabase/migrations/`, applied via `npx supabase db push --linked`

### Repository Layer

| Repository | Interface | Implementation | Persistence |
|---|---|---|---|
| AuthRepository | sessionState, signIn/Up/Out | SupabaseAuthRepository | Supabase Auth SDK |
| SiteRepository | getUserSites, createSite, syncSites | SupabaseSiteRepository | Room cache + Postgrest |
| InvitationRepository | create/delete/redeem invitations | SupabaseInvitationRepository | Postgrest only |
| CameraRepository | CRUD cameras, filter by site | LocalCameraRepository | Room only (local) |

### Extensions

- `core-data/extensions/`: ResultExtensions, FlowExtensions (ResultState, retryWithBackoff), StringExtensions (date formatting, validation)
- `core-ui/extensions/`: ModifierExtensions (shimmerEffect), ContextExtensions (shareText, copyToClipboard, maskEmail, maskRtspCredentials)
