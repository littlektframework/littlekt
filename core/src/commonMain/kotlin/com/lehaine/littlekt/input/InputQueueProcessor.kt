package com.lehaine.littlekt.input

import com.lehaine.littlekt.input.internal.InternalInputEvent
import com.lehaine.littlekt.input.internal.InternalInputEventType
import com.lehaine.littlekt.util.datastructure.Pool
import com.lehaine.littlekt.util.fastForEach

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputQueueProcessor {

    val currentEventTime get() = _currentEventTime

    private val eventsPool = Pool(preallocate = 10, gen = { InternalInputEvent() })

    private val queue = mutableListOf<InternalInputEvent>()
    private val processingQueue = mutableListOf<InternalInputEvent>()
    private var _currentEventTime = 0L

    fun drain(processors: List<InputProcessor>) {
        if (processors.isEmpty()) {
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
                InternalInputEventType.KEY_TYPED -> processors.keyTyped(character = it.typedChar)
                InternalInputEventType.TOUCH_DOWN -> processors.touchDown(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_UP -> processors.touchUp(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_DRAGGED -> processors.touchDragged(it.x, it.y, it.pointer)
                InternalInputEventType.MOUSE_MOVED -> processors.mouseMoved(it.x, it.y)
                InternalInputEventType.SCROLLED -> processors.scrolled(it.x, it.y)
                InternalInputEventType.GAMEPAD_BUTTON_DOWN -> processors.gamepadButtonPressed(
                    it.gamepadButton,
                    it.gamepadButtonPressure,
                    it.gamepad.index
                )
                InternalInputEventType.GAMEPAD_BUTTON_UP -> processors.gamepadButtonReleased(
                    it.gamepadButton,
                    it.gamepad.index
                )
                InternalInputEventType.GAMEPAD_JOYSTICK_MOVED -> processors.gamepadJoystickMoved(
                    it.gamepadStick,
                    it.x,
                    it.y,
                    it.gamepad.index
                )
                InternalInputEventType.GAMEPAD_TRIGGER_CHANGED -> processors.gamepadTriggerChanged(
                    it.gamepadButton,
                    it.gamepadButtonPressure,
                    it.gamepad.index
                )
            }
        }
        processingQueue.clear()
    }

    fun keyDown(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_DOWN
                this.key = key
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun keyUp(key: Key, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_UP
                this.key = key
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun keyTyped(character: Char, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.KEY_TYPED
                typedChar = character
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun touchDown(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_DOWN
                x = screenX
                y = screenY
                queueTime = time
                this.pointer = pointer
            }
        }.also { queue.add(it) }
    }

    fun touchUp(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_UP
                x = screenX
                y = screenY
                queueTime = time
                this.pointer = pointer
            }
        }.also { queue.add(it) }
    }

    fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.TOUCH_DRAGGED
                x = screenX
                y = screenY
                queueTime = time
                this.pointer = pointer
            }
        }.also { queue.add(it) }
    }

    fun mouseMoved(screenX: Float, screenY: Float, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.MOUSE_MOVED
                x = screenX
                y = screenY
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun scrolled(amountX: Float, amountY: Float, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.SCROLLED
                x = amountX
                y = amountY
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun gamepadButtonDown(button: GameButton, pressure: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_BUTTON_DOWN
                this.gamepadButton = button
                this.gamepadButtonPressure = pressure
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun gamepadButtonUp(button: GameButton, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_BUTTON_UP
                this.gamepadButton = button
                this.gamepadButtonPressure = 0f
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun gamepadJoystickMoved(stick: GameStick, x: Float, y: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_JOYSTICK_MOVED
                this.gamepadStick = stick
                this.x = x
                this.y = y
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    fun gamepadTriggerMoved(button: GameButton, pressure: Float, gamepad: GamepadInfo, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_TRIGGER_CHANGED
                this.gamepadButton = button
                this.gamepadButtonPressure = pressure
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { queue.add(it) }
    }

    private fun List<InputProcessor>.keyDown(key: Key) {
        fastForEach { it.keyDown(key) }
    }

    private fun List<InputProcessor>.keyUp(key: Key) {
        fastForEach { it.keyUp(key) }
    }

    private fun List<InputProcessor>.keyTyped(character: Char) {
        fastForEach { it.keyTyped(character) }
    }

    private fun List<InputProcessor>.touchDown(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { it.touchDown(screenX, screenY, pointer) }
    }

    private fun List<InputProcessor>.touchUp(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { it.touchUp(screenX, screenY, pointer) }
    }

    private fun List<InputProcessor>.touchDragged(screenX: Float, screenY: Float, pointer: Pointer) {
        fastForEach { it.touchDragged(screenX, screenY, pointer) }
    }

    private fun List<InputProcessor>.mouseMoved(screenX: Float, screenY: Float) {
        fastForEach { it.mouseMoved(screenX, screenY) }
    }

    private fun List<InputProcessor>.scrolled(amountX: Float, amountY: Float) {
        fastForEach { it.scrolled(amountX, amountY) }
    }

    private fun List<InputProcessor>.gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int) {
        fastForEach { it.gamepadButtonPressed(button, pressure, gamepad) }
    }

    private fun List<InputProcessor>.gamepadButtonReleased(button: GameButton, gamepad: Int) {
        fastForEach { it.gamepadButtonReleased(button, gamepad) }
    }

    private fun List<InputProcessor>.gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int) {
        fastForEach { it.gamepadJoystickMoved(stick, xAxis, yAxis, gamepad) }
    }

    private fun List<InputProcessor>.gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int) {
        fastForEach { it.gamepadTriggerChanged(button, pressure, gamepad) }
    }
}