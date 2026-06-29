package com.pulseweaver.heartbeat.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed interface RegistrationResult {
    data class Success(
        val response: RegistrationResponse,
    ) : RegistrationResult

    /**
     * [reason] carries the user-facing copy and the stable `PWC-PAIR-*`
     * diagnostic; [detail] holds supplementary admin context (e.g. the raw HTTP
     * status) that is shown alongside the code but is never part of it.
     */
    data class Error(
        val reason: PairingError,
        val detail: String? = null,
    ) : RegistrationResult
}

class RegistrationClient(
    private val client: HttpClient = HeartbeatClient.defaultClient(),
) {
    suspend fun claim(rawCode: String): RegistrationResult {
        val code = sanitizePairingCode(rawCode)
        val serverUrl =
            when (val check = validatePairingCode(code)) {
                is PairingCodeCheck.Valid -> check.serverUrl
                is PairingCodeCheck.Invalid -> return RegistrationResult.Error(check.reason)
            }
        return try {
            val url = serverUrl.trimEnd('/') + "/api/v1/device-pair"
            val response =
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody("""{"code":"$code"}""")
                    timeout { requestTimeoutMillis = 15_000 }
                }
            when (response.status.value) {
                200, 201 -> RegistrationResult.Success(response.body())
                400 -> RegistrationResult.Error(PairingError.REJECTED)
                404, 410 -> RegistrationResult.Error(PairingError.EXPIRED)
                else ->
                    RegistrationResult.Error(
                        PairingError.SERVER,
                        detail = "HTTP ${response.status.value}",
                    )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RegistrationResult.Error(PairingError.NETWORK)
        }
    }

    companion object {
        /**
         * Decodes a registration code produced by the server.
         *
         * The code is base64url (no padding) encoding of:
         *   bytes[0..31]  — raw token (32 bytes, opaque to the client)
         *   bytes[32..]   — UTF-8 server URL
         *
         * Returns serverUrl.
         * Throws [IllegalArgumentException] if the payload is < 33 bytes.
         */
        @OptIn(ExperimentalEncodingApi::class)
        fun decodeServerURL(code: String): String {
            val decoded = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(code)
            require(decoded.size >= 33) {
                "Registration code payload too short: ${decoded.size} bytes (minimum 33)"
            }
            return decoded.copyOfRange(32, decoded.size).decodeToString()
        }
    }
}
