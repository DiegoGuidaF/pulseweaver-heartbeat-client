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
    data class Success(val response: RegistrationResponse) : RegistrationResult
    data class Error(val message: String) : RegistrationResult
}

class RegistrationClient(
    private val client: HttpClient = HeartbeatClient.defaultClient(),
) {
    suspend fun claim(code: String): RegistrationResult {
        return try {
            val serverUrl = decodeServerURL(code)
            val url = serverUrl.trimEnd('/') + "/api/v1/register"
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody("""{"code":"$code"}""")
                timeout { requestTimeoutMillis = 15_000 }
            }
            when (response.status.value) {
                200, 201 -> RegistrationResult.Success(response.body())
                400 -> RegistrationResult.Error("Invalid registration code")
                404, 410 -> RegistrationResult.Error("Code expired or already used")
                else -> RegistrationResult.Error("Server error (${response.status.value})")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            RegistrationResult.Error("Invalid registration code")
        } catch (e: Exception) {
            RegistrationResult.Error("Connection failed")
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
