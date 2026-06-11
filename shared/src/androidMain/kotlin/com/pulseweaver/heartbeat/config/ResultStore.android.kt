package com.pulseweaver.heartbeat.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pulseweaver.heartbeat.ApplicationContextHolder
import com.pulseweaver.heartbeat.service.HeartbeatResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.resultDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "heartbeat_result",
)

actual class ResultStore actual constructor() {
    private val ds get() = ApplicationContextHolder.context.resultDataStore

    actual suspend fun load(): LastHeartbeatState? = ds.data.first().toState()

    actual fun observe(): Flow<LastHeartbeatState?> = ds.data.map { it.toState() }

    private fun Preferences.toState(): LastHeartbeatState? {
        val success = this[Keys.SUCCESS] ?: return null
        return LastHeartbeatState(
            result =
                HeartbeatResult(
                    success = success,
                    message = this[Keys.MESSAGE] ?: "",
                    hint = this[Keys.HINT],
                    ip = this[Keys.IP],
                    trigger = this[Keys.TRIGGER] ?: "",
                ),
            time = this[Keys.TIME] ?: "",
            epochMs = this[Keys.EPOCH_MS] ?: 0L,
        )
    }

    actual suspend fun save(
        result: HeartbeatResult,
        time: String,
        epochMs: Long,
    ) {
        ds.edit { prefs ->
            prefs[Keys.SUCCESS] = result.success
            prefs[Keys.MESSAGE] = result.message
            if (result.hint != null) prefs[Keys.HINT] = result.hint else prefs.remove(Keys.HINT)
            if (result.ip != null) prefs[Keys.IP] = result.ip else prefs.remove(Keys.IP)
            prefs[Keys.TRIGGER] = result.trigger
            prefs[Keys.TIME] = time
            prefs[Keys.EPOCH_MS] = epochMs
        }
    }

    private object Keys {
        val SUCCESS = booleanPreferencesKey("last_success")
        val MESSAGE = stringPreferencesKey("last_message")
        val HINT = stringPreferencesKey("last_hint")
        val IP = stringPreferencesKey("last_ip")
        val TRIGGER = stringPreferencesKey("last_trigger")
        val TIME = stringPreferencesKey("last_time")
        val EPOCH_MS = longPreferencesKey("last_epoch_ms")
    }
}
