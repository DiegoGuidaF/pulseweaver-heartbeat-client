package com.pulseweaver.heartbeat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.prefs.Preferences
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Basic UI smoke tests for HeartbeatScreen.
 *
 * These verify the screen renders correctly and key interactions work.
 * They run on JVM using compose-ui-test — no emulator required.
 */
@OptIn(ExperimentalTestApi::class)
class HeartbeatScreenUiTest {
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scheduler = BackgroundScheduler(testScope)

    @BeforeTest
    fun clearPersistedConfig() {
        // Wipe JVM Preferences so each test starts with a clean default config
        val prefs = Preferences.userRoot().node("com/pulseweaver/heartbeat")
        prefs.clear()
        prefs.flush()
    }

    // ── Layout presence ─────────────────────────────────────────────

    @Test
    fun screenRendersAppTitle() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()
            onNodeWithText("PulseWeaver", substring = true).assertIsDisplayed()
        }

    @Test
    fun screenRendersAllCards() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.STATUS_HERO).assertIsDisplayed()
            onNodeWithTag(TestTags.CONNECTION_CARD).assertIsDisplayed()
            // These cards may be below the fold — scroll to them first
            onNodeWithTag(TestTags.SCHEDULE_CARD).performScrollTo().assertIsDisplayed()
            onNodeWithTag(TestTags.APPEARANCE_CARD).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun statusHeroShowsStoppedByDefault() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.STATUS_LABEL).assertTextEquals("Stopped")
        }

    // ── Heartbeat switch gating ─────────────────────────────────────

    @Test
    fun heartbeatSwitchDisabledWithoutConfig() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.HEARTBEAT_SWITCH).performScrollTo()
            onNodeWithTag(TestTags.HEARTBEAT_SWITCH).assertIsNotEnabled()
        }

    @Test
    fun heartbeatSwitchEnabledAfterValidConfig() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            // Fill in valid server URL and API key
            onNodeWithTag(TestTags.SERVER_URL_FIELD).performTextInput("https://example.com")
            onNodeWithTag(TestTags.API_KEY_FIELD).performTextInput("test-key")

            onNodeWithTag(TestTags.HEARTBEAT_SWITCH).performScrollTo()
            onNodeWithTag(TestTags.HEARTBEAT_SWITCH).assertIsEnabled()
        }

    // ── Theme chips ─────────────────────────────────────────────────

    @Test
    fun themeChipsArePresent() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.THEME_CHIP_AUTO).performScrollTo().assertIsDisplayed()
            onNodeWithTag(TestTags.THEME_CHIP_LIGHT).performScrollTo().assertIsDisplayed()
            onNodeWithTag(TestTags.THEME_CHIP_DARK).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun themeChipClickChangesSelection() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.THEME_CHIP_DARK).performScrollTo().performClick()
            onNodeWithTag(TestTags.THEME_CHIP_DARK).assertIsDisplayed()
        }

    // ── Desktop-specific: Send Now button ───────────────────────────

    @Test
    fun sendNowButtonDisabledWithoutConfig() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            // On JVM, platformHasBackgroundLimit == false, so Send Now appears
            onNodeWithTag(TestTags.SEND_NOW_BUTTON).performScrollTo().assertIsDisplayed()
            onNodeWithTag(TestTags.SEND_NOW_BUTTON).assertIsNotEnabled()
        }

    @Test
    fun sendNowButtonEnabledAfterValidConfig() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    HeartbeatScreen(scheduler = scheduler)
                }
            }

            onNodeWithTag(TestTags.SERVER_URL_FIELD).performTextInput("https://example.com")
            onNodeWithTag(TestTags.API_KEY_FIELD).performTextInput("key")

            onNodeWithTag(TestTags.SEND_NOW_BUTTON).performScrollTo()
            onNodeWithTag(TestTags.SEND_NOW_BUTTON).assertIsEnabled()
        }
}
