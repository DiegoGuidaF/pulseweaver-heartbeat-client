package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.config.HeartbeatConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartbeatClientTest {

    private val validConfig = HeartbeatConfig(
        serverUrl = "https://pulse.example.com",
        apiKey = "test-key-42",
        intervalSeconds = 300,
        enabled = true,
    )

    private val sampleSuccessBody = """
        {
          "id": 1,
          "device_id": 10,
          "ip": "93.184.216.34",
          "is_enabled": true,
          "created_at": "2025-01-01T00:00:00Z",
          "updated_at": "2025-06-15T12:00:00Z"
        }
    """.trimIndent()

    private val jsonHeaders = headersOf(
        HttpHeaders.ContentType, ContentType.Application.Json.toString()
    )

    /** Build a [HeartbeatClient] backed by a mock engine that captures requests. */
    private fun mockClient(
        handler: suspend (url: String, method: HttpMethod, headers: io.ktor.http.Headers) ->
            Triple<String, HttpStatusCode, io.ktor.http.Headers>,
    ): HeartbeatClient {
        val engine = MockEngine { request ->
            val (body, status, responseHeaders) = handler(
                request.url.toString(),
                request.method,
                request.headers,
            )
            respond(body, status, responseHeaders)
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return HeartbeatClient(httpClient)
    }

    /** Convenience: mock that always returns a fixed response. */
    private fun mockClientResponse(
        body: String = sampleSuccessBody,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HeartbeatClient {
        val engine = MockEngine {
            respond(body, status, jsonHeaders)
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return HeartbeatClient(httpClient)
    }

    // ── Request shape ───────────────────────────────────────────────

    @Test
    fun send_constructsCorrectUrlFromConfig() = runTest {
        var capturedUrl = ""
        val client = mockClient { url, _, _ ->
            capturedUrl = url
            Triple(sampleSuccessBody, HttpStatusCode.OK, jsonHeaders)
        }

        client.send(validConfig, "scheduled")

        assertEquals("https://pulse.example.com/api/v1/heartbeat", capturedUrl)
    }

    @Test
    fun send_trailingSlashInUrl_isNormalised() = runTest {
        var capturedUrl = ""
        val config = validConfig.copy(serverUrl = "https://pulse.example.com/")
        val client = mockClient { url, _, _ ->
            capturedUrl = url
            Triple(sampleSuccessBody, HttpStatusCode.OK, jsonHeaders)
        }

        client.send(config, "scheduled")

        assertEquals("https://pulse.example.com/api/v1/heartbeat", capturedUrl)
    }

    @Test
    fun send_sendsApiKeyHeader() = runTest {
        var capturedApiKey: String? = null
        val client = mockClient { _, _, headers ->
            capturedApiKey = headers["X-API-Key"]
            Triple(sampleSuccessBody, HttpStatusCode.OK, jsonHeaders)
        }

        client.send(validConfig, "scheduled")

        assertEquals("test-key-42", capturedApiKey)
    }

    @Test
    fun send_usesPostMethod() = runTest {
        var capturedMethod: HttpMethod? = null
        val client = mockClient { _, method, _ ->
            capturedMethod = method
            Triple(sampleSuccessBody, HttpStatusCode.OK, jsonHeaders)
        }

        client.send(validConfig, "scheduled")

        assertEquals(HttpMethod.Post, capturedMethod)
    }

    // ── Successful responses ────────────────────────────────────────

    @Test
    fun send_200_returnsSuccessWithIp() = runTest {
        val client = mockClientResponse(status = HttpStatusCode.OK)

        val result = client.send(validConfig, "scheduled")

        assertTrue(result.success)
        assertEquals("93.184.216.34", result.ip)
        assertEquals("Heartbeat sent", result.message)
    }

    @Test
    fun send_201_returnsSuccessWithIp() = runTest {
        val client = mockClientResponse(status = HttpStatusCode.Created)

        val result = client.send(validConfig, "scheduled")

        assertTrue(result.success)
        assertEquals("93.184.216.34", result.ip)
    }

    @Test
    fun send_200_manualTrigger_messageIsIpUpdated() = runTest {
        val client = mockClientResponse(status = HttpStatusCode.OK)

        val result = client.send(validConfig, "manual")

        assertEquals("IP updated", result.message)
        assertEquals("manual", result.trigger)
    }

    // ── Error responses ─────────────────────────────────────────────

    @Test
    fun send_401_returnsInvalidApiKeyError() = runTest {
        val client = mockClientResponse(body = "Unauthorized", status = HttpStatusCode.Unauthorized)

        val result = client.send(validConfig, "scheduled")

        assertFalse(result.success)
        assertEquals("Invalid API key", result.message)
        assertNull(result.ip)
    }

    @Test
    fun send_404_returnsDeviceNotFoundError() = runTest {
        val client = mockClientResponse(body = "Not Found", status = HttpStatusCode.NotFound)

        val result = client.send(validConfig, "scheduled")

        assertFalse(result.success)
        assertEquals("Device not found", result.message)
    }

    @Test
    fun send_429_returnsRateLimitedError() = runTest {
        val client = mockClientResponse(body = "Too Many", status = HttpStatusCode.TooManyRequests)

        val result = client.send(validConfig, "scheduled")

        assertFalse(result.success)
        assertEquals("Rate limited", result.message)
    }

    @Test
    fun send_500_returnsGenericServerError() = runTest {
        val client = mockClientResponse(body = "Error", status = HttpStatusCode.InternalServerError)

        val result = client.send(validConfig, "scheduled")

        assertFalse(result.success)
        assertTrue(result.message.contains("Server error"))
        assertTrue(result.message.contains("500"))
    }

    // ── Network / exception handling ────────────────────────────────

    @Test
    fun send_networkError_returnsConnectionFailed() = runTest {
        val engine = MockEngine { throw RuntimeException("No route to host") }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val client = HeartbeatClient(httpClient)

        val result = client.send(validConfig, "scheduled")

        assertFalse(result.success)
        assertEquals("Connection failed", result.message)
        assertEquals("Heartbeat will resume when connected", result.hint)
    }
}
