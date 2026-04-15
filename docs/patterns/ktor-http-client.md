# Ktor HTTP Client

All HTTP communication uses the Ktor client. The `HeartbeatClient` in `service/` is the only place HTTP calls are made.

## Pattern

```kotlin
class HeartbeatClient(
    private val engine: HttpClientEngine = CIO.create() // or Darwin on iOS
) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json() }
        // ... other plugins
    }

    suspend fun sendHeartbeat(config: HeartbeatConfig): HeartbeatResult {
        return try {
            val response = client.post("${config.serverUrl}/api/v1/heartbeat") {
                header("X-API-Key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(HeartbeatRequest(...))
            }
            when (response.status) {
                HttpStatusCode.OK -> HeartbeatResult.Success
                HttpStatusCode.Unauthorized -> HeartbeatResult.AuthError
                else -> HeartbeatResult.ServerError(response.status.value)
            }
        } catch (e: Exception) {
            HeartbeatResult.NetworkError(e)
        }
    }
}
```

## Key rules

- **Engine is injectable** — pass `MockEngine` in tests, platform-specific engine in production.
- **Result types over exceptions** — return sealed class results, don't throw for expected failures.
- **Single HTTP client instance** — reuse the `HttpClient` across calls.
- **Platform engines**: CIO for JVM/Android, Darwin for iOS.

---
**Verified against:** `shared/src/commonMain/.../service/HeartbeatClient.kt`
**Applies to:** any HTTP communication
**Known gaps:** none
**Last verified:** 2026-04-15
