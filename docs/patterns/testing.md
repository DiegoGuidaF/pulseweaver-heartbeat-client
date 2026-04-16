# Testing

Tests use Ktor `MockEngine` for HTTP and Compose `ui-test` for UI. All tests live in `commonTest` (shared) or `jvmTest` (desktop-specific).

## HTTP tests with MockEngine

Wrap the `MockEngine` in an `HttpClient` and inject it into the class under test:

```kotlin
private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun mockClient(body: String, status: HttpStatusCode): HeartbeatClient {
    val engine = MockEngine { respond(body, status, jsonHeaders) }
    val httpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    return HeartbeatClient(httpClient)
}

@Test
fun send_200_returnsSuccess() = runTest {
    val client = mockClient(sampleSuccessBody, HttpStatusCode.OK)
    val result = client.send(config, "scheduled")
    assertTrue(result.success)
}

@Test
fun send_401_returnsAuthError() = runTest {
    val client = mockClient("Unauthorized", HttpStatusCode.Unauthorized)
    val result = client.send(config, "scheduled")
    assertFalse(result.success)
    assertEquals("Invalid API key", result.message)
}
```

For network failures, throw directly from the engine:
```kotlin
val engine = MockEngine { throw RuntimeException("No route to host") }
```

## Compose UI tests

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun screenRendersTitle() = runComposeUiTest {
    setContent {
        MaterialTheme(colorScheme = lightColorScheme()) {
            HeartbeatScreen(scheduler = scheduler)
        }
    }
    onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()
}
```

- Always wrap content in `MaterialTheme` — Compose M3 components require it.
- Use `performScrollTo()` before asserting on nodes that may be off-screen.
- Test tags live in `TestTags.kt` (commonMain) — use them instead of text matchers when possible.

### JVM config isolation

JVM UI tests share `java.util.prefs.Preferences` with the running process. Clear them before each test:

```kotlin
@BeforeTest
fun clearPersistedConfig() {
    val prefs = Preferences.userRoot().node("com/pulseweaver/heartbeat")
    prefs.clear()
    prefs.flush()
}
```

Without this, a test that saves config will pollute subsequent tests.

## Linting (ktlint)

**Do not use `./gradlew ktlintCheck` to verify your code.** The top-level task scans generated iOS resource collector files under `build/generated/compose/resourceGenerator/` and fails with indentation violations — this is a pre-existing build tooling issue unrelated to your changes.

Run source-set-specific tasks instead:

```bash
# Check all source sets that matter for most features:
./gradlew shared:runKtlintCheckOverCommonMainSourceSet \
          shared:runKtlintCheckOverAndroidMainSourceSet \
          shared:runKtlintCheckOverJvmMainSourceSet \
          shared:runKtlintCheckOverCommonTestSourceSet \
          shared:runKtlintCheckOverJvmTestSourceSet
```

Auto-fix style issues with `./gradlew ktlintFormat` (top-level format is safe — it only writes source files, not generated ones).

## Key rules

- **`MockEngine` for HTTP** — never make real network calls in tests.
- **`runTest` for coroutines** — use `kotlinx-coroutines-test`.
- **`runComposeUiTest` for UI** — Compose Multiplatform test framework.
- **Test in `commonTest`** by default — only use `jvmTest` for desktop-specific behavior (UI tests require JVM).
- **`MockEngine` needs `HttpClient` wrapper** — construct `HttpClient(engine) { install(...) }` and pass the `HttpClient` to your class, not the engine directly.

---
**Verified against:** `shared/src/commonTest/`, `shared/src/jvmTest/`
**Applies to:** all tests
**Known gaps:** none
**Last verified:** 2026-04-16
