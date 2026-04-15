# Testing

Tests use Ktor `MockEngine` for HTTP and Compose `ui-test` for UI. All tests live in `commonTest` (shared) or `jvmTest` (desktop-specific).

## HTTP tests with MockEngine

```kotlin
@Test
fun sendHeartbeat_success_returnsSuccess() = runTest {
    val engine = MockEngine { request ->
        assertEquals("/api/v1/heartbeat", request.url.encodedPath)
        respond(
            content = "{}",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val client = HeartbeatClient(engine)
    val result = client.sendHeartbeat(testConfig)
    assertIs<HeartbeatResult.Success>(result)
}

@Test
fun sendHeartbeat_unauthorized_returnsAuthError() = runTest {
    val engine = MockEngine {
        respond(content = "", status = HttpStatusCode.Unauthorized)
    }
    val client = HeartbeatClient(engine)
    val result = client.sendHeartbeat(testConfig)
    assertIs<HeartbeatResult.AuthError>(result)
}
```

## Compose UI tests

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun heartbeatScreen_showsServerUrl() = runComposeUiTest {
    setContent {
        HeartbeatScreen(config = testConfig, onSend = {})
    }
    onNodeWithText(testConfig.serverUrl).assertExists()
}
```

## Key rules

- **`MockEngine` for HTTP** — never make real network calls in tests.
- **`runTest` for coroutines** — use `kotlinx-coroutines-test`.
- **`runComposeUiTest` for UI** — Compose Multiplatform test framework.
- **Test in `commonTest`** by default — only use `jvmTest` for desktop-specific behavior.

---
**Verified against:** `shared/src/commonTest/`
**Applies to:** all tests
**Known gaps:** none
**Last verified:** 2026-04-15
