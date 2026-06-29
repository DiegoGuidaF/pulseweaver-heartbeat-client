package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.platform.BiometricAuth
import kotlin.time.TimeSource

private const val GRACE_PERIOD_SECONDS = 60L

/**
 * Wraps [content] with an optional biometric lock screen.
 *
 * On desktop, [BiometricAuth.isAvailable] is always false → content shown directly.
 *
 * On mobile, reads [biometricEnabled] from [ConfigStore] on each app open. If enabled:
 * - Shows a biometric prompt before revealing the UI on every cold open.
 * - Re-prompts when the app returns from background and more than [GRACE_PERIOD_SECONDS]
 *   have elapsed since the last successful authentication.
 *
 * Authentication is driven by [authSession]: incrementing it starts a new auth attempt.
 * This covers initial open, "Try Again" after failure, and grace-period re-auth uniformly.
 */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    if (!BiometricAuth.isAvailable()) {
        content()
        return
    }

    // Backing state objects captured directly in the lifecycle observer lambda
    // so it always reads current values, not values captured at composition time.
    val isAuthenticatedState = remember { mutableStateOf(false) }
    val lastAuthMarkState = remember { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }
    val authSessionState = remember { mutableStateOf(0) }

    var isAuthenticated by isAuthenticatedState
    var lastAuthMark by lastAuthMarkState
    var authSession by authSessionState
    var authFailed by remember { mutableStateOf(false) }

    val configStore = remember { ConfigStore() }

    // Single auth effect — re-runs on initial open, retry, and grace-period expiry.
    LaunchedEffect(authSession) {
        authFailed = false
        val config = configStore.load()
        if (!config.biometricEnabled) {
            isAuthenticated = true
            return@LaunchedEffect
        }
        isAuthenticated = false // hide content while prompt is showing
        val success = BiometricAuth.authenticate("Unlock PulseWeaver Companion")
        if (success) {
            isAuthenticated = true
            lastAuthMark = TimeSource.Monotonic.markNow()
        } else {
            authFailed = true
        }
    }

    // Re-authenticate when returning from background after the grace period.
    // Reads state values directly (not via delegate) to see current values at observer call time.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && isAuthenticatedState.value) {
                    val mark = lastAuthMarkState.value
                    if (mark != null && mark.elapsedNow().inWholeSeconds > GRACE_PERIOD_SECONDS) {
                        authSessionState.value++
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        isAuthenticated -> content()
        authFailed -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Authentication required",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { authSession++ }) {
                        Text("Try Again")
                    }
                }
            }
        }
        // else: blank screen while prompt is showing
    }
}
