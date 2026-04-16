package com.pulseweaver.heartbeat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.ThemeMode
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.service.HeartbeatResult
import com.pulseweaver.heartbeat.service.HeartbeatUtils
import com.pulseweaver.heartbeat.ui.AuthGate
import com.pulseweaver.heartbeat.ui.HeartbeatScreen
import com.pulseweaver.heartbeat.ui.SetupScreen

// Navy depth Theme  ────────────────────────────────────────
// Dark: deep navy background + slightly lighter card surfaces (rich, not flat).
// Light: pure white background + light-grey cards (clean, no lavender bleed).
private val IndigoLight = lightColorScheme(
    primary = Color(0xFF5C7CFA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE4FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1B2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B2E),
    surfaceVariant = Color(0xFFF4F4F6),
    onSurfaceVariant = Color(0xFF44465A),
)
private val IndigoDark = darkColorScheme(
    primary = Color(0xFF5C7CFA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3451CB),
    onPrimaryContainer = Color(0xFFDBE4FF),
    background = Color(0xFF0D0F1E),
    onBackground = Color(0xFFE2E5F1),
    surface = Color(0xFF161829),
    onSurface = Color(0xFFE2E5F1),
    surfaceVariant = Color(0xFF1E2035),
    onSurfaceVariant = Color(0xFFADB5D4),
)

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
        screenState = if (HeartbeatUtils.isConfigValid(config.serverUrl, config.apiKey)) {
            ScreenState.Main
        } else {
            ScreenState.Setup
        }
    }

    val systemIsDark = isSystemInDarkTheme()
    val useDark = HeartbeatUtils.shouldUseDarkTheme(themeMode, systemIsDark)

    MaterialTheme(colorScheme = if (useDark) IndigoDark else IndigoLight) {
        when (screenState) {
            ScreenState.Loading -> { /* blank while config loads — avoids setup/main flash */ }
            ScreenState.Setup -> SetupScreen(
                onProvisioningComplete = { screenState = ScreenState.Main },
                onManualSetup = { screenState = ScreenState.Main },
            )
            ScreenState.Main -> AuthGate {
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
