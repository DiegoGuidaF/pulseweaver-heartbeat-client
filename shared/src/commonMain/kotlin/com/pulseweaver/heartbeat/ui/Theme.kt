package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Navy depth theme ────────────────────────────────────────
// Dark: deep navy background + slightly lighter card surfaces (rich, not flat).
// Light: pure white background + light-grey cards (clean, no lavender bleed).
private val IndigoLight =
    lightColorScheme(
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
private val IndigoDark =
    darkColorScheme(
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

/**
 * The PulseWeaver "Navy depth" Material 3 theme, shared by every screen.
 *
 * The root [Surface] paints the themed background and, crucially, sets the matching `onSurface`
 * content colour — screens that live outside a `Scaffold` (e.g. `SetupScreen`) would otherwise
 * inherit the default black `LocalContentColor` and render invisible text on the dark theme.
 */
@Composable
fun PulseWeaverTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) IndigoDark else IndigoLight
    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background, content = content)
    }
}
