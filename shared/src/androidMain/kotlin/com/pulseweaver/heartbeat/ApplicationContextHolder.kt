package com.pulseweaver.heartbeat

import android.content.Context

/**
 * Process-scoped holder for the application Context.
 * Initialized once in [PulseWeaverApp.onCreate] before any component needs it.
 */
object ApplicationContextHolder {
    private lateinit var _context: Context

    fun init(context: Context) {
        _context = context.applicationContext
    }

    val context: Context
        get() = _context
}
