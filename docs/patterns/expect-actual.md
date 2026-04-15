# Expect/Actual Platform Abstraction

Kotlin Multiplatform uses `expect`/`actual` declarations to abstract platform-specific behavior. All shared logic lives in `commonMain`; platform implementations go in `androidMain`, `jvmMain`, `iosMain`.

## Pattern

```kotlin
// commonMain — declare the contract
expect class ConfigStore {
    fun save(config: HeartbeatConfig)
    fun load(): HeartbeatConfig?
}

// androidMain — Android implementation
actual class ConfigStore(private val context: Context) {
    actual fun save(config: HeartbeatConfig) { /* SharedPreferences */ }
    actual fun load(): HeartbeatConfig? { /* SharedPreferences */ }
}

// jvmMain — Desktop implementation
actual class ConfigStore {
    actual fun save(config: HeartbeatConfig) { /* java.util.prefs or file */ }
    actual fun load(): HeartbeatConfig? { /* java.util.prefs or file */ }
}
```

## Existing expect/actual abstractions

| Abstraction | Package | Purpose |
|-------------|---------|---------|
| `ConfigStore` | `config/` | Persistent configuration storage |
| `BackgroundScheduler` | `platform/` | Periodic heartbeat scheduling |
| `NetworkMonitor` | `platform/` | Network connectivity detection |
| `BiometricAuth` | `platform/` | Biometric/PIN authentication gate |

## Key rules

- **`commonMain` is the default** — write everything there unless it requires a platform API.
- **Keep expect declarations minimal** — expose only the contract the common code needs, not the full platform API surface.
- **One `actual` per target** — `androidMain`, `jvmMain`, `iosMain`. Missing actuals cause compile errors.

---
**Verified against:** `shared/src/commonMain/.../platform/`, `shared/src/androidMain/.../platform/`
**Applies to:** any new platform-specific feature
**Known gaps:** none
**Last verified:** 2026-04-15
