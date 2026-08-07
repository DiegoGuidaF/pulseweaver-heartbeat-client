package com.pulseweaver.heartbeat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.pulseweaver.heartbeat.config.UpdateStore
import com.pulseweaver.heartbeat.service.UpdateCheck
import com.pulseweaver.heartbeat.service.UpdateChecker
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val RELEASE_JSON =
    """{"tag_name":"v1.5.0","html_url":"https://example.test/releases/v1.5.0",""" +
        """"assets":[{"name":"PulseWeaver-Companion-v1.5.0-android.apk",""" +
        """"browser_download_url":"https://example.test/app.apk"}]}"""

/**
 * Covers the notice's UI contract on desktop, where [com.pulseweaver.heartbeat.platform.UpdateInstaller]
 * is deliberately unavailable — so the CTA must offer the release page rather than an install.
 */
@OptIn(ExperimentalTestApi::class)
class UpdateNoticeUiTest {
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @BeforeTest
    fun clearPersistedUpdateState() {
        val prefs = Preferences.userRoot().node("com/pulseweaver/heartbeat/update")
        prefs.clear()
        prefs.flush()
    }

    private fun noticeState(channel: String = "release"): UpdateNoticeState {
        val engine =
            MockEngine {
                respond(
                    RELEASE_JSON,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        val checker =
            UpdateChecker(
                client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
                currentVersion = "1.2.3",
                channel = channel,
                // Desktop resolves null; the Android suffix here proves the card still falls back
                // to the release page when the platform cannot install.
                assetSuffix = "-android.apk",
            )
        return UpdateNoticeState(checker, UpdateStore(), testScope)
    }

    @Test
    fun card_hiddenUntilAnUpdateIsFound() =
        runComposeUiTest {
            val state = noticeState()
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) { UpdateCard(state) }
            }

            onNodeWithTag(TestTags.UPDATE_CARD).assertDoesNotExist()
        }

    @Test
    fun card_showsNewVersionAndOffersTheReleasePageOnDesktop() =
        runComposeUiTest {
            val state = noticeState()
            runBlocking { state.checkIfDue() }

            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) { UpdateCard(state) }
            }

            onNodeWithTag(TestTags.UPDATE_CARD).assertIsDisplayed()
            onNodeWithText("1.5.0", substring = true).assertIsDisplayed()
            // Desktop cannot install in place, so the CTA must not promise one.
            onNodeWithText("View release").assertIsDisplayed()
        }

    @Test
    fun skip_hidesTheCard() =
        runComposeUiTest {
            val state = noticeState()
            runBlocking { state.checkIfDue() }

            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) { UpdateCard(state) }
            }

            onNodeWithTag(TestTags.UPDATE_SKIP).performClick()
            waitForIdle()

            onNodeWithTag(TestTags.UPDATE_CARD).assertDoesNotExist()
        }

    @Test
    fun manualLink_hiddenOnNonReleaseBuilds() =
        runComposeUiTest {
            val state = noticeState(channel = "local")
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) { CheckForUpdatesLink(state) }
            }

            onNodeWithTag(TestTags.UPDATE_CHECK_LINK).assertDoesNotExist()
        }

    @Test
    fun manualLink_shownOnReleaseBuilds() =
        runComposeUiTest {
            val state = noticeState()
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) { CheckForUpdatesLink(state) }
            }

            onNodeWithTag(TestTags.UPDATE_CHECK_LINK).assertIsDisplayed()
        }
}
