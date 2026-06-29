package com.pulseweaver.heartbeat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test

/**
 * Smoke tests for SetupScreen layout and interaction gating.
 */
@OptIn(ExperimentalTestApi::class)
class SetupScreenUiTest {
    // ── Layout presence ─────────────────────────────────────────────

    @Test
    fun screenRendersCodeFieldAndActivateButton() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
                }
            }

            onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD).assertIsDisplayed()
            onNodeWithTag(TestTags.ACTIVATE_BUTTON).assertIsDisplayed()
        }

    @Test
    fun activateButtonDisabledWhenFieldEmpty() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
                }
            }

            onNodeWithTag(TestTags.ACTIVATE_BUTTON).assertIsNotEnabled()
        }

    @Test
    fun activateButtonEnabledAfterCodeEntered() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
                }
            }

            onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD).performTextInput("someregistrationcode")
            onNodeWithTag(TestTags.ACTIVATE_BUTTON).assertIsEnabled()
        }

    @OptIn(ExperimentalEncodingApi::class)
    private fun buildCode(serverUrl: String): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
            ByteArray(32) { it.toByte() } + serverUrl.encodeToByteArray(),
        )

    // ── Local feedback (no network) ─────────────────────────────────

    @Test
    fun recognizedCodeShowsServerHostConfirmation() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
                }
            }

            onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD)
                .performTextInput(buildCode("https://pulse.example.com"))

            onNodeWithTag(TestTags.SETUP_HOST_CONFIRM)
                .assertTextEquals("Ready to pair with pulse.example.com")
        }

    @Test
    fun malformedCodeShowsDiagnosticWithoutNetwork() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
                }
            }

            // Valid base64url characters but too short to be a real code: claim()
            // rejects it locally, so no network call is made.
            onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD).performTextInput("abcdef")
            onNodeWithTag(TestTags.ACTIVATE_BUTTON).performClick()
            waitForIdle()

            onNodeWithTag(TestTags.SETUP_ERROR_TEXT).assertIsDisplayed()
            onNodeWithTag(TestTags.SETUP_ERROR_DETAIL).assertTextEquals("PWC-PAIR-FORMAT")
        }

    @Test
    fun manualSetupLinkIsDisplayed() =
        runComposeUiTest {
            setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
                }
            }

            onNodeWithTag(TestTags.MANUAL_SETUP_LINK).assertIsDisplayed()
        }
}
