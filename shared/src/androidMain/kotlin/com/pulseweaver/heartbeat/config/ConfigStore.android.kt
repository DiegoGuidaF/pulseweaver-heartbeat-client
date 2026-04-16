package com.pulseweaver.heartbeat.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulseweaver.heartbeat.ApplicationContextHolder
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "heartbeat_config"
)

// Security note: The API key is stored in DataStore (app-private internal storage).
// On Android 10+ this is protected by OS-level file-based encryption. On older devices
// (API 24–28) it is protected by the app sandbox.
actual class ConfigStore actual constructor() {

    private val ds get() = ApplicationContextHolder.context.dataStore

    actual suspend fun load(): HeartbeatConfig {
        val prefs = ds.data.first()
        return HeartbeatConfig(
            serverUrl = prefs[Keys.SERVER_URL] ?: "",
            apiKey = prefs[Keys.API_KEY] ?: "",
            intervalSeconds = prefs[Keys.INTERVAL_SECONDS] ?: 900,
            enabled = prefs[Keys.ENABLED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.AUTO) }
                ?: ThemeMode.AUTO,
            settingsLocked = prefs[Keys.SETTINGS_LOCKED] ?: false,
        )
    }

    actual suspend fun save(config: HeartbeatConfig) {
        ds.edit { prefs ->
            prefs[Keys.SERVER_URL] = config.serverUrl
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.INTERVAL_SECONDS] = config.intervalSeconds
            prefs[Keys.ENABLED] = config.enabled
            prefs[Keys.BIOMETRIC_ENABLED] = config.biometricEnabled
            prefs[Keys.THEME_MODE] = config.themeMode.name
            prefs[Keys.SETTINGS_LOCKED] = config.settingsLocked
        }
    }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val INTERVAL_SECONDS = intPreferencesKey("interval_seconds")
        val ENABLED = booleanPreferencesKey("enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SETTINGS_LOCKED = booleanPreferencesKey("settings_locked")
    }
}

