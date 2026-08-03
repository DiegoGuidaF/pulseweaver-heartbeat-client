package com.pulseweaver.heartbeat.ui

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

private val GRACE = 60.seconds

class AuthSessionTest {
    private lateinit var clock: TestTimeSource

    @BeforeTest
    fun setUp() {
        clock = TestTimeSource()
        AuthSession.timeSource = clock
        AuthSession.lock()
    }

    @AfterTest
    fun tearDown() {
        AuthSession.lock()
        AuthSession.timeSource = TimeSource.Monotonic
    }

    @Test
    fun configurationChange_doesNotRelock() {
        AuthSession.markUnlocked()
        clock += 10.minutes

        // No background trip was reported — an Activity recreation must keep the unlock,
        // however long the app has been open.
        assertFalse(AuthSession.shouldRelock(GRACE))
        assertTrue(AuthSession.isUnlocked)
    }

    @Test
    fun backgroundTripWithinGrace_doesNotRelock() {
        AuthSession.markUnlocked()
        AuthSession.onEnteredBackground()
        clock += 30.seconds

        assertFalse(AuthSession.shouldRelock(GRACE))
    }

    @Test
    fun backgroundTripBeyondGrace_relocks() {
        AuthSession.markUnlocked()
        AuthSession.onEnteredBackground()
        clock += 61.seconds

        assertTrue(AuthSession.shouldRelock(GRACE))
    }

    @Test
    fun backgroundTrip_isConsumedBySingleCheck() {
        AuthSession.markUnlocked()
        AuthSession.onEnteredBackground()
        clock += 61.seconds

        assertTrue(AuthSession.shouldRelock(GRACE))
        // The recreated composition consumed it; the ON_RESUME that follows must not
        // relock a second time.
        AuthSession.markUnlocked()
        assertFalse(AuthSession.shouldRelock(GRACE))
    }

    @Test
    fun onEnteredBackground_keepsEarliestMark() {
        AuthSession.markUnlocked()
        AuthSession.onEnteredBackground()
        clock += 61.seconds
        AuthSession.onEnteredBackground()

        // The grace period covers the whole absence, not just the latest leg.
        assertTrue(AuthSession.shouldRelock(GRACE))
    }

    @Test
    fun lockedSession_neverRelocks() {
        AuthSession.onEnteredBackground()
        clock += 61.seconds

        assertFalse(AuthSession.shouldRelock(GRACE))
    }

    @Test
    fun successfulAuth_clearsPendingBackgroundTrip() {
        // The device-credential prompt stops the Activity, which reports a background
        // trip while the user is still authenticating.
        AuthSession.onEnteredBackground()
        clock += 61.seconds
        AuthSession.markUnlocked()

        assertFalse(AuthSession.shouldRelock(GRACE))
    }
}
