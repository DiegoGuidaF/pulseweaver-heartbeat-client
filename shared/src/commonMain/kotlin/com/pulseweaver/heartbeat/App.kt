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

// Indigo (#5C7CFA) as primary — structure & action per style guide.
private val IndigoLight = lightColorScheme(
    primary = Color(0xFF5C7CFA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE4FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFF5C7CFA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3451CB),
    onPrimaryContainer = Color(0xFFDBE4FF),
)

@Composable
fun App(
    scheduler: BackgroundScheduler,
    onLastResultChange: (HeartbeatResult?) -> Unit = {},
    sendNowTrigger: Int = 0,
) {
    var themeMode by remember { mutableStateOf(ThemeMode.AUTO) }

    LaunchedEffect(Unit) {
        themeMode = ConfigStore().load().themeMode
    }

    val systemIsDark = isSystemInDarkTheme()
    val useDark = HeartbeatUtils.shouldUseDarkTheme(themeMode, systemIsDark)

    MaterialTheme(colorScheme = if (useDark) IndigoDark else IndigoLight) {
        AuthGate {
            HeartbeatScreen(
                scheduler = scheduler,
                onLastResultChange = onLastResultChange,
                sendNowTrigger = sendNowTrigger,
                onThemeModeChange = { themeMode = it },
            )
        }
    }
}
