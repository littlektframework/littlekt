package com.littlekt.input

import com.littlekt.input.internal.InternalInputEvent
import com.littlekt.input.internal.InternalInputEventType
import com.littlekt.util.datastructure.Pool
import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputQueueProcessor {

    val currentEventTime
        get() = _currentEventTime

    private val eventsPool =
        Pool(reset = { it.reset() }, preallocate = 25, gen = { InternalInputEvent() })

    private val queue = mutableListOf<InternalInputEvent>()
    private val processingQueue = mutableListOf<InternalInputEvent>()
    private var _currentEventTime = 0L

    fun drain(processors: List<InputProcessor>) {
        if (processors.isEmpty()) {
            eventsPool.free(queue)
            queue.clear()
            return
        }
        processingQueue.addAll(queue)
        queue.clear()

        processingQueue.forEach {
            _currentEventTime = it.queueTime
            when (it.type) {
                InternalInputEventType.KEY_DOWN -> processors.keyDown(key = it.key)
                InternalInputEventType.KEY_UP -> processors.keyUp(key = it.key)
                InternalInputEventType.KEY_REPEAT -> processors.keyRepeat(key = it.key)
                InternalInputEventType.CHAR_TYPED -> processors.charTyped(character = it.typedChar)
                InternalInputEventType.TOUCH_DOWN -> processors.touchDown(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_UP -> processors.touchUp(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_DRAGGED ->
                    processors.touchDragged(it.x, it.y, it.pointer)
                InternalInputEventType.MOUSE_MOVED -> processors.mouseMoved(it.x, it.y)
                InternalInputEventType.SCROLLED -> processors.scrolled(it.x, it.y)
                InternalInputEventType.GAMEPAD_BUTTON_DOWN ->
                    processors.gamepadButtonPressed(
                        it.gamepadButton,
                        it.gamepadButtonPressure,
                        it.gamepad.index
                    )
                InternalInputEventType.GAMEPAD_BUTTON_UP ->
                    processors.gamepadButtonReleased(it.gamepadButton, it.gamepad.index)
                InternalInputEventType.GAMEPAD_JOYSTICK_MOVED ->
                    processors.gamepadJoystickMoved(it.gamepadStick, it.x, it.y, it.gamepad.index)
                InternalInputEventType.GAMEPAD_TRIGGER_CHANGED ->
                    processors.gamepadTriggerChanged(
                        it.gamepadButton,
                        it.gamepadButtonPressure,
                        it.gamepad.index
                    )
            }
        }
        eventsPool.free(processingQueue)
        processingQueue.clear()
    }

    fun keyDown(key: Key, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.KEY_DOWN
                    this.key = key
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun keyUp(key: Key, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.KEY_UP
                    this.key = key
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun keyRepeat(key: Key, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.KEY_REPEAT
                    this.key = key
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun charTyped(character: Char, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.CHAR_TYPED
                    typedChar = character
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun touchDown(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.TOUCH_DOWN
                    x = screenX
                    y = screenY
                    queueTime = time
                    this.pointer = pointer
                }
            }
            .also { queue.add(it) }
    }

    fun touchUp(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.TOUCH_UP
                    x = screenX
                    y = screenY
                    queueTime = time
                    this.pointer = pointer
                }
            }
            .also { queue.add(it) }
    }

    fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.TOUCH_DRAGGED
                    x = screenX
                    y = screenY
                    queueTime = time
                    this.pointer = pointer
                }
            }
            .also { queue.add(it) }
    }

    fun mouseMoved(screenX: Float, screenY: Float, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.MOUSE_MOVED
                    x = screenX
                    y = screenY
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun scrolled(amountX: Float, amountY: Float, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.SCROLLED
                    x = amountX
                    y = amountY
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun gamepadButtonDown(button: GameButton, pressure: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.GAMEPAD_BUTTON_DOWN
                    this.gamepadButton = button
                    this.gamepadButtonPressure = pressure
                    this.gamepad = gamepad
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun gamepadButtonUp(button: GameButton, gamepad: GamepadInfo, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.GAMEPAD_BUTTON_UP
                    this.gamepadButton = button
                    this.gamepadButtonPressure = 0f
                    this.gamepad = gamepad
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun gamepadJoystickMoved(
        stick: GameStick,
        x: Float,
        y: Float,
        gamepad: GamepadInfo,
        time: Long
    ) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.GAMEPAD_JOYSTICK_MOVED
                    this.gamepadStick = stick
                    this.x = x
                    this.y = y
                    this.gamepad = gamepad
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    fun gamepadTriggerMoved(button: GameButton, pressure: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool
            .alloc {
                it.apply {
                    type = InternalInputEventType.GAMEPAD_TRIGGER_CHANGED
                    this.gamepadButton = button
                    this.gamepadButtonPressure = pressure
                    this.gamepad = gamepad
                    queueTime = time
                }
            }
            .also { queue.add(it) }
    }

    private fun List<InputProcessor>.keyDown(key: Key) {
        fastForEach { if (it.keyDown(key)) return }
    }

    private fun List<InputProcessor>.keyUp(key: Key) {
        fastForEach { if (it.keyUp(key)) return }
    }

    private fun List<InputProcessor>.keyRepeat(key: Key) {
        fastForEach { if (it.keyRepeat(key)) return }
    }

    private fun List<InputProcessor>.charTyped(character: Char) {
        fastForEach { if (it.charTyped(character)) return }
    }

    private fun List<InputProcessor>.touchDown(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { if (it.touchDown(screenX, screenY, pointer)) return }
    }

    private fun List<InputProcessor>.touchUp(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { if (it.touchUp(screenX, screenY, pointer)) return }
    }

    private fun List<InputProcessor>.touchDragged(
        screenX: Float,
        screenY: Float,
        pointer: Pointer
    ) {
        fastForEach { if (it.touchDragged(screenX, screenY, pointer)) return }
    }

    private fun List<InputProcessor>.mouseMoved(screenX: Float, screenY: Float) {
        fastForEach { if (it.mouseMoved(screenX, screenY)) return }
    }

    private fun List<InputProcessor>.scrolled(amountX: Float, amountY: Float) {
        fastForEach { if (it.scrolled(amountX, amountY)) return }
    }

    private fun List<InputProcessor>.gamepadButtonPressed(
        button: GameButton,
        pressure: Float,
        gamepad: Int
    ) {
        fastForEach { if (it.gamepadButtonPressed(button, pressure, gamepad)) return }
    }

    private fun List<InputProcessor>.gamepadButtonReleased(button: GameButton, gamepad: Int) {
        fastForEach { if (it.gamepadButtonReleased(button, gamepad)) return }
    }

    private fun List<InputProcessor>.gamepadJoystickMoved(
        stick: GameStick,
        xAxis: Float,
        yAxis: Float,
        gamepad: Int
    ) {
        fastForEach { if (it.gamepadJoystickMoved(stick, xAxis, yAxis, gamepad)) return }
    }

    private fun List<InputProcessor>.gamepadTriggerChanged(
        button: GameButton,
        pressure: Float,
        gamepad: Int
    ) {
        fastForEach { if (it.gamepadTriggerChanged(button, pressure, gamepad)) return }
    }
}
