# Kotlin App — Full Documentation

The Kotlin Multiplatform app is the full-featured heartbeat client for phones and desktops. It runs in the background, handles network changes, and requires no manual intervention after setup.

## Features

- **Background heartbeats** — configurable interval, runs without user interaction
- **Network-aware** — detects connectivity changes and sends a heartbeat immediately when back online
- **Biometric lock** (Android) — optionally require fingerprint/face to view or change settings, with a 60-second grace period
- **System tray** (Desktop) — lives in the tray with a state-aware icon; no window needed after setup
- **Light / Dark / Auto theme** — follows your system preference or pick manually
- **Minimal permissions** — no accounts, no telemetry, no internet access beyond your own server

## Supported platforms

| Platform | Status      | Background scheduling       |
|----------|-------------|-----------------------------|
| Android  | ✅ Ready    | WorkManager (survives doze) |
| Linux    | ✅ Ready    | JVM timer + system tray     |
| Windows  | ✅ Ready    | JVM timer + system tray     |
| macOS    | ✅ Ready    | JVM timer + system tray     |
| iOS      | 🚧 Planned  | BGAppRefreshTask             |

## Screenshots

<!-- Replace these with actual screenshots -->

| Android | Desktop |
|---------|---------|
| ![Android screenshot](screenshot-android.png) | ![Desktop screenshot](screenshot-desktop.png) |

## Installing

Download the latest release for your platform from [GitHub Releases](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/releases):

| Platform | Artifact                   |
|----------|----------------------------|
| Android  | `.apk`                     |
| Linux    | `.deb` / `.rpm` / AppImage |
| Windows  | `.msi`                     |
| macOS    | `.dmg`                     |

## Configuring

Open the app and fill in:

- **Server URL** — your PulseWeaver instance (e.g. `https://pw.example.com`)
- **API Key** — the `wdk_...` key from the PulseWeaver dashboard
- **Interval** — how often to send heartbeats (default: 60 seconds)

Flip the switch to start. On desktop the app moves to the system tray.

> **Tip for mobile / dynamic IP devices:** Enable the **address lease** on the PulseWeaver server for that device and set the TTL slightly above the heartbeat interval (e.g. 20 min TTL for a 15 min interval). This way, if a heartbeat is delayed by a network switch or doze cycle, the IP doesn't expire prematurely.

## Building from source

Requires **JDK 17+** and the Android SDK (for Android builds).

```bash
# Run tests
./gradlew shared:jvmTest

# Desktop app (current OS)
./gradlew shared:run

# Android APK
./gradlew androidApp:assembleRelease

# Native desktop installers
./gradlew shared:packageDmg          # macOS
./gradlew shared:packageMsi          # Windows
./gradlew shared:packageDeb          # Linux .deb
./gradlew shared:packageRpm          # Linux .rpm
```

## Tech stack

| Layer    | Technology                                                             |
|----------|------------------------------------------------------------------------|
| Language | Kotlin 2.3.20                                                          |
| UI       | Compose Multiplatform 1.10.3                                           |
| HTTP     | Ktor 3.4.2                                                             |
| Build    | Gradle with version catalog, AGP 9.1.0                                 |
| Tests    | kotlin-test, kotlinx-coroutines-test, Ktor MockEngine, compose-ui-test |
