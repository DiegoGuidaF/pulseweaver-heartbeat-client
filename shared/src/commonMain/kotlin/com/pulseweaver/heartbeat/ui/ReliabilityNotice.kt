package com.pulseweaver.heartbeat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Kept short and free of Android vocabulary on purpose. The earlier copy led with "Doze",
// "App Standby" and a three-clause explanation, which reads as technical boilerplate and gets
// dismissed unread — the one thing this prompt cannot afford.
private const val TITLE = "Keep this device connected"
private const val BODY =
    "Android can pause PulseWeaver while your phone sleeps, which may drop this device's " +
        "access. Allowing unrestricted battery use keeps it connected."

// The settings page differs by Android version: "Battery" up to Android 14, "App battery
// usage" from 15. Naming both is what stops someone landing on a usage graph and assuming
// they are done.
private const val PATH_HINT = "On the page that opens: Battery (or App battery usage) → Unrestricted."

private const val OPEN_SETTINGS = "Open settings"

/**
 * The one-per-install modal. Prominent because granting the exemption is what keeps the device
 * authorized while the phone sleeps, and single-shot because a device that never reports the
 * exemption would otherwise reopen it on every launch — see `HeartbeatUtils.reliabilityPrompt`.
 */
@Composable
fun ReliabilityDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(TestTags.RELIABILITY_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(BODY, style = MaterialTheme.typography.bodyMedium)
                Text(
                    PATH_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(TestTags.RELIABILITY_ALLOW_BUTTON),
                onClick = onOpenSettings,
            ) { Text(OPEN_SETTINGS) }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(TestTags.RELIABILITY_DISMISS),
                onClick = onDismiss,
            ) { Text("Not now") }
        },
    )
}

/**
 * The standing inline request, shown once the modal has had its turn and for as long as the app
 * is not exempt. Deliberately not dismissible: it is the only on-screen evidence that the
 * exemption is missing, so it has to survive into the screenshot a user attaches to a report.
 */
@Composable
fun ReliabilityCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.RELIABILITY_CARD),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // WarningYellow, not Amber: amber is reserved for liveness, and this is a warning
            // about a setting — see the ui style guide's amber/warning split.
            Text(
                TITLE,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.WarningYellow,
            )
            Text(
                "Android may pause PulseWeaver while your phone sleeps. Set its battery use to Unrestricted.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(TestTags.RELIABILITY_CARD_ACTION),
            ) { Text(OPEN_SETTINGS) }
        }
    }
}
