package com.pulseweaver.heartbeat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import com.pulseweaver.heartbeat.platform.BackgroundScheduler

// FragmentActivity (not ComponentActivity) is required by BiometricPrompt.
// FragmentActivity extends ComponentActivity so setContent and enableEdgeToEdge still work.
class MainActivity : FragmentActivity() {

    private lateinit var scheduler: BackgroundScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduler = BackgroundScheduler(applicationContext)
        setContent {
            App(scheduler = scheduler)
        }
        if (savedInstanceState == null) {
            promptDisableBatteryOptimization()
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityHolder.set(this)
    }

    override fun onPause() {
        super.onPause()
        ActivityHolder.clear()
    }

    private fun promptDisableBatteryOptimization() {
        val pm = getSystemService<PowerManager>() ?: return
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}


