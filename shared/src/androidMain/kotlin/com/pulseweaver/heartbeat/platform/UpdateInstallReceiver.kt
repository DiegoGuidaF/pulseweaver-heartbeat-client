package com.pulseweaver.heartbeat.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

private const val TAG = "UpdateInstall"

/**
 * Receives the outcome of a `PackageInstaller` session started by [UpdateInstaller].
 *
 * The load-bearing case is [PackageInstaller.STATUS_PENDING_USER_ACTION]: a committed session
 * shows nothing on its own, and the system hands back the confirmation Intent here for the app
 * to launch. Without this branch the update would stage silently and never install.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm == null) {
                    Log.w(TAG, "pending user action carried no confirmation intent")
                    return
                }
                // Launched from a receiver, so it needs its own task.
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirm) }
                    .onFailure { Log.w(TAG, "could not show the install prompt", it) }
            }

            PackageInstaller.STATUS_SUCCESS -> Log.i(TAG, "update installed")

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w(TAG, "update install failed (status=$status): $message")
            }
        }
    }
}
