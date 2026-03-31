package com.pulseweaver.heartbeat

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the currently resumed [FragmentActivity].
 * Set in [MainActivity.onResume] and cleared in [MainActivity.onPause].
 * Used by [BiometricAuth] which requires a live FragmentActivity to show a prompt.
 */
object ActivityHolder {
    private var ref: WeakReference<FragmentActivity>? = null

    fun set(activity: FragmentActivity) {
        ref = WeakReference(activity)
    }

    fun clear() {
        ref = null
    }

    fun get(): FragmentActivity? = ref?.get()
}
