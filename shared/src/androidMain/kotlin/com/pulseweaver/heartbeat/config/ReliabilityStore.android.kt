package com.pulseweaver.heartbeat.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.pulseweaver.heartbeat.ApplicationContextHolder
import kotlinx.coroutines.flow.first

private val Context.reliabilityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "heartbeat_reliability",
)

actual class ReliabilityStore actual constructor() {
    private val ds get() = ApplicationContextHolder.context.reliabilityDataStore

    actual suspend fun load(): ReliabilityState {
        val prefs = ds.data.first()
        return ReliabilityState(promptSeen = prefs[Keys.PROMPT_SEEN] ?: false)
    }

    actual suspend fun save(state: ReliabilityState) {
        ds.edit { prefs -> prefs[Keys.PROMPT_SEEN] = state.promptSeen }
    }

    private object Keys {
        val PROMPT_SEEN = booleanPreferencesKey("prompt_seen")
    }
}
