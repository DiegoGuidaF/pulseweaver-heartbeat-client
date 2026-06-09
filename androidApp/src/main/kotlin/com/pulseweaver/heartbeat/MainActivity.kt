package com.pulseweaver.heartbeat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    }

    override fun onResume() {
        super.onResume()
        ActivityHolder.set(this)
    }

    override fun onPause() {
        super.onPause()
        ActivityHolder.clear()
    }
}


