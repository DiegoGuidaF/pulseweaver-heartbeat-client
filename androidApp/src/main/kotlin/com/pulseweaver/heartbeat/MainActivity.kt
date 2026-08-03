package com.pulseweaver.heartbeat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.pulseweaver.heartbeat.platform.BackgroundScheduler
import com.pulseweaver.heartbeat.ui.AuthSession

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
    }

    override fun onResume() {
        super.onResume()
        ActivityHolder.set(this)
    }

    override fun onPause() {
        super.onPause()
        ActivityHolder.clear()
    }

    override fun onStop() {
        super.onStop()
        // A configuration change (rotation, theme switch) recreates this Activity without
        // the app ever leaving the foreground, so it must not start the biometric
        // grace-period clock — otherwise rotating re-prompts for a fingerprint.
        if (!isChangingConfigurations) AuthSession.onEnteredBackground()
    }
}


