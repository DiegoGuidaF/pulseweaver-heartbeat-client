package com.pulseweaver.heartbeat.service

import com.pulseweaver.heartbeat.config.HeartbeatConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
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
                    timeout { requestTimeoutMillis = 10_000 }
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
            println("HeartbeatClient: send failed → ${e::class.simpleName}: ${e.message}")
            e.cause?.let { println("HeartbeatClient: caused by → ${it::class.simpleName}: ${it.message}") }
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
        fun defaultClient(): HttpClient =
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
    }
}
