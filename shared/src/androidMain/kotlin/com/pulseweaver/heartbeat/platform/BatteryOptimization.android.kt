package com.pulseweaver.heartbeat.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
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

    // Opens the system battery-optimization list, not the per-app exemption dialog. The direct
    // request (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) needs a Play-restricted permission whose
    // allowed use cases don't cover a heartbeat keep-alive; the list intent is unrestricted and the
    // user disables optimization for the app themselves.
    actual fun requestExemption() {
        val launcher: Context = ActivityHolder.get() ?: ApplicationContextHolder.context
        val intent =
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                // App context is not an Activity, so it needs its own task to launch the screen.
                if (launcher !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        launcher.startActivity(intent)
    }
}
