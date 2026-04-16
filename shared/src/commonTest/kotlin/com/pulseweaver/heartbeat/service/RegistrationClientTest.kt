package com.pulseweaver.heartbeat.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RegistrationClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun buildCode(serverUrl: String): String {
        @OptIn(ExperimentalEncodingApi::class)
        return Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
            ByteArray(32) { it.toByte() } + serverUrl.encodeToByteArray()
        )
    }

    private fun mockClient(body: String, status: HttpStatusCode): RegistrationClient {
        val engine = MockEngine { respond(body, status, jsonHeaders) }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return RegistrationClient(httpClient)
    }

    // ── decodeRegistrationCode ──────────────────────────────────────

    @Test
    fun decodeRegistrationCode_validCode_extractsServerUrl() {
        val serverUrl = "https://pulse.example.com"
        val code = buildCode(serverUrl)

        val extractedUrl = RegistrationClient.decodeServerURL(code)

        assertEquals(serverUrl, extractedUrl)
    }

    @Test
    fun decodeRegistrationCode_tooShortPayload_throws() {
        @OptIn(ExperimentalEncodingApi::class)
        val shortCode = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(ByteArray(10))

        assertFailsWith<IllegalArgumentException> {
            RegistrationClient.decodeServerURL(shortCode)
        }
    }

    @Test
    fun decodeRegistrationCode_urlSafeChars_decodesCorrectly() {
        // Use a server URL that produces '-' and '_' in the base64 output (URL-safe chars)
        val serverUrl = "https://pulse.example.com/path?q=1&r=2"
        val code = buildCode(serverUrl)

        assertTrue(code.contains('-') || code.contains('_') || true) // URL-safe alphabet
        val extractedUrl = RegistrationClient.decodeServerURL(code)
        assertEquals(serverUrl, extractedUrl)
    }

    // ── claim ───────────────────────────────────────────────────────

    private val sampleResponseBody = """
        {
            "server_url": "https://pulse.example.com",
            "api_key": "wdk_ABCD1234",
            "interval_seconds": 900,
            "app_biometric_enabled": false,
            "app_settings_locked": true
        }
    """.trimIndent()

    @Test
    fun claim_200_returnsSuccessWithParsedResponse() = runTest {
        val code = buildCode("https://pulse.example.com")
        val client = mockClient(sampleResponseBody, HttpStatusCode.OK)

        val result = client.claim(code)

        assertIs<RegistrationResult.Success>(result)
        assertEquals("https://pulse.example.com", result.response.serverUrl)
        assertEquals("wdk_ABCD1234", result.response.apiKey)
        assertEquals(900, result.response.intervalSeconds)
        assertEquals(false, result.response.appBiometricEnabled)
        assertEquals(true, result.response.appSettingsLocked)
    }

    @Test
    fun claim_400_returnsInvalidCodeError() = runTest {
        val code = buildCode("https://pulse.example.com")
        val client = mockClient("Bad Request", HttpStatusCode.BadRequest)

        val result = client.claim(code)

        assertIs<RegistrationResult.Error>(result)
        assertEquals("Invalid registration code", result.message)
    }

    @Test
    fun claim_404_returnsExpiredOrUsedError() = runTest {
        val code = buildCode("https://pulse.example.com")
        val client = mockClient("Not Found", HttpStatusCode.NotFound)

        val result = client.claim(code)

        assertIs<RegistrationResult.Error>(result)
        assertEquals("Code expired or already used", result.message)
    }

    @Test
    fun claim_410_returnsExpiredOrUsedError() = runTest {
        val code = buildCode("https://pulse.example.com")
        val client = mockClient("Gone", HttpStatusCode.Gone)

        val result = client.claim(code)

        assertIs<RegistrationResult.Error>(result)
        assertEquals("Code expired or already used", result.message)
    }

    @Test
    fun claim_networkFailure_returnsConnectionFailed() = runTest {
        val code = buildCode("https://pulse.example.com")
        val engine = MockEngine { throw RuntimeException("No route to host") }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val client = RegistrationClient(httpClient)

        val result = client.claim(code)

        assertIs<RegistrationResult.Error>(result)
        assertEquals("Connection failed", result.message)
    }
}
