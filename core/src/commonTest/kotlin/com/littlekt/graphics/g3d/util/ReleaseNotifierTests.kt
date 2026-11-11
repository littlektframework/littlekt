package com.littlekt.graphics.g3d.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseNotifierTests {

    private class TestListener : ReleaseListener {
        val released = mutableListOf<Int>()

        override fun onReleased(id: Int) {
            released += id
        }
    }

    @Test
    fun test_notify_calls_listener_once() {
        val notifier = ReleaseNotifier()
        val listener = TestListener()
        notifier.add(listener)
        notifier.add(listener)
        notifier.notifyReleased(4)
        notifier.notifyReleased(4)
        assertEquals(listOf(4), listener.released)
    }

    @Test
    fun test_notify_increments_version() {
        val notifier = ReleaseNotifier()
        val version = notifier.version
        notifier.notifyReleased(1)
        assertEquals(version + 1, notifier.version)
    }

    @Test
    fun test_removed_listener_not_notified() {
        val notifier = ReleaseNotifier()
        val listener = TestListener()
        notifier.add(listener)
        notifier.remove(listener)
        notifier.notifyReleased(1)
        assertEquals(0, listener.released.size)
    }

    @Test
    fun test_listener_added_after_release_is_notified_on_next_release() {
        val notifier = ReleaseNotifier()
        notifier.notifyReleased(1)
        val listener = TestListener()
        notifier.add(listener)
        notifier.notifyReleased(1)
        assertEquals(listOf(1), listener.released)
    }
}
