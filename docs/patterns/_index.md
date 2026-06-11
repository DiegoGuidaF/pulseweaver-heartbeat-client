# Heartbeat Client Pattern Index

> Before implementing a feature, scan this index and read every pattern that applies.
> After implementing, check the [self-improvement protocol](../../project/workflow/WORKFLOW.md#pattern-maintenance).

| Pattern | File | Use when | Avoid when | Refs |
|---------|------|----------|------------|------|
| App architecture & screen routing | `app-architecture.md` | Adding a new screen, changing navigation, modifying composable hierarchy | Small changes within an existing card | `App.kt`, `HeartbeatScreen.kt` |
| Config persistence | `config-persistence.md` | Adding a new config field, changing storage behavior | Reading config (just use `ConfigStore().load()`) | `config/HeartbeatConfig.kt`, `ConfigStore.*.kt` |
| Expect/actual platform abstraction | `expect-actual.md` | Adding platform-specific behavior (scheduling, biometrics, storage) | Pure business logic that works in commonMain | `platform/BackgroundScheduler.kt`, `config/ConfigStore.kt` |
| Ktor HTTP client | `ktor-http-client.md` | Making HTTP requests, adding new API endpoints | UI code or platform scheduling | `service/HeartbeatClient.kt` |
| Testing | `testing.md` | Writing unit tests, HTTP tests, or UI tests | — | `commonTest/`, `jvmTest/` |
| Android validation & platform behaviour | `android/` | Validating Android-only background behaviour on a device/emulator (Doze, battery optimization, WorkManager, network callbacks) | commonMain logic or desktop UI — use `testing.md` | `androidApp/`, `shared/src/androidMain/` |
| UI theming & component style | `ui-theming.md` | Creating new UI composables, cards, or styled elements | Backend/service code | `App.kt`, `HeartbeatScreen.kt` |
