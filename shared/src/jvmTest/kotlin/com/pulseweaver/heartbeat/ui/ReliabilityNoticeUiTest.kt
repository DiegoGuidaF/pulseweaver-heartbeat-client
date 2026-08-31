package com.pulseweaver.heartbeat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Renders the reliability surfaces directly. `HeartbeatScreen` never shows them on desktop —
 * `BatteryOptimization.isExempt()` is hardcoded true on JVM — so the composables are driven
 * here, and the decision that picks between them is unit-tested in `HeartbeatUtilsTest`.
 */
@OptIn(ExperimentalTestApi::class)
class ReliabilityNoticeUiTest {
    @Test
    fun dialog_bothButtonsRetireIt() =
        runComposeUiTest {
            var opened = 0
            var dismissed = 0
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    ReliabilityDialog(onOpenSettings = { opened++ }, onDismiss = { dismissed++ })
                }
            }

            onNodeWithTag(TestTags.RELIABILITY_DIALOG).assertIsDisplayed()
            onNodeWithTag(TestTags.RELIABILITY_ALLOW_BUTTON).performClick()
            onNodeWithTag(TestTags.RELIABILITY_DISMISS).performClick()
            waitForIdle()

            assertEquals(1, opened)
            assertEquals(1, dismissed)
        }

    // The card is the standing evidence that the exemption is missing, so it carries no dismiss
    // affordance — only the CTA.
    @Test
    fun card_offersOnlyTheSettingsRoute() =
        runComposeUiTest {
            var opened = 0
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    ReliabilityCard(onOpenSettings = { opened++ })
                }
            }

            onNodeWithTag(TestTags.RELIABILITY_CARD).assertIsDisplayed()
            onNodeWithTag(TestTags.RELIABILITY_DISMISS).assertDoesNotExist()
            onNodeWithTag(TestTags.RELIABILITY_CARD_ACTION).performClick()
            waitForIdle()

            assertEquals(1, opened)
        }
}
