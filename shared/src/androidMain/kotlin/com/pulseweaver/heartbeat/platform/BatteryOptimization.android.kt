package com.pulseweaver.heartbeat.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import com.pulseweaver.heartbeat.ActivityHolder
import com.pulseweaver.heartbeat.ApplicationContextHolder

actual object BatteryOptimization {
    actual fun isExempt(): Boolean {
        val context = ApplicationContextHolder.context
        val pm = context.getSystemService<PowerManager>() ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Opens this app's App info page, not the per-app exemption dialog. The direct request
    // (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) needs a Play-restricted permission whose
    // allowed use cases don't cover a heartbeat keep-alive; App info is unrestricted and its
    // Battery entry offers the same exemption under "Unrestricted".
    //
    // Not the system-wide optimization list (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS): OEM
    // builds filter that list to apps that are already exempt, so the one app the user came to
    // change is the one missing from it. App info also resolves on every device, which that
    // screen is not guaranteed to do.
    actual fun requestExemption() {
        val launcher: Context = ActivityHolder.get() ?: ApplicationContextHolder.context
        val packageName = ApplicationContextHolder.context.packageName
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ).apply {
                // App context is not an Activity, so it needs its own task to launch the screen.
                if (launcher !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        launcher.startActivity(intent)
    }
}
