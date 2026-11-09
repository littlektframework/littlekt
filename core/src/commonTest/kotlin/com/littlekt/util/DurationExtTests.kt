package com.littlekt.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds as intMilliseconds
import kotlin.time.Duration.Companion.minutes as intMinutes

class DurationExtTests {

    @Test
    fun test_float_to_duration() {
        assertEquals(1500.intMilliseconds, 1.5f.seconds)
        assertEquals(250.intMilliseconds, 250f.milliseconds)
        assertEquals(90.intMinutes, 1.5f.hours)
        assertEquals(2.intMinutes, 2f.minutes)
        assertEquals(36.intMinutes * 60, 1.5f.days)
    }

    @Test
    fun test_float_small_units() {
        assertEquals(2.intMilliseconds, 2000f.microseconds)
        assertEquals(1.intMilliseconds, 1_000_000f.nanoseconds)
    }

    @Test
    fun test_duration_to_float() {
        val duration = 1500.intMilliseconds
        assertEquals(1.5f, duration.seconds)
        assertEquals(1500f, duration.milliseconds)
        assertEquals(1_500_000f, duration.microseconds)
        assertEquals(1.5e9f, duration.nanoseconds)
        assertEquals(0.025f, duration.minutes)
    }

    @Test
    fun test_duration_large_units_to_float() {
        assertEquals(1.5f, 90.intMinutes.hours)
        assertEquals(0.5f, (12 * 60).intMinutes.days)
    }
}
