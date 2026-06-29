package com.pulseweaver.heartbeat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.HeartbeatConfig
import com.pulseweaver.heartbeat.config.ResultStore
import com.pulseweaver.heartbeat.config.ThemeMode
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.platform.currentEpochMs
import com.pulseweaver.heartbeat.service.HeartbeatResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import java.util.prefs.Preferences
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Generates the screenshots embedded in `docs/app.md`. Unlike the smoke tests, these render the
 * real screens at a phone aspect ratio in the production "Navy depth" theme and write PNGs.
 *
 * Writing is opt-in: a normal `jvmTest` run renders nothing (the release flow asserts a clean tree).
 * Regenerate with `make screenshots`, which sets `-Dpw.screenshots=true`.
 *
 * The shots render the desktop variant of the shared UI (no mobile FAB / biometric card); the
 * status circle, last-heartbeat line, and setup flow are identical shared Compose. Phone-chrome
 * shots would need an emulator run.
 */
@OptIn(ExperimentalTestApi::class)
class DocScreenshotsTest {
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scheduler = BackgroundScheduler(testScope)

    @BeforeTest
    fun clearPersistedState() {
        Preferences.userRoot().node("com/pulseweaver/heartbeat").apply {
            clear()
            flush()
        }
        Preferences.userRoot().node("com/pulseweaver/heartbeat/result").apply {
            clear()
            flush()
        }
    }

    @Test
    fun mainScreenManualHeartbeat() {
        if (!screenshotsEnabled) return
        seedActiveDevice(elapsedMs = 2 * 60 * 1000L) // "Last sent … (2m ago)"
        renderMainScreen { capture("main-manual-heartbeat.png") }
    }

    @Test
    fun mainScreenAfterActivation() {
        if (!screenshotsEnabled) return
        seedActiveDevice(elapsedMs = 5 * 1000L) // freshly paired: "(<1m ago)"
        renderMainScreen { capture("main-after-activation.png") }
    }

    @Test
    fun setupScreenWithPairingCode() {
        if (!screenshotsEnabled) return
        runDesktopComposeUiTest(width = PHONE_WIDTH_PX, height = PHONE_HEIGHT_PX) {
            setPhoneContent {
                SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
            }
            // A representative pairing code so the field is populated and Activate is enabled.
            onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD).performTextInput("PW-7Q2X-9F4K-3M8D")
            waitForIdle()
            capture("setup-pairing.png")
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    /** Persist a configured, running device plus a successful last heartbeat for the screen to load. */
    private fun seedActiveDevice(elapsedMs: Long) =
        runBlocking {
            ConfigStore().save(
                HeartbeatConfig(
                    serverUrl = "https://pw-device.example.com",
                    apiKey = "wdk_demo_key",
                    intervalSeconds = 1800,
                    enabled = true,
                    themeMode = ThemeMode.DARK,
                ),
            )
            ResultStore().save(
                result = HeartbeatResult.success(ip = "203.0.113.42", trigger = "manual"),
                time = "14:32",
                epochMs = currentEpochMs() - elapsedMs,
            )
        }

    /** Render [HeartbeatScreen], wait for the seeded config to load (status flips to "Active"), capture. */
    private fun renderMainScreen(capture: ComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(width = PHONE_WIDTH_PX, height = PHONE_HEIGHT_PX) {
            setPhoneContent { HeartbeatScreen(scheduler = scheduler) }
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Active").fetchSemanticsNodes().isNotEmpty()
            }
            // Freeze the status-hero pulse so capture is deterministic and never waits on an
            // infinite animation.
            mainClock.autoAdvance = false
            capture()
        }
}

// 2× the logical phone size: gives a crisp PNG (logical density set via LocalDensity below).
private const val SCALE = 2
private const val PHONE_WIDTH_DP = 412
private const val PHONE_HEIGHT_DP = 892
private const val PHONE_WIDTH_PX = PHONE_WIDTH_DP * SCALE
private const val PHONE_HEIGHT_PX = PHONE_HEIGHT_DP * SCALE

/** True when `-Dpw.screenshots` is set to a non-empty, non-"false" value. */
private val screenshotsEnabled: Boolean =
    System.getProperty("pw.screenshots").let { it != null && it.isNotBlank() && it != "false" }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setPhoneContent(content: @Composable () -> Unit) =
    setContent {
        // The surface is sized in pixels; LocalDensity maps it back to logical phone dp.
        // PulseWeaverTheme supplies the navy background and content colour.
        CompositionLocalProvider(LocalDensity provides Density(density = SCALE.toFloat(), fontScale = 1f)) {
            PulseWeaverTheme(darkTheme = true, content = content)
        }
    }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.capture(fileName: String) {
    val image = onRoot().captureToImage()
    val target = File(screenshotDir(), fileName)
    target.parentFile.mkdirs()
    target.writeBytes(image.toPngBytes())
    println("Wrote screenshot: ${target.absolutePath}")
}

private fun ImageBitmap.toPngBytes(): ByteArray {
    val data =
        Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)
            ?: error("Failed to PNG-encode screenshot")
    return data.bytes
}

/** Resolve the output dir: `-Dpw.screenshotDir` override, else `<repo>/screenshots/companionapp`. */
private fun screenshotDir(): File {
    System.getProperty("pw.screenshotDir")?.takeIf { it.isNotBlank() }?.let { return File(it) }
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
    val repoRoot = dir ?: File(System.getProperty("user.dir"))
    return File(repoRoot, "screenshots/companionapp")
}
