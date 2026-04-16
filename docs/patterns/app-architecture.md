# App Architecture & Screen Routing

The app is a single-activity Compose Multiplatform application with no navigation library. Screen routing is handled by conditional composable rendering in `App.kt`.

## Composable Hierarchy

```
App()                           — MaterialTheme, screen routing
├── SetupScreen()               — shown when unconfigured (no valid serverUrl + apiKey)
└── AuthGate {                  — biometric lock gate (mobile only)
      HeartbeatScreen()         — main screen, all content in one scrollable Column
    }
```

## Screen Routing

`App.kt` uses a `ScreenState` sealed interface to decide what to show:
- `Loading` → blank (avoids config-load flash)
- `Setup` → SetupScreen (device provisioning / first-run)
- `Main` → AuthGate wrapping HeartbeatScreen

The initial state is determined by loading config from `ConfigStore` and checking `HeartbeatUtils.isConfigValid(serverUrl, apiKey)`. AuthGate only wraps the Main path — the setup screen should not be gated by biometrics since the user hasn't configured biometric yet.

## HeartbeatScreen Layout

All UI lives on a single scrollable `Column` inside a `Scaffold`. Cards are independent composables:

| Card | Purpose | Always shown? |
|------|---------|---------------|
| StatusHero | Pulsing status circle, IP, countdown | Yes |
| ConnectionCard | Server URL + API key (collapsible) | Yes (collapsed when valid) |
| ScheduleCard | Interval chips + heartbeat toggle | Yes |
| LastResultCard | Last HTTP response details | Only after first heartbeat |
| Actions row | Send Now (desktop) + Open Server URL | Yes |
| SecurityCard | Biometric toggle | Mobile only (`BiometricAuth.isAvailable()`) |
| AppearanceCard | Theme mode chips (Auto/Light/Dark) | Yes |

## State Management

- All state is `var ... by remember { mutableStateOf(...) }` — no ViewModel, no state holder class.
- Config auto-saves with a 500ms debounce via `LaunchedEffect(config)`.
- `lastSavedConfig` prevents the "Saved ✓" indicator from flashing on initial load.
- The heartbeat scheduler and network monitor are started/stopped inside `LaunchedEffect` blocks based on `config.enabled`.

## Key Design Decisions

- **No navigation library**: The app has 2 screens — `ScreenState` sealed interface is sufficient. Adding Voyager/Decompose would be over-engineering.
- **No ViewModel**: State lives in composable scope. The app is simple enough that a ViewModel would add indirection without benefit.
- **No DI framework**: Dependencies are constructed via `remember { ... }` in composables and constructor injection for testability.

---
**Verified against:** `App.kt`, `HeartbeatScreen.kt`, `AuthGate.kt`
**Applies to:** any new screen or major UI change
**Known gaps:** none
**Last verified:** 2026-04-16
