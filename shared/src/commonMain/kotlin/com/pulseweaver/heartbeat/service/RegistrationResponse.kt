package com.pulseweaver.heartbeat.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server response from POST /api/v1/device-pair (200).
 * Contains all config fields needed to fully provision the app.
 */
@Serializable
data class RegistrationResponse(
    @SerialName("server_url") val serverUrl: String,
    @SerialName("api_key") val apiKey: String,
    @SerialName("interval_seconds") val intervalSeconds: Int,
    @SerialName("app_biometric_enabled") val appBiometricEnabled: Boolean,
    @SerialName("app_settings_locked") val appSettingsLocked: Boolean,
)
