package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulseweaver.heartbeat.BuildInfo
import com.pulseweaver.heartbeat.config.UpdateState
import com.pulseweaver.heartbeat.config.UpdateStore
import com.pulseweaver.heartbeat.platform.InstallOutcome
import com.pulseweaver.heartbeat.platform.UpdateInstaller
import com.pulseweaver.heartbeat.platform.UrlOpener
import com.pulseweaver.heartbeat.platform.currentEpochMs
import com.pulseweaver.heartbeat.service.UpdateCheck
import com.pulseweaver.heartbeat.service.UpdateChecker
import com.pulseweaver.heartbeat.service.isNewerThanSkipped
import com.pulseweaver.heartbeat.service.shouldCheckNow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Transient one-line feedback about the last check or install attempt. */
enum class UpdateNoticeMessage {
    UP_TO_DATE,
    CHECK_FAILED,
    NEEDS_PERMISSION,
    INSTALL_FAILED,
}

/**
 * Drives the update notice. Held in composable scope like the rest of the app's state — see the
 * app-architecture pattern; there is no ViewModel here.
 */
@Stable
class UpdateNoticeState internal constructor(
    private val checker: UpdateChecker,
    private val store: UpdateStore,
    private val scope: CoroutineScope,
) {
    /** False on `dev` and `local` builds, which have no version comparable to a release tag. */
    val isSupported: Boolean = checker.isSupported

    var available by mutableStateOf<UpdateCheck.Available?>(null)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var message by mutableStateOf<UpdateNoticeMessage?>(null)
        private set

    /** True when the CTA installs in place; false when it can only open the release page. */
    val canInstall: Boolean
        get() = available?.downloadUrl != null && UpdateInstaller.isAvailable()

    internal suspend fun checkIfDue() {
        if (!isSupported) return
        val state = store.load()
        if (!shouldCheckNow(currentEpochMs(), state.lastCheckedAtMs)) return
        runCheck(state)
    }

    fun checkNow() {
        if (!isSupported || isBusy) return
        scope.launch { runCheck(store.load()) }
    }

    fun skip() {
        val update = available ?: return
        available = null
        scope.launch { store.save(store.load().copy(skippedVersion = update.version)) }
    }

    /** Installs where the platform allows it, otherwise hands the release page to the browser. */
    fun act() {
        val update = available ?: return
        val downloadUrl = update.downloadUrl
        if (downloadUrl == null || !UpdateInstaller.isAvailable()) {
            UrlOpener.open(update.releaseUrl)
            return
        }
        if (isBusy) return
        scope.launch {
            isBusy = true
            message = null
            try {
                when (UpdateInstaller.install(downloadUrl)) {
                    // The OS prompt has taken over; nothing left for the app to say.
                    InstallOutcome.STARTED -> Unit
                    InstallOutcome.NEEDS_PERMISSION -> message = UpdateNoticeMessage.NEEDS_PERMISSION
                    InstallOutcome.FAILED -> {
                        message = UpdateNoticeMessage.INSTALL_FAILED
                        UrlOpener.open(update.releaseUrl)
                    }
                }
            } finally {
                isBusy = false
            }
        }
    }

    private suspend fun runCheck(state: UpdateState) {
        isBusy = true
        message = null
        try {
            when (val result = checker.check()) {
                is UpdateCheck.Available -> {
                    stampChecked(state)
                    if (result.isNewerThanSkipped(state.skippedVersion)) {
                        available = result
                    } else {
                        message = UpdateNoticeMessage.UP_TO_DATE
                    }
                }

                UpdateCheck.UpToDate -> {
                    stampChecked(state)
                    available = null
                    message = UpdateNoticeMessage.UP_TO_DATE
                }

                // Deliberately not stamped: a failed check hasn't answered the question, so the
                // next launch should retry rather than sit out the full day.
                UpdateCheck.Failed -> message = UpdateNoticeMessage.CHECK_FAILED
            }
        } finally {
            isBusy = false
        }
    }

    private suspend fun stampChecked(state: UpdateState) {
        store.save(state.copy(lastCheckedAtMs = currentEpochMs()))
    }
}

@Composable
fun rememberUpdateNotice(): UpdateNoticeState {
    val scope = rememberCoroutineScope()
    val state = remember { UpdateNoticeState(UpdateChecker(), UpdateStore(), scope) }
    LaunchedEffect(Unit) { state.checkIfDue() }
    return state
}

/**
 * The automatic notice. Sits directly under the status hero so it lands in the first viewport
 * on both a phone and a desktop window — seen without being looked for — while staying inline
 * rather than modal, because a stale version does not stop heartbeats.
 */
@Composable
fun UpdateCard(
    state: UpdateNoticeState,
    modifier: Modifier = Modifier,
) {
    val update = state.available ?: return

    Card(
        modifier = modifier.fillMaxWidth().testTag(TestTags.UPDATE_CARD),
        // primaryContainer, not the translucent surfaceVariant every other card uses: this is
        // the one card that has to catch someone who opened the app to check their heartbeat.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Update available",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Version ${update.version} is out — you are on ${BuildInfo.version}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = state::act,
                    enabled = !state.isBusy,
                    modifier = Modifier.testTag(TestTags.UPDATE_ACTION),
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.canInstall) "Download & install" else "View release")
                }
                TextButton(
                    onClick = state::skip,
                    modifier = Modifier.testTag(TestTags.UPDATE_SKIP),
                ) {
                    Text("Skip this version")
                }
            }
            state.message?.let { message ->
                Text(
                    messageText(message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag(TestTags.UPDATE_STATUS),
                )
            }
        }
    }
}

/**
 * The manual affordance, beside the build-identity line — the spot that already answers "which
 * build am I running?", so it is where someone who *is* looking will look. Also the only way to
 * re-check without waiting out the daily interval.
 */
@Composable
fun CheckForUpdatesLink(
    state: UpdateNoticeState,
    modifier: Modifier = Modifier,
) {
    if (!state.isSupported) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = state::checkNow,
            enabled = !state.isBusy,
            modifier = Modifier.testTag(TestTags.UPDATE_CHECK_LINK),
        ) {
            Text(
                if (state.isBusy) "Checking…" else "Check for updates",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Only when no card is showing — the card renders the same message itself.
        val message = state.message
        if (message != null && state.available == null) {
            Text(
                messageText(message),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.UPDATE_STATUS),
            )
        }
    }
}

private fun messageText(message: UpdateNoticeMessage): String =
    when (message) {
        UpdateNoticeMessage.UP_TO_DATE -> "Up to date"
        UpdateNoticeMessage.CHECK_FAILED -> "Couldn't check"
        UpdateNoticeMessage.NEEDS_PERMISSION -> "Allow installs for PulseWeaver, then try again"
        UpdateNoticeMessage.INSTALL_FAILED -> "Download failed — opening the release page"
    }
