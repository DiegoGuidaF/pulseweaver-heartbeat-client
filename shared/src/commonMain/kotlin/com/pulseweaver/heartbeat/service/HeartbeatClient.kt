package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.config.HeartbeatConfig
import com.pulseweaver.heartbeat.platform.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class HeartbeatClient(
    private val client: HttpClient = defaultClient(),
) {
    suspend fun send(
        config: HeartbeatConfig,
        trigger: String,
    ): HeartbeatResult {
        val url = config.serverUrl.trimEnd('/') + "/api/v1/heartbeat"

        return try {
            val response =
                client.post(url) {
                    header("X-API-Key", config.apiKey)
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
            when (response.status.value) {
                200, 201 -> {
                    val body = response.body<AddressResponse>()
                    HeartbeatResult.success(body.ip, trigger)
                }
                401 ->
                    HeartbeatResult.error(
                        "Invalid API key",
                        "Check your device settings on the server",
                        trigger,
                    )
                404 ->
                    HeartbeatResult.error(
                        "Device not found",
                        "The device may have been deleted",
                        trigger,
                    )
                429 ->
                    HeartbeatResult.error(
                        "Rate limited",
                        "Will retry on next scheduled heartbeat",
                        trigger,
                    )
                else ->
                    HeartbeatResult.error(
                        "Server error (${response.status.value})",
                        "Will retry on next scheduled heartbeat",
                        trigger,
                    )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Heartbeat", "send failed (trigger=$trigger)", e)
            HeartbeatResult.error(
                "Connection failed",
                "Heartbeat will resume when connected",
                trigger,
            )
        }
    }

    fun close() {
        client.close()
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient { configureDefaults() }

        /** Same production config on an injected engine, so tests exercise the real plugin stack. */
        fun defaultClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) { configureDefaults() }

        // Reliability policy for unreliable networks (e.g. a phone on subway 3G): short
        // attempts with fresh retries beat one long attempt, because a stuck attempt is
        // usually held by something a new connection bypasses — a black-holed DNS lookup,
        // a stale pooled socket after a Wi-Fi <-> cellular hop. The heartbeat is an
        // idempotent refresh, so a duplicate send is harmless.
        //
        // Install order matters: HttpSend runs the first-installed plugin outermost, so
        // retry must precede timeout for the request timeout to budget each attempt
        // rather than the whole retry loop.
        private fun HttpClientConfig<*>.configureDefaults() {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpRequestRetry) {
                maxRetries = 2
                // 5xx and network exceptions (including per-attempt timeouts) are worth a
                // fresh attempt; 4xx (bad key, deleted device, rate limit) are definitive.
                retryOnServerErrors()
                retryOnException(retryOnTimeout = true)
                exponentialDelay()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
        }
    }
}
