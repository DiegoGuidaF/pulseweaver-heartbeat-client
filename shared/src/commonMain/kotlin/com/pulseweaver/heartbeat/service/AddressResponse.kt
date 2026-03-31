package com.pulseweaver.heartbeat.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server response from POST /api/v1/heartbeat (200/201).
 * Mirrors the server's Address model. Only [ip] is used by the client;
 * the rest is deserialized but ignored.
 */
@Serializable
data class AddressResponse(
    val id: Long,
    @SerialName("device_id") val deviceId: Long,
    val ip: String,
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)
