# Metabind App Android

An Android demo app that renders dynamic UI components from the Metabind service. The app uses the in-tree Metabind Content and Assistant SDKs to interpret and display SwiftUI-like declarative component descriptions natively with Jetpack Compose.

## Features

- **Dynamic UI rendering** via the Metabind library with real-time subscription support
- **QR code scanning** to load components by link (CameraX + ML Kit)
- **Recents list** with local Room database persistence and swipe-to-dismiss
- **Deep linking** support (`ai.metabind://app/`)
- **Edge-to-edge** UI with Material3 theming

## MCP project previews

In Composer, generate a preview QR from a project’s Connect screen. In the app,
open Preview and scan the QR. The included project key is encrypted with Android
Keystore and kept outside Android backup; the recents database stores only a
credential-free URL. No phone login or separate key entry is required. The project
must already have an assistant provider configured.

On an emulator or a device without a camera, Preview opens a URL field directly.
Physical devices also offer **Paste Preview Link**, including when camera access
is denied. Removing a project from Recents removes its locally saved access.

The assistant opens ready to chat against saved, unpublished drafts. While the
preview is resumed, it checks for updated tool definitions and UI resources every
three seconds between turns. Existing messages, tool arguments, and results stay
in place; refresh does not call tools again. Changes to a component’s structure may
reset its local UI state. Server cache lifetimes affect when saved changes appear.

Production and development links select matching MCP and Agent services. The QR
format is unchanged: an HTTPS `/preview/mcp?url=<encoded MCP project endpoint>`
wrapper with a `#key=<project key>&name=<optional title>` fragment. Only trusted
Metabind hosts and canonical project IDs are accepted. Do not put keys in a query,
logs, source files, or screenshots.

### Local validation

```sh
./gradlew :app:assembleDebug :data-home:testDebugUnitTest \
  :metabind-android:metabindai:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

`PreviewCredentialsTest` checks Keystore persistence, environment isolation,
ciphertext storage, and tamper rejection. `LiveMCPPreviewTest` is opt-in and skips
without a locally supplied Finance preview link in the debug app’s cache file
`mcp-preview-test-link`. It deletes the file after reading it, imports through the
UI, asks read-only subscriptions and spending questions, and reopens saved access.
Supply the optional instrumentation argument `projectTitle` if the project has a
different title. The separate `reopenSavedPreviewAfterProcessRestart` test runs
only with `reopenSavedPreview=true` after the preview has been imported.

Physical camera scanning, newly generated QR access/revocation, and live edits
against deployed freshness changes require separate end-to-end validation.

## Architecture

Multi-module Gradle project:

```
app/                  → Main activity, navigation, app entry point
├── base-ui/          → Shared UI components and utilities
├── base-theme/       → Jetpack Compose theming (Material3)
├── feature-home/     → Screens: Recents, Detail, Preview, ScanLink
├── data-home/        → Room database, repositories, models
└── dynamicfeature/   → Dynamic feature module
```

## Building

> **Note**: JAVA_HOME must point to a JDK 21+ installation. Check `local.properties` or environment variables if you encounter errors.

```bash
./gradlew assembleDebug       # Debug build
./gradlew assembleRelease     # Release build
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumentation tests
```

## Key Technologies

- **Kotlin 2.3** / **Jetpack Compose 1.9** / **Material3**
- **Metabind** library for dynamic component rendering
- **Dagger Hilt** for dependency injection
- **Room** for local persistence
- **CameraX** + **ML Kit** for barcode scanning
- **Coil** for image loading
- **Media3/ExoPlayer** for video playback
