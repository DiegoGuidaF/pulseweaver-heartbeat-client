# Heartbeat Client — Implementation Progress

> Feature spec: `planning/features/phase-1-heartbeat-client.md`
> Framework decision: `planning/adr/005-heartbeat-client-framework.md`

## Stage 1+2: Scaffold & Shared Core — DONE (2026-03-30)

### Approach

Rebuilt from the **official JetBrains KMP-App-Template**, updated to latest dependency
versions and adapted for PulseWeaver. Uses the template's multi-module layout
(`shared` + `androidApp`) with desktop support added.

### Toolchain versions

| Tool | Version |
|---|---|
| Kotlin | 2.3.20 |
| Compose Multiplatform | 1.10.3 |
| AGP | 9.1.0 |
| Gradle | 9.3.1 |
| Ktor | 3.4.2 |
| kotlinx-serialization | 1.10.0 |
| kotlinx-coroutines | 1.10.2 |
| ktlint | 12.2.0 |
| Compose Hot Reload | 1.0.0 |

### Project structure (multi-module, from KMP-App-Template)

```
heartbeat-client/
├── shared/                            # KMP shared library (all platforms)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/pulseweaver/heartbeat/
│       │   ├── App.kt                          # root Composable (placeholder)
│       │   ├── config/HeartbeatConfig.kt       # config data class
│       │   ├── service/
│       │   │   ├── AddressResponse.kt          # @Serializable server response
│       │   │   ├── HeartbeatClient.kt          # Ktor HTTP client
│       │   │   └── HeartbeatResult.kt          # result data class
│       │   ├── platform/                       # expect/actual (Stage 3)
│       │   └── ui/                             # UI composables (Stage 3)
│       ├── commonTest/kotlin/                  # test infrastructure
│       ├── androidMain/kotlin/                 # Android-specific code
│       ├── iosMain/kotlin/.../MainViewController.kt
│       └── jvmMain/kotlin/.../main.kt          # desktop entry point
├── androidApp/                        # thin Android wrapper
│   ├── build.gradle.kts
│   └── src/main/{kotlin,res,AndroidManifest.xml}
├── iosApp/                            # Xcode project wrapper
├── .github/workflows/                 # CI (Android, iOS, Desktop)
├── build.gradle.kts                   # root (plugin declarations)
├── settings.gradle.kts
├── gradle.properties
└── gradle/libs.versions.toml          # version catalog
```

Key differences from previous scaffold:
- **Multi-module layout**: `shared` (KMP library) + `androidApp` (Android wrapper)
  instead of single `composeApp` module. Cleaner separation.
- **AGP 9.1.0 with `androidMultiplatformLibrary`**: Resolves the AGP 9.0/KMP
  incompatibility. The `com.android.kotlin.multiplatform.library` plugin works
  correctly with `org.jetbrains.kotlin.multiplatform`.
- **Desktop target is `jvm()`** — source set is `jvmMain`, not `desktopMain`.
- **iOS framework name remains `Shared`** — matches template convention.

### Build targets

| Platform | Target | Source set | HTTP Engine | Status |
|---|---|---|---|---|
| Android | android | androidMain | OkHttp | ✅ |
| iOS (arm64 + sim) | iosArm64, iosSimulatorArm64 | iosMain | Darwin | ✅ |
| Desktop (macOS/Linux/Windows) | jvm | jvmMain | CIO | ✅ |

### Deviations from spec

1. **Multi-module `shared` + `androidApp`** instead of single `composeApp`.
2. **Desktop target is `jvm()`** — source set is `jvmMain`, not `desktopMain`.
3. **AGP 9.1.0** with `androidMultiplatformLibrary` — resolves AGP/KMP incompatibility.
4. **`AddressResponse` has full server fields**, not just `ip`. Matches actual server response.
5. **No `expect`/`actual` declarations yet.** Deferred to Stage 3/4.
6. **No `kotlinx-datetime`**. `HeartbeatResult` has no timestamp field — UI tracks time.
7. **Compose Hot Reload plugin included** for fast iteration in Stage 3.

## Stage 3: Desktop Target — DONE (2026-03-30)

**Scope:** `expect`/`actual` declarations + desktop implementations + full Compose UI.

### What was implemented

| Component | File(s) | Notes |
|---|---|---|
| `expect`/`actual` contracts | `commonMain/config/ConfigStore.kt`, `commonMain/platform/*.kt` | ConfigStore, BackgroundScheduler, NetworkMonitor, BiometricAuth, UrlOpener, PlatformUtils |
| Desktop ConfigStore | `jvmMain/config/ConfigStore.jvm.kt` | `java.util.prefs.Preferences` at `com/pulseweaver/heartbeat` |
| Desktop BackgroundScheduler | `jvmMain/platform/BackgroundScheduler.jvm.kt` | App-scoped coroutine timer; interval restarts cleanly on reschedule |
| Desktop NetworkMonitor | `jvmMain/platform/NetworkMonitor.jvm.kt` | Polls `NetworkInterface` every 30 s on IO dispatcher |
| Desktop UrlOpener | `jvmMain/platform/UrlOpener.jvm.kt` | `java.awt.Desktop.browse()` |
| BiometricAuth (desktop) | `jvmMain/platform/BiometricAuth.jvm.kt` | Always unavailable; authenticate() returns true (passthrough) |
| Android stubs | `androidMain/**` | Compilable no-ops; Stage 4 implements WorkManager, DataStore, EncryptedSharedPreferences |
| iOS stubs | `iosMain/**` | Compilable no-ops; Stage later |
| `HeartbeatScreen` | `commonMain/ui/HeartbeatScreen.kt` | Full spec UI: status banner, URL/key fields, interval chips, toggle, last-result row, Send Now + Open Server URL, biometric toggle (hidden desktop), onboarding hint, background hint |
| `AuthGate` | `commonMain/ui/AuthGate.kt` | Passthrough on desktop; mobile gate in Stage 4 |
| `App.kt` | `commonMain/App.kt` | Takes `BackgroundScheduler` + `onLastResultChange`; dark/light MaterialTheme |
| System tray + window | `jvmMain/main.kt` | Minimize-to-tray on close, tray menu (status/show/quit), programmatic green dot icon |
| Manifest | `androidApp/AndroidManifest.xml` | INTERNET, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, USE_BIOMETRIC |

### Key design decisions

- **`BackgroundScheduler` constructed at entry point** — desktop: `BackgroundScheduler(appScope)`, Android/iOS: `BackgroundScheduler()`. `App()` receives it as a parameter.
- **`ConfigStore`/`NetworkMonitor` no-arg constructors** — `remember { ConfigStore() }` inside `HeartbeatScreen`.
- **Auto-save debounce** — `LaunchedEffect(config) { delay(500); configStore.save(config) }`.
- **Network change fires immediate heartbeat** — `NetworkMonitor.startMonitoring { coroutineScope.launch { sendHeartbeat("network_change") } }`.
- **`-Xexpect-actual-classes`** compiler flag suppresses Beta warning (stable in Kotlin 2.x).

### Build verification

- [x] `./gradlew :shared:compileKotlinJvm` — PASS, 0 warnings
- [x] `./gradlew :androidApp:assembleDebug` — PASS, 0 warnings

## Stage 4: Android Target — NEXT

**Scope:** WorkManager, ConnectivityManager, DataStore, EncryptedSharedPreferences, androidx.biometric.

**Spec reference:** `planning/features/phase-1-heartbeat-client.md` lines 86–94, 176–193, 213–238.

**Key decisions to make in Stage 4:**
- `ConfigStore` Android actual needs `Context`. Pattern: `ApplicationContextHolder` singleton set in Application class.
- `BackgroundScheduler` actual will take `context: Context` in its constructor (breaking the no-arg stub). Entry point (MainActivity) passes `this` or `applicationContext`.
- `HeartbeatWorker` extends `CoroutineWorker` — full access to EncryptedSharedPreferences.
- Battery optimization prompt: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

