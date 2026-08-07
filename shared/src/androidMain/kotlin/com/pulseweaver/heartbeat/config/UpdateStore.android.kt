package com.pulseweaver.heartbeat.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulseweaver.heartbeat.ApplicationContextHolder
import kotlinx.coroutines.flow.first

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "heartbeat_update",
)

actual class UpdateStore actual constructor() {
    private val ds get() = ApplicationContextHolder.context.updateDataStore

    actual suspend fun load(): UpdateState {
        val prefs = ds.data.first()
        return UpdateState(
            lastCheckedAtMs = prefs[Keys.LAST_CHECKED_AT_MS] ?: 0L,
            skippedVersion = prefs[Keys.SKIPPED_VERSION] ?: "",
        )
    }

    actual suspend fun save(state: UpdateState) {
        ds.edit { prefs ->
            prefs[Keys.LAST_CHECKED_AT_MS] = state.lastCheckedAtMs
            prefs[Keys.SKIPPED_VERSION] = state.skippedVersion
        }
    }

    private object Keys {
        val LAST_CHECKED_AT_MS = longPreferencesKey("last_checked_at_ms")
        val SKIPPED_VERSION = stringPreferencesKey("skipped_version")
    }
}
