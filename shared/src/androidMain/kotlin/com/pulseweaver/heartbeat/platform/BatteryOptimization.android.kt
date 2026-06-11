package com.pulseweaver.heartbeat.platform

import android.annotation.SuppressLint
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

    // BatteryLife: the direct-request intent is the user-facing exemption dialog. It is fired only
    // from the explanatory reliability card (user-initiated), which is what Play policy requires.
    @SuppressLint("BatteryLife")
    actual fun requestExemption() {
        val packageName = ApplicationContextHolder.context.packageName
        val launcher: Context = ActivityHolder.get() ?: ApplicationContextHolder.context
        val intent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", packageName, null)
                // App context is not an Activity, so it needs its own task to launch the dialog.
                if (launcher !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        launcher.startActivity(intent)
    }
}
