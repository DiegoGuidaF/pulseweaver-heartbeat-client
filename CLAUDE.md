# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PulseWeaver Heartbeat Client — a Kotlin Multiplatform app that keeps devices authorized on a PulseWeaver server by sending periodic `POST /api/v1/heartbeat` requests. Single core feature; everything else (scheduling, network-aware retries, biometric lock, system tray) is usability around that.

## Commands

All commands run from the repo root. Requires **JDK 17+** (Android builds also need the Android SDK).

### Build & Run
- `./gradlew shared:run` — run desktop app (current OS)
- `./gradlew androidApp:assembleDebug` — build Android debug APK

### Test
- `./gradlew shared:jvmTest` — run JVM tests (unit + compose UI tests)
- `./gradlew shared:allTests` — run tests on all available targets

### Lint
- `./gradlew ktlintCheck` — check Kotlin code style
- `./gradlew ktlintFormat` — auto-fix code style

### Package
- `./gradlew shared:packageDmg` — macOS installer
- `./gradlew shared:packageMsi` — Windows installer
- `./gradlew shared:packageDeb` — Linux .deb

## Architecture

Kotlin Multiplatform with Compose Multiplatform UI. Two Gradle modules:

- **`:shared`** — all application code (common + platform source sets)
- **`:androidApp`** — thin Android entry point wrapping `:shared`

### Source sets (`shared/src/`)

| Source set | Purpose |
|---|---|
| `commonMain` | UI, business logic, HTTP client, config — the bulk of the app |
| `androidMain` | WorkManager scheduler, biometric auth, Android config store |
| `jvmMain` | Desktop entry point, system tray, JVM timer scheduler |
| `iosMain` | iOS HTTP engine (Ktor Darwin) |
| `commonTest` | Unit tests, Ktor MockEngine tests, compose UI tests |
| `jvmTest` | Desktop-specific test support |

### Key packages (`com.pulseweaver.heartbeat`)

| Package | What it contains |
|---|---|
| `service/` | `HeartbeatClient` (Ktor HTTP), result types |
| `config/` | `HeartbeatConfig` data class, `ConfigStore` expect/actual |
| `platform/` | `BackgroundScheduler`, `NetworkMonitor`, `BiometricAuth` (expect/actual) |
| `ui/` | Compose screens (`HeartbeatScreen`, `AuthGate`) |

### Tech stack
- Kotlin 2.3.20, Compose Multiplatform 1.10.3, Ktor 3.4.2
- Version catalog: `gradle/libs.versions.toml`
- Code style: ktlint

## Conventions

- **expect/actual** for platform abstractions — keep `commonMain` as the default working location
- Tests use Ktor `MockEngine` for HTTP and `compose-ui-test` for UI
- Keep the app simple: one feature (heartbeat), minimal surface area
