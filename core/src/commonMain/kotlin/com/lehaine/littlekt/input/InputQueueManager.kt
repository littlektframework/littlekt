package com.lehaine.littlekt.input

import com.lehaine.littlekt.input.internal.InternalInputEvent
import com.lehaine.littlekt.input.internal.InternalInputEventType
import com.lehaine.littlekt.util.datastructure.Pool

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputQueueManager {

    val currentEventTime get() = _currentEventTime

    private val eventsPool = Pool(preallocate = 10, gen = { InternalInputEvent() })

    private val queue = mutableListOf<InternalInputEvent>()
    private val processingQueue = mutableListOf<InternalInputEvent>()
    private var _currentEventTime = 0L

    fun drain(processor: InputProcessor?) {
        if (processor == null) {
            queue.clear()
            return
        }
        processingQueue.addAll(queue)
        queue.clear()

        processingQueue.forEach {
            _currentEventTime = it.queueTime
            when (it.type) {
                InternalInputEventType.KEY_DOWN -> processor.keyDown(key = it.key)
                InternalInputEventType.KEY_UP -> processor.keyUp(key = it.key)
                InternalInputEventType.KEY_TYPED -> processor.keyTyped(character = it.typedChar)
                InternalInputEventType.TOUCH_DOWN -> processor.touchDown(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_UP -> processor.touchUp(it.x, it.y, it.pointer)
                InternalInputEventType.TOUCH_DRAGGED -> processor.touchDragged(it.x, it.y, it.pointer)
                InternalInputEventType.MOUSE_MOVED -> processor.mouseMoved(it.x, it.y)
                InternalInputEventType.SCROLLED -> processor.scrolled(it.x, it.y)
                InternalInputEventType.GAMEPAD_BUTTON_DOWN -> processor.gamepadButtonPressed(
                    it.gamepadButton,
                    it.gamepadButtonPressure,
                    it.gamepad
                )
                InternalInputEventType.GAMEPAD_BUTTON_UP -> processor.gamepadButtonReleased(
                    it.gamepadButton,
                    it.gamepad
                )
                InternalInputEventType.GAMEPAD_JOYSTICK_MOVED -> processor.gamepadJoystickMoved(
                    it.gamepadStick,
                    it.gamepadStickDistance,
                    it.gamepad
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

    fun gamepadButtonDown(button: GameButton, pressure: Float, gamepad: Int, time: Long) {
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

    fun gamepadButtonUp(button: GameButton, gamepad: Int, time: Long) {
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

    fun gamepadJoystickMoved(stick: GameStick, distance: Float, gamepad: Int, time: Long) {
        eventsPool.alloc {
            it.apply {
                type = InternalInputEventType.GAMEPAD_JOYSTICK_MOVED
                this.gamepadStick = stick
                this.gamepadStickDistance = distance
                this.gamepad = gamepad
                queueTime = time
            }
        }.also { queue.add(it) }
    }
}