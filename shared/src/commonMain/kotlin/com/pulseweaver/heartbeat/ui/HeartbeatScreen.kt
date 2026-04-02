package com.pulseweaver.heartbeat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.pulseweaver.heartbeat.config.ThemeMode
import com.pulseweaver.heartbeat.service.HeartbeatUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.HeartbeatConfig
import com.pulseweaver.heartbeat.config.ResultStore
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.platform.BiometricAuth
import com.pulseweaver.heartbeat.platform.NetworkMonitor
import com.pulseweaver.heartbeat.platform.UrlOpener
import com.pulseweaver.heartbeat.platform.currentTimeForDisplay
import com.pulseweaver.heartbeat.platform.platformHasBackgroundLimit
import com.pulseweaver.heartbeat.service.HeartbeatClient
import com.pulseweaver.heartbeat.service.HeartbeatResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private val INTERVAL_SECONDS = listOf(60, 300, 900, 1800, 3600)
private val INTERVAL_LABELS = listOf("1m", "5m", "15m", "30m", "1h")

// Amber = liveness/pulse, Indigo (primary) = action/structure — per style guide.
private val Amber = Color(0xFFFFA94D)
private val StoppedGrey = Color(0xFF9E9E9E)
private val ErrorRed = Color(0xFFFA5252)
private val WarningYellow = Color(0xFFFCC419)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartbeatScreen(
    scheduler: BackgroundScheduler,
    onLastResultChange: (HeartbeatResult?) -> Unit = {},
    sendNowTrigger: Int = 0,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val configState = remember { mutableStateOf(HeartbeatConfig()) }
    var config by configState
    var isLoaded by remember { mutableStateOf(false) }
    // Tracks the last config written to / read from disk — used to suppress
    // the "Saved ✓" indicator when the change was driven by the initial load,
    // not a user edit.
    var lastSavedConfig by remember { mutableStateOf<HeartbeatConfig?>(null) }

    var lastResult by remember { mutableStateOf<HeartbeatResult?>(null) }
    var lastResultTime by remember { mutableStateOf("") }
    var lastResultMark by remember { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }
    var nextInDisplay by remember { mutableStateOf("") }

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { HeartbeatClient() }
    val configStore = remember { ConfigStore() }
    val resultStore = remember { ResultStore() }
    val networkMonitor = remember { NetworkMonitor() }

    suspend fun sendHeartbeat(trigger: String) {
        if (isSending) return
        isSending = true
        val result = client.send(config, trigger)
        lastResult = result
        lastResultTime = currentTimeForDisplay()
        lastResultMark = TimeSource.Monotonic.markNow()
        resultStore.save(result, lastResultTime)
        onLastResultChange(result)
        isSending = false
    }

    // Load config and last heartbeat result on startup
    LaunchedEffect(Unit) {
        val loaded = configStore.load()
        lastSavedConfig = loaded  // mark as already-on-disk so auto-save skips it
        config = loaded
        val savedState = resultStore.load()
        if (savedState != null) {
            lastResult = savedState.result
            lastResultTime = savedState.time
        }
        isLoaded = true
        if (config.enabled) {
            networkMonitor.startMonitoring { coroutineScope.launch { sendHeartbeat("network_change") } }
            scheduler.schedulePeriodicHeartbeat(config.intervalSeconds) { sendHeartbeat("scheduled") }
        }
    }

    // Auto-save (debounced) — only fires when config differs from what's on disk
    LaunchedEffect(config) {
        if (!isLoaded || config == lastSavedConfig) return@LaunchedEffect
        delay(500)
        configStore.save(config)
        lastSavedConfig = config
        showSaved = true
        delay(2000)
        showSaved = false
    }

    // "Send Now" from tray
    LaunchedEffect(sendNowTrigger) {
        if (sendNowTrigger > 0 && isLoaded) sendHeartbeat("tray")
    }

    // Countdown ticker
    LaunchedEffect(lastResultMark, config.intervalSeconds, config.enabled) {
        if (!config.enabled || lastResultMark == null) { nextInDisplay = ""; return@LaunchedEffect }
        while (true) {
            val elapsed = lastResultMark!!.elapsedNow().inWholeSeconds
            val remaining = config.intervalSeconds - elapsed
            nextInDisplay = if (remaining > 0) formatDuration(remaining) else "now"
            if (remaining <= 0) break
            delay(1_000)
        }
    }

    // Reschedule when interval changes
    LaunchedEffect(config.intervalSeconds) {
        if (!isLoaded || !config.enabled) return@LaunchedEffect
        scheduler.schedulePeriodicHeartbeat(config.intervalSeconds) { sendHeartbeat("scheduled") }
    }

    DisposableEffect(Unit) {
        onDispose {
            scheduler.cancelHeartbeat()
            networkMonitor.stopMonitoring()
            client.close()
        }
    }

    val isConfigValid = HeartbeatUtils.isConfigValid(config.serverUrl, config.apiKey)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Small amber pulse dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Amber, CircleShape),
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Amber, fontWeight = FontWeight.Bold)) {
                                    append("Pulse")
                                }
                                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append("Weaver")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag(TestTags.APP_TITLE),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Status hero ────────────────────────────────────────────────
            StatusHero(
                enabled = config.enabled,
                lastResult = lastResult,
                lastResultTime = lastResultTime,
                nextInDisplay = nextInDisplay,
                isSending = isSending,
                isConfigValid = isConfigValid,
                onTap = { coroutineScope.launch { sendHeartbeat("manual") } },
            )

            // ── Connection card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().testTag(TestTags.CONNECTION_CARD),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Connection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        AnimatedVisibility(
                            visible = showSaved,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(600)),
                        ) {
                            Text(
                                "Saved ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = config.serverUrl,
                        onValueChange = { config = config.copy(serverUrl = it) },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://server.example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.SERVER_URL_FIELD),
                    )
                    OutlinedTextField(
                        value = config.apiKey,
                        onValueChange = { config = config.copy(apiKey = it) },
                        label = { Text("API Key") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Text(if (isApiKeyVisible) "Hide" else "Show")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.API_KEY_FIELD),
                    )
                }
            }

            // ── Schedule card ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SCHEDULE_CARD),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Column {
                        Text("Check every", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            INTERVAL_SECONDS.forEachIndexed { i, seconds ->
                                FilterChip(
                                    selected = config.intervalSeconds == seconds,
                                    onClick = { config = config.copy(intervalSeconds = seconds) },
                                    label = { Text(INTERVAL_LABELS[i]) },
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Heartbeat", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (!isConfigValid) {
                                Text("Enter server URL and API key to enable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = config.enabled,
                            enabled = isConfigValid,
                            onCheckedChange = { enabled ->
                                config = config.copy(enabled = enabled)
                                if (enabled) {
                                    scheduler.schedulePeriodicHeartbeat(config.intervalSeconds) { sendHeartbeat("scheduled") }
                                    networkMonitor.startMonitoring { coroutineScope.launch { sendHeartbeat("network_change") } }
                                    coroutineScope.launch { sendHeartbeat("manual") }
                                } else {
                                    scheduler.cancelHeartbeat()
                                    networkMonitor.stopMonitoring()
                                }
                            },
                            modifier = Modifier.testTag(TestTags.HEARTBEAT_SWITCH),
                        )
                    }
                }
            }

            // ── Last result card ───────────────────────────────────────────
            if (lastResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    LastResultRow(
                        modifier = Modifier.padding(16.dp),
                        lastResult = lastResult!!,
                        lastResultTime = lastResultTime,
                        nextInDisplay = nextInDisplay,
                    )
                }
            }

            // ── Actions ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Desktop only: Send Now inline; mobile uses FAB
                if (!platformHasBackgroundLimit) {
                    Button(
                        onClick = { coroutineScope.launch { sendHeartbeat("manual") } },
                        enabled = isConfigValid && !isSending,
                        modifier = Modifier.testTag(TestTags.SEND_NOW_BUTTON),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Send Now")
                        }
                    }
                }
                OutlinedButton(
                    onClick = { UrlOpener.open(config.serverUrl) },
                    enabled = config.serverUrl.isNotEmpty(),
                ) {
                    Text("Open Server URL ↗")
                }
            }

            // ── Security card (mobile only) ────────────────────────────────
            if (BiometricAuth.isAvailable()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Security", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Require biometric to open", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = config.biometricEnabled,
                                onCheckedChange = { config = config.copy(biometricEnabled = it) },
                            )
                        }
                    }
                }
            }

            // ── Appearance card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().testTag(TestTags.APPEARANCE_CARD),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            val chipTag = when (mode) {
                                ThemeMode.AUTO -> TestTags.THEME_CHIP_AUTO
                                ThemeMode.LIGHT -> TestTags.THEME_CHIP_LIGHT
                                ThemeMode.DARK -> TestTags.THEME_CHIP_DARK
                            }
                            FilterChip(
                                selected = config.themeMode == mode,
                                onClick = {
                                    config = config.copy(themeMode = mode)
                                    onThemeModeChange(mode)
                                },
                                label = {
                                    Text(when (mode) {
                                        ThemeMode.AUTO -> "Auto"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    })
                                },
                                modifier = Modifier.testTag(chipTag),
                            )
                        }
                    }
                }
            }

            // ── Background limit hint (mobile only) ────────────────────────
            if (platformHasBackgroundLimit) {
                Text(
                    text = "ⓘ Background heartbeat minimum is 15 min on mobile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusHero(
    enabled: Boolean,
    lastResult: HeartbeatResult?,
    lastResultTime: String,
    nextInDisplay: String,
    isSending: Boolean = false,
    isConfigValid: Boolean = false,
    onTap: () -> Unit = {},
) {
    val statusColor = when {
        !enabled -> StoppedGrey
        lastResult?.success == false -> ErrorRed
        else -> Amber
    }

    // Pulsing ripple — only computed and running when active and healthy
    val showPulse = enabled && lastResult?.success != false
    val pulseScale: Float
    val pulseAlpha: Float
    if (showPulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        pulseScale = infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.8f,
            animationSpec = infiniteRepeatable(tween(2400, easing = EaseOut), RepeatMode.Restart),
            label = "pulseScale",
        ).value
        pulseAlpha = infiniteTransition.animateFloat(
            initialValue = 0.5f, targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "pulseAlpha",
        ).value
    } else {
        pulseScale = 1f
        pulseAlpha = 0f
    }

    val canTap = isConfigValid && !isSending

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag(TestTags.STATUS_HERO),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(enabled = canTap) { onTap() },
        ) {
            // Ripple ring
            if (showPulse) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .background(statusColor.copy(alpha = pulseAlpha), CircleShape),
                )
            }
            // Status circle
            Surface(
                shape = CircleShape,
                color = statusColor,
                shadowElevation = 4.dp,
                modifier = Modifier.size(72.dp),
            ) {
                if (isSending) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = when {
                isSending -> "Sending…"
                !enabled -> "Stopped"
                lastResult?.success == false -> "Error"
                else -> "Active"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            modifier = Modifier.testTag(TestTags.STATUS_LABEL),
        )

        if (isConfigValid) {
            Text(
                text = "Tap to send now",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (enabled && lastResult?.ip != null) {
            Text(
                text = "IP: ${lastResult.ip}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (lastResultTime.isNotEmpty()) {
            Text(
                text = if (nextInDisplay.isNotEmpty()) "Next in $nextInDisplay · Last $lastResultTime"
                       else "Last sent $lastResultTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastResultRow(
    modifier: Modifier = Modifier,
    lastResult: HeartbeatResult,
    lastResultTime: String,
    nextInDisplay: String,
) {
    val resultColor = when {
        lastResult.success -> Amber
        lastResult.message.contains("limited", ignoreCase = true) -> WarningYellow
        else -> ErrorRed
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Last response", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            text = lastResult.message,
            style = MaterialTheme.typography.bodyMedium,
            color = resultColor,
        )
        if (lastResultTime.isNotEmpty()) {
            Text(
                text = "at $lastResultTime  ·  trigger: ${lastResult.trigger}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (lastResult.hint != null) {
            Text(text = lastResult.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(totalSeconds: Long): String = HeartbeatUtils.formatDuration(totalSeconds)
