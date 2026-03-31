# PulseWeaver Heartbeat Client

Keep your devices authorized on your [PulseWeaver](https://github.com/DiegoGuidaF/PulseWeaver) server — automatically.

PulseWeaver is a self-hosted, IP-based dynamic firewall. It sits as an authorization middleware in front of your reverse proxy (e.g. Caddy) and decides whether to allow or deny requests based on the caller's IP. For an IP to be allowed, a device must **send periodic heartbeats** to prove it's still active.

This app does exactly that. Install it on a phone, laptop, or desktop and it keeps your IP registered in the background — no browser tabs, no Tasker hacks, no manual steps.

## Platforms

| Platform | Status | Background scheduling |
|----------|--------|-----------------------|
| Android  | ✅ Ready | WorkManager (survives doze) |
| Linux    | ✅ Ready | JVM timer + system tray |
| Windows  | ✅ Ready | JVM timer + system tray |
| macOS    | ✅ Ready | JVM timer + system tray |
| iOS      | 🚧 Planned | BGAppRefreshTask |

Single codebase — built with Kotlin Multiplatform and Compose Multiplatform.

## Features

- **Background heartbeats** — configurable interval, runs without user interaction
- **Network-aware** — detects connectivity changes and sends a heartbeat immediately when back online
- **Biometric lock** (Android) — optionally require fingerprint/face to view or change settings, with a 60-second grace period
- **System tray** (Desktop) — lives in the tray with a state-aware icon; no window needed after setup
- **Light / Dark / Auto theme** — follows your system preference or pick manually
- **Minimal permissions** — no accounts, no telemetry, no internet access beyond your own server

## Screenshots

<!-- Replace these with actual screenshots -->

| Android | Desktop |
|---------|---------|
| ![Android screenshot](docs/screenshot-android.png) | ![Desktop screenshot](docs/screenshot-desktop.png) |

## Getting started

### 1. Generate an API key

In the PulseWeaver dashboard, create a **device key** (starts with `wdk_...`). Each device needs its own key.

### 2. Install the app

Download the latest release for your platform from [GitHub Releases](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/releases):

| Platform | Artifact |
|----------|----------|
| Android  | `.apk` |
| Linux    | `.deb` / `.rpm` / AppImage |
| Windows  | `.msi` |
| macOS    | `.dmg` |

### 3. Configure

Open the app and fill in:

- **Server URL** — your PulseWeaver instance (e.g. `https://pw.example.com`)
- **API Key** — the `wdk_...` key from step 1
- **Interval** — how often to send heartbeats (default: 60 seconds)

Flip the switch to start. On desktop the app moves to the system tray.

> **Tip for mobile / dynamic IP devices:** Enable the **address lease** on the PulseWeaver server for that device and set the TTL slightly above the heartbeat interval (e.g. 20 min TTL for a 15 min interval). This way, if a heartbeat is delayed by a network switch or doze cycle, the IP doesn't expire prematurely.

## API

The client calls a single endpoint. No server-side changes are needed.

```
POST {server_url}/api/v1/heartbeat
Header: X-API-Key: wdk_...
Body:   (empty — server reads IP from the request)

200 — IP updated
201 — New IP registered
401 — Invalid API key
404 — Device not found
429 — Rate limited
```

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

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3.20 |
| UI | Compose Multiplatform 1.10.3 |
| HTTP | Ktor 3.4.2 |
| Build | Gradle with version catalog, AGP 9.1.0 |
| Tests | kotlin-test, kotlinx-coroutines-test, Ktor MockEngine, compose-ui-test |

## Contributing

Contributions are welcome. The project follows a spec-driven workflow — feature specs live in the [planning workspace](https://github.com/DiegoGuidaF/PulseWeaver) and are written before implementation starts.

## License

[AGPL-3.0](LICENSE)
