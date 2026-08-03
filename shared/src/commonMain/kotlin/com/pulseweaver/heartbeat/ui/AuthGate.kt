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
import kotlin.time.Duration.Companion.seconds

private val GRACE_PERIOD = 60.seconds

/**
 * Wraps [content] with an optional biometric lock screen.
 *
 * On desktop, [BiometricAuth.isAvailable] is always false → content shown directly.
 *
 * On mobile, reads [biometricEnabled] from [ConfigStore] on each app open. If enabled:
 * - Shows a biometric prompt before revealing the UI on every cold open.
 * - Re-prompts when the app returns from background and more than [GRACE_PERIOD] has
 *   elapsed since the last successful authentication.
 *
 * The unlock itself is held by [AuthSession], not by this composable, so an Activity
 * recreation (rotation, theme change) re-enters an already-unlocked session instead of
 * prompting again.
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

    // Backing state object captured directly in the lifecycle observer lambda
    // so it always reads the current value, not the value captured at composition time.
    val authSessionState = remember { mutableStateOf(0) }
    var authSession by authSessionState
    var authFailed by remember { mutableStateOf(false) }

    val configStore = remember { ConfigStore() }

    // Settled during composition rather than in an effect: when the system destroyed the
    // Activity while backgrounded, the new composition must already know it is locked, or
    // it paints the protected content for a frame before the prompt arrives.
    remember { if (AuthSession.shouldRelock(GRACE_PERIOD)) AuthSession.lock() }

    // Single auth effect — re-runs on initial open, retry, and grace-period expiry.
    LaunchedEffect(authSession) {
        authFailed = false
        val config = configStore.load()
        if (!config.biometricEnabled) {
            AuthSession.markUnlocked()
            return@LaunchedEffect
        }
        if (AuthSession.isUnlocked) return@LaunchedEffect // survived a configuration change
        val success = BiometricAuth.authenticate("Unlock PulseWeaver Companion")
        if (success) {
            AuthSession.markUnlocked()
        } else {
            authFailed = true
        }
    }

    // Re-authenticate when returning from background after the grace period.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && AuthSession.shouldRelock(GRACE_PERIOD)) {
                    AuthSession.lock()
                    authSessionState.value++
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        AuthSession.isUnlocked -> content()
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
