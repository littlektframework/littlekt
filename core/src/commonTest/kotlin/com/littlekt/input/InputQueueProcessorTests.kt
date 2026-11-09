package com.littlekt.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InputQueueProcessorTests {

    private class Recorder(val handles: Boolean = false, val queue: InputQueueProcessor? = null) :
        InputProcessor {
        val events = mutableListOf<String>()
        val times = mutableListOf<Long>()

        private fun record(event: String): Boolean {
            events += event
            queue?.let { times += it.currentEventTime }
            return handles
        }

        override fun keyDown(key: Key) = record("keyDown $key")

        override fun keyUp(key: Key) = record("keyUp $key")

        override fun keyRepeat(key: Key) = record("keyRepeat $key")

        override fun charTyped(character: Char) = record("char $character")

        override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer) =
            record("touchDown ${screenX.str()} ${screenY.str()} $pointer")

        override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer) =
            record("touchUp ${screenX.str()} ${screenY.str()} $pointer")

        override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer) =
            record("dragged ${screenX.str()} ${screenY.str()} $pointer")

        override fun mouseMoved(screenX: Float, screenY: Float) = record("moved ${screenX.str()} ${screenY.str()}")

        override fun scrolled(amountX: Float, amountY: Float) = record("scrolled ${amountX.str()} ${amountY.str()}")

        override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int) =
            record("pressed $button ${pressure.str()} $gamepad")

        override fun gamepadButtonReleased(button: GameButton, gamepad: Int) =
            record("released $button $gamepad")

        override fun gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int) =
            record("stick $stick ${xAxis.str()} ${yAxis.str()} $gamepad")

        override fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int) =
            record("trigger $button ${pressure.str()} $gamepad")
    }

    @Test
    fun test_drain_dispatches_in_queued_order() {
        val queue = InputQueueProcessor()
        val recorder = Recorder()
        queue.keyDown(Key.A, 0)
        queue.charTyped('a', 0)
        queue.keyUp(Key.A, 0)
        queue.drain(listOf(recorder))
        assertEquals(listOf("keyDown A", "char a", "keyUp A"), recorder.events)
    }

    @Test
    fun test_drain_dispatches_every_event_type() {
        val queue = InputQueueProcessor()
        val recorder = Recorder()
        val pad = GamepadInfo(2)
        queue.keyRepeat(Key.B, 0)
        queue.touchDown(1f, 2f, Pointer.POINTER2, 0)
        queue.touchDragged(3f, 4f, Pointer.POINTER2, 0)
        queue.touchUp(5f, 6f, Pointer.POINTER2, 0)
        queue.mouseMoved(7f, 8f, 0)
        queue.scrolled(0f, -1f, 0)
        queue.gamepadButtonDown(GameButton.BUTTON1, 0.5f, pad, 0)
        queue.gamepadButtonUp(GameButton.BUTTON1, pad, 0)
        queue.gamepadJoystickMoved(GameStick.RIGHT, 0.25f, -0.75f, pad, 0)
        queue.gamepadTriggerMoved(GameButton.R2, 0.3f, pad, 0)
        queue.drain(listOf(recorder))
        assertEquals(
            listOf(
                "keyRepeat B",
                "touchDown 1.0 2.0 POINTER2",
                "dragged 3.0 4.0 POINTER2",
                "touchUp 5.0 6.0 POINTER2",
                "moved 7.0 8.0",
                "scrolled 0.0 -1.0",
                "pressed BUTTON1 0.5 2",
                "released BUTTON1 2",
                "stick RIGHT 0.25 -0.75 2",
                "trigger R2 0.3 2",
            ),
            recorder.events
        )
    }

    @Test
    fun test_handled_event_stops_propagation() {
        val queue = InputQueueProcessor()
        val first = Recorder(handles = true)
        val second = Recorder()
        queue.keyDown(Key.A, 0)
        queue.drain(listOf(first, second))
        assertEquals(listOf("keyDown A"), first.events)
        assertTrue(second.events.isEmpty())
    }

    @Test
    fun test_unhandled_event_reaches_every_processor() {
        val queue = InputQueueProcessor()
        val first = Recorder()
        val second = Recorder()
        queue.scrolled(1f, 2f, 0)
        queue.drain(listOf(first, second))
        assertEquals(listOf("scrolled 1.0 2.0"), first.events)
        assertEquals(listOf("scrolled 1.0 2.0"), second.events)
    }

    @Test
    fun test_current_event_time_matches_event_during_dispatch() {
        val queue = InputQueueProcessor()
        val recorder = Recorder(queue = queue)
        queue.keyDown(Key.A, 10)
        queue.keyUp(Key.A, 25)
        queue.drain(listOf(recorder))
        assertEquals(listOf(10L, 25L), recorder.times)
        assertEquals(25L, queue.currentEventTime)
    }

    @Test
    fun test_drain_without_processors_drops_events() {
        val queue = InputQueueProcessor()
        val recorder = Recorder()
        queue.keyDown(Key.A, 0)
        queue.drain(emptyList())
        queue.drain(listOf(recorder))
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun test_drained_events_are_not_redelivered() {
        val queue = InputQueueProcessor()
        val recorder = Recorder()
        queue.keyDown(Key.A, 0)
        queue.drain(listOf(recorder))
        queue.drain(listOf(recorder))
        assertEquals(1, recorder.events.size)
    }

    @Test
    fun test_recycled_events_do_not_leak_previous_values() {
        val queue = InputQueueProcessor()
        val recorder = Recorder()
        queue.touchDown(9f, 9f, Pointer.POINTER3, 0)
        queue.drain(listOf(recorder))
        queue.mouseMoved(1f, 1f, 0)
        queue.keyDown(Key.Z, 0)
        queue.drain(listOf(recorder))
        assertEquals(listOf("touchDown 9.0 9.0 POINTER3", "moved 1.0 1.0", "keyDown Z"), recorder.events)
    }

    @Test
    fun test_queue_grows_past_preallocated_pool() {
        val queue = InputQueueProcessor()
        val recorder = Recorder()
        repeat(2) {
            repeat(60) { queue.charTyped('a' + (it % 26), it.toLong()) }
            queue.drain(listOf(recorder))
        }
        assertEquals(120, recorder.events.size)
        assertEquals("char h", recorder.events[119])
    }
}
