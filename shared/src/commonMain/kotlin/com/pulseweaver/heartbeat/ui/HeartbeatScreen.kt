package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.config.ConfigStore
import com.pulseweaver.heartbeat.config.HeartbeatConfig
import com.pulseweaver.heartbeat.config.ResultStore
import com.pulseweaver.heartbeat.config.ThemeMode
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.platform.BatteryOptimization
import com.pulseweaver.heartbeat.platform.BiometricAuth
import com.pulseweaver.heartbeat.platform.NetworkMonitor
import com.pulseweaver.heartbeat.platform.UrlOpener
import com.pulseweaver.heartbeat.platform.currentEpochMs
import com.pulseweaver.heartbeat.platform.currentTimeForDisplay
import com.pulseweaver.heartbeat.platform.platformHasBackgroundLimit
import com.pulseweaver.heartbeat.service.HeartbeatClient
import com.pulseweaver.heartbeat.service.HeartbeatResult
import com.pulseweaver.heartbeat.service.HeartbeatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private val INTERVAL_SECONDS = listOf(900, 1800, 3600, 21600, 86400)
private val INTERVAL_LABELS = listOf("15m", "30m", "1h", "6h", "1d")

// Indigo (primary) = action/structure, Amber = liveness/pulse — per style guide.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartbeatScreen(
    scheduler: BackgroundScheduler,
    onLastResultChange: (HeartbeatResult?) -> Unit = {},
    sendNowTrigger: Int = 0,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onEnterSetupCode: () -> Unit = {},
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
    var lastResultEpochMs by remember { mutableStateOf(0L) }
    var elapsedDisplay by remember { mutableStateOf("") }
    var nextInDisplay by remember { mutableStateOf("") }

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    var showReRegisterDialog by remember { mutableStateOf(false) }
    // Expanded by default for first-run; collapsed once the config is valid.
    var connectionExpanded by remember { mutableStateOf(true) }
    // Android-only: true once the app is exempt from battery optimization. Starts true on
    // desktop/iOS so the reliability surface never appears there.
    var batteryExempt by remember { mutableStateOf(BatteryOptimization.isExempt()) }
    // Dismissing the exemption modal hides it for this session; it reappears next launch while
    // still unexempt, so the prompt stays prominent without nagging mid-session.
    var reliabilityDismissed by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { HeartbeatClient() }
    val configStore = remember { ConfigStore() }
    val resultStore = remember { ResultStore() }
    val networkMonitor = remember { NetworkMonitor() }

    suspend fun sendHeartbeat(trigger: String) {
        if (isSending) return
        isSending = true
        val result = client.send(config, trigger)
        val epochMs = currentEpochMs()
        lastResult = result
        lastResultTime = currentTimeForDisplay()
        lastResultMark = TimeSource.Monotonic.markNow()
        lastResultEpochMs = epochMs
        resultStore.save(result, lastResultTime, epochMs)
        onLastResultChange(result)
        isSending = false
    }

    // Load config and last heartbeat result on startup
    LaunchedEffect(Unit) {
        val loaded = configStore.load()
        lastSavedConfig = loaded // mark as already-on-disk so auto-save skips it
        config = loaded
        val savedState = resultStore.load()
        if (savedState != null) {
            lastResult = savedState.result
            lastResultTime = savedState.time
            lastResultEpochMs = savedState.epochMs
        }
        if (HeartbeatUtils.isConfigValid(loaded.serverUrl, loaded.apiKey)) {
            connectionExpanded = false
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
        if (!config.enabled || lastResultMark == null) {
            nextInDisplay = ""
            return@LaunchedEffect
        }
        while (true) {
            val elapsed = lastResultMark!!.elapsedNow().inWholeSeconds
            val remaining = config.intervalSeconds - elapsed
            nextInDisplay = if (remaining > 0) formatDuration(remaining) else "now"
            if (remaining <= 0) break
            delay(1_000)
        }
    }

    // Elapsed-since-last-heartbeat ticker — updates every 30s, drives "Xm ago" label
    LaunchedEffect(lastResultEpochMs) {
        if (lastResultEpochMs == 0L) {
            elapsedDisplay = ""
            return@LaunchedEffect
        }
        while (true) {
            elapsedDisplay = HeartbeatUtils.formatElapsed(lastResultEpochMs, currentEpochMs())
            delay(30_000)
        }
    }

    // Reschedule when interval changes
    LaunchedEffect(config.intervalSeconds) {
        if (!isLoaded || !config.enabled) return@LaunchedEffect
        scheduler.schedulePeriodicHeartbeat(config.intervalSeconds) { sendHeartbeat("scheduled") }
    }

    // Observe ResultStore so background (WorkManager) heartbeats refresh the open UI. The first
    // emission is the value already shown via the initial load, so it's dropped; each later write
    // updates the result and restarts the countdown. No-op past the first emission on desktop/iOS.
    LaunchedEffect(isLoaded) {
        if (!isLoaded) return@LaunchedEffect
        resultStore.observe().drop(1).collect { state ->
            if (state == null) return@collect
            lastResult = state.result
            lastResultTime = state.time
            lastResultEpochMs = state.epochMs
            lastResultMark = TimeSource.Monotonic.markNow()
            onLastResultChange(state.result)
        }
    }

    // Re-check the battery-optimization exemption until granted, so the reliability modal disappears
    // shortly after the user returns from the system dialog. Inert on desktop/iOS (starts exempt).
    LaunchedEffect(Unit) {
        while (!batteryExempt) {
            delay(1_000)
            batteryExempt = BatteryOptimization.isExempt()
        }
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
                        BrandMark(size = 24.dp)
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = AppColors.Amber, fontWeight = FontWeight.Bold)) {
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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
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
                elapsedDisplay = elapsedDisplay,
                nextInDisplay = nextInDisplay,
                isSending = isSending,
                isConfigValid = isConfigValid,
                onTap = { coroutineScope.launch { sendHeartbeat("manual") } },
            )

            // ── Background reliability (Android, until exempt) ─────────────
            // A modal rather than an inline card: granting the exemption is what keeps the device
            // authorized while the phone sleeps, so the request must be prominent, not optional.
            if (isLoaded && config.enabled && !batteryExempt && !reliabilityDismissed) {
                AlertDialog(
                    modifier = Modifier.testTag(TestTags.RELIABILITY_CARD),
                    onDismissRequest = { reliabilityDismissed = true },
                    title = { Text("Keep this device authorized") },
                    text = {
                        Text(
                            "Android pauses background apps to save battery, which can delay your " +
                                "heartbeat by hours and let this device's access expire. Allow " +
                                "background activity so PulseWeaver can keep it authorized while " +
                                "your phone sleeps.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            modifier = Modifier.testTag(TestTags.RELIABILITY_ALLOW_BUTTON),
                            onClick = {
                                BatteryOptimization.requestExemption()
                                reliabilityDismissed = true
                            },
                        ) { Text("Allow") }
                    },
                    dismissButton = {
                        TextButton(onClick = { reliabilityDismissed = true }) { Text("Not now") }
                    },
                )
            }

            // ── Connection card — hidden until config is loaded to avoid flash ──
            if (isLoaded) {
                ConnectionCard(
                    config = config,
                    expanded = connectionExpanded,
                    isConfigValid = isConfigValid,
                    showSaved = showSaved,
                    isApiKeyVisible = isApiKeyVisible,
                    locked = config.settingsLocked,
                    onExpandToggle = { connectionExpanded = !connectionExpanded },
                    onServerUrlChange = { config = config.copy(serverUrl = it) },
                    onApiKeyChange = { config = config.copy(apiKey = it) },
                    onApiKeyVisibilityToggle = { isApiKeyVisible = !isApiKeyVisible },
                    onEnterSetupCode = { showReRegisterDialog = true },
                )
            }

            // ── Re-register confirmation dialog ───────────────────────────
            if (showReRegisterDialog) {
                AlertDialog(
                    onDismissRequest = { showReRegisterDialog = false },
                    title = { Text("Start over?") },
                    text = {
                        Text(
                            "All current settings will be erased. You will need a registration code from your administrator to set up the device again.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showReRegisterDialog = false
                                coroutineScope.launch {
                                    scheduler.cancelHeartbeat()
                                    networkMonitor.stopMonitoring()
                                    configStore.save(HeartbeatConfig())
                                    onEnterSetupCode()
                                }
                            },
                        ) { Text("Continue") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReRegisterDialog = false }) { Text("Cancel") }
                    },
                )
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
                        Text(
                            "Check every",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            INTERVAL_SECONDS.forEachIndexed { i, seconds ->
                                FilterChip(
                                    selected = config.intervalSeconds == seconds,
                                    onClick = { config = config.copy(intervalSeconds = seconds) },
                                    label = { Text(INTERVAL_LABELS[i]) },
                                    enabled = !config.settingsLocked,
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
                                Text(
                                    "Enter server URL and API key to enable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = config.enabled,
                            enabled = isConfigValid && !config.settingsLocked,
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
                        elapsedDisplay = elapsedDisplay,
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
                                enabled = !config.settingsLocked,
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
                            val chipTag =
                                when (mode) {
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
                                    Text(
                                        when (mode) {
                                            ThemeMode.AUTO -> "Auto"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        },
                                    )
                                },
                                modifier = Modifier.testTag(chipTag),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String = HeartbeatUtils.formatDuration(totalSeconds)
