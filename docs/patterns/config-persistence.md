# Config Persistence

All user configuration is stored via the `ConfigStore` expect/actual class. Platform implementations use the native persistence mechanism.

## HeartbeatConfig Data Class

**File:** `shared/src/commonMain/kotlin/com/pulseweaver/heartbeat/config/HeartbeatConfig.kt`

```kotlin
data class HeartbeatConfig(
    val serverUrl: String = "",
    val apiKey: String = "",
    val intervalSeconds: Int = 900,
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val settingsLocked: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO,
)
```

All fields have defaults — a fresh `HeartbeatConfig()` is always safe to use.

## ConfigStore expect/actual

**Contract:** `shared/src/commonMain/.../config/ConfigStore.kt`
```kotlin
expect class ConfigStore() {
    suspend fun load(): HeartbeatConfig
    suspend fun save(config: HeartbeatConfig)
}
```

### Platform Implementations

| Platform | File | Mechanism | Key format |
|----------|------|-----------|------------|
| Android | `androidMain/.../ConfigStore.android.kt` | Jetpack DataStore Preferences | `snake_case` (e.g. `server_url`, `api_key`, `settings_locked`) |
| JVM/Desktop | `jvmMain/.../ConfigStore.jvm.kt` | `java.util.prefs.Preferences` at node `com/pulseweaver/heartbeat` | `camelCase` (e.g. `serverUrl`, `apiKey`, `settingsLocked`) |
| iOS | `iosMain/.../ConfigStore.ios.kt` | Stub (returns default, no-op save) | N/A |

### Android Details
- DataStore name: `"heartbeat_config"`
- Context obtained via `ApplicationContextHolder.context` singleton
- `ThemeMode` stored as string name (`AUTO`/`LIGHT`/`DARK`) with `runCatching` fallback
- Keys are defined in a private `Keys` object using typed preference key constructors

### JVM/Desktop Details
- Uses `Preferences.userRoot().node("com/pulseweaver/heartbeat")`
- `biometricEnabled` is hardcoded to `false` (not applicable on desktop)
- `prefs.flush()` is called after save

## ResultStore

Separate expect/actual class for persisting the last heartbeat result (`LastHeartbeatState`). Same platform pattern as `ConfigStore`:
- Android: DataStore Preferences (`"heartbeat_result"`)
- JVM: `java.util.prefs.Preferences`
- iOS: Stub

## Adding a New Config Field

1. Add the field to `HeartbeatConfig` with a backward-compatible default value
2. Update **Android** `ConfigStore`: add key to `Keys` object, read in `load()`, write in `save()`
3. Update **JVM** `ConfigStore`: add `prefs.get*()`/`prefs.put*()` calls
4. **iOS**: no change needed (stub returns `HeartbeatConfig()` which uses default)
5. If the field affects scheduling or network behavior, update the relevant `LaunchedEffect` in `HeartbeatScreen`

## Auto-save Behavior

Config is auto-saved in `HeartbeatScreen` via:
```kotlin
LaunchedEffect(config) {
    if (!isLoaded || config == lastSavedConfig) return@LaunchedEffect
    delay(500)  // debounce
    configStore.save(config)
    lastSavedConfig = config
    // show "Saved ✓" for 2s
}
```

---
**Verified against:** `ConfigStore.kt` (expect), `ConfigStore.android.kt`, `ConfigStore.jvm.kt`, `ConfigStore.ios.kt`
**Applies to:** any config change or new persistent setting
**Known gaps:** iOS ConfigStore is a stub
**Last verified:** 2026-04-16
