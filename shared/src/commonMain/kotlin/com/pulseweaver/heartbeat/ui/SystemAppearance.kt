package com.pulseweaver.heartbeat.ui

import androidx.compose.runtime.Composable

/**
 * Aligns the platform system bars (status / navigation) with the app theme so their
 * icons stay legible against the app background — including a manual Light/Dark override
 * that differs from the OS setting. No-op where there is no app-controlled system bar.
 */
@Composable
expect fun SystemAppearance(darkTheme: Boolean)
