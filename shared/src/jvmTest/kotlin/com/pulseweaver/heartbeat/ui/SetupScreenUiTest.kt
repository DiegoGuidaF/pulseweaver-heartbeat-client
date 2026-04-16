package com.pulseweaver.heartbeat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Smoke tests for SetupScreen layout and interaction gating.
 */
@OptIn(ExperimentalTestApi::class)
class SetupScreenUiTest {

    // ── Layout presence ─────────────────────────────────────────────

    @Test
    fun screenRendersCodeFieldAndActivateButton() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
            }
        }

        onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD).assertIsDisplayed()
        onNodeWithTag(TestTags.ACTIVATE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun activateButtonDisabledWhenFieldEmpty() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
            }
        }

        onNodeWithTag(TestTags.ACTIVATE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun activateButtonEnabledAfterCodeEntered() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
            }
        }

        onNodeWithTag(TestTags.REGISTRATION_CODE_FIELD).performTextInput("someregistrationcode")
        onNodeWithTag(TestTags.ACTIVATE_BUTTON).assertIsEnabled()
    }

    @Test
    fun manualSetupLinkIsDisplayed() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SetupScreen(onProvisioningComplete = {}, onManualSetup = {})
            }
        }

        onNodeWithTag(TestTags.MANUAL_SETUP_LINK).assertIsDisplayed()
    }
}
