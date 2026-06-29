package com.pulseweaver.heartbeat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.ThemeMode
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.service.HeartbeatResult
import com.pulseweaver.heartbeat.service.HeartbeatUtils
import com.pulseweaver.heartbeat.ui.AuthGate
import com.pulseweaver.heartbeat.ui.HeartbeatScreen
import com.pulseweaver.heartbeat.ui.PulseWeaverTheme
import com.pulseweaver.heartbeat.ui.SetupScreen
import com.pulseweaver.heartbeat.ui.SystemAppearance

private sealed interface ScreenState {
    data object Loading : ScreenState

    data object Setup : ScreenState

    data object Main : ScreenState
}

@Composable
fun App(
    scheduler: BackgroundScheduler,
    onLastResultChange: (HeartbeatResult?) -> Unit = {},
    sendNowTrigger: Int = 0,
) {
    var themeMode by remember { mutableStateOf(ThemeMode.AUTO) }
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Loading) }

    LaunchedEffect(Unit) {
        val config = ConfigStore().load()
        themeMode = config.themeMode
        screenState =
            if (HeartbeatUtils.isConfigValid(config.serverUrl, config.apiKey)) {
                ScreenState.Main
            } else {
                ScreenState.Setup
            }
    }

    val systemIsDark = isSystemInDarkTheme()
    val useDark = HeartbeatUtils.shouldUseDarkTheme(themeMode, systemIsDark)

    PulseWeaverTheme(darkTheme = useDark) {
        SystemAppearance(darkTheme = useDark)
        when (screenState) {
            ScreenState.Loading -> { /* blank while config loads — avoids setup/main flash */ }
            ScreenState.Setup ->
                SetupScreen(
                    onProvisioningComplete = { screenState = ScreenState.Main },
                    onManualSetup = { screenState = ScreenState.Main },
                )
            ScreenState.Main ->
                AuthGate {
                    HeartbeatScreen(
                        scheduler = scheduler,
                        onLastResultChange = onLastResultChange,
                        sendNowTrigger = sendNowTrigger,
                        onThemeModeChange = { themeMode = it },
                        onEnterSetupCode = { screenState = ScreenState.Setup },
                    )
                }
        }
    }
}
