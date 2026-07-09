package com.pulseweaver.heartbeat.platform

import kotlin.test.Test

/**
 * Smoke test: exercises the whole facade so the expect/actual `Log` sink links and
 * runs on every test target (JVM + native) without throwing.
 */
class LogTest {
    @Test
    fun allLevelsWriteWithoutThrowing() {
        Log.d("test", "debug line")
        Log.i("test", "info line")
        Log.w("test", "warn line", RuntimeException("boom"))
        Log.e("test", "error line", IllegalStateException("nope"))
    }
}
