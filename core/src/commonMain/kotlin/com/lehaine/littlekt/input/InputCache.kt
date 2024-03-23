package com.lehaine.littlekt.input

import com.lehaine.littlekt.math.isFuzzyEqual
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.internal.epochMillis
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputCache {

    private val pointerCache = Pointer.entries.toTypedArray()
    private val keyCache = Key.entries.toTypedArray()

    private val pointersTouching = mutableMapOf<Pointer, Boolean>()
    private val pointersJustTouched = mutableMapOf<Pointer, Boolean>()
    private val pointersJustReleased = mutableMapOf<Pointer, Boolean>()

    private val keysPressed = mutableMapOf<Key, Boolean>()
    private val keysJustPressed = mutableMapOf<Key, Boolean>()
    private val keysJustReleased = mutableMapOf<Key, Boolean>()

    private val gamepadButtonsLastState =
        mutableMapOf<Int, MutableMap<GameButton, Float>>()
    private val gamepadButtonsJustPressed = mutableMapOf<Int, MutableMap<GameButton, Boolean>>()
    private val gamepadButtonsJustReleased = mutableMapOf<Int, MutableMap<GameButton, Boolean>>()
    private val gamepadStickLastPosition = mutableMapOf<Int, MutableMap<GameStick, FloatArray>>()

    private var totalKeysPressed = 0
    private var touches = 0
    private var totalGamepadButtonsPressed = 0

    val anyKeyPressed: Boolean get() = totalKeysPressed > 0
    val anyGamepadButtonPressed get() = totalGamepadButtonsPressed > 0

    val isTouching: Boolean get() = touches > 0

    val currentEventTime: Long get() = queueManager.currentEventTime

    var anyKeyJustPressed = false
        private set
    var anyKeyJustReleased = false
        private set

    var anyGamepadButtonsJustPressed = false
        private set
    var anyGamepadButtonsJustReleased = false
        private set

    var justTouched = false
        private set

    var justTouchReleased = false
        private set

    private val queueManager = InputQueueProcessor()

    fun isJustTouched(pointer: Pointer): Boolean {
        return pointersJustTouched[pointer] ?: false
    }

    fun isTouching(pointer: Pointer): Boolean {
        return pointersTouching[pointer] ?: false
    }

    fun isTouching(totalPointers: Int): Boolean {
        var total = 0
        pointersTouching.values.forEach {
            if (it) {
                total++
            }
        }
        return total >= totalPointers
    }

    fun isTouchJustReleased(pointer: Pointer): Boolean {
        return pointersJustReleased[pointer] ?: false
    }

    fun isKeyJustPressed(key: Key): Boolean {
        return keysJustPressed[key] ?: false
    }

    fun isKeyPressed(key: Key): Boolean {
        return keysPressed[key] ?: false
    }

    fun isKeyJustReleased(key: Key): Boolean {
        return keysJustReleased[key] ?: false
    }

    fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean {
        return gamepadButtonsJustPressed.getOrPut(gamepad) { mutableMapOf() }[button] ?: false
    }

    fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean {
        return gamepadButtonsLastState.getOrPut(gamepad) { mutableMapOf() }[button] != 0f
    }

    fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        return gamepadButtonsJustReleased.getOrPut(gamepad) { mutableMapOf() }[button] ?: false
    }

    fun onTouchDown(x: Float, y: Float, pointer: Pointer) {
        if (pointersTouching[pointer] != true) {
            queueManager.touchDown(x, y, pointer, epochMillis())
            touches++
            justTouched = true
            pointersTouching[pointer] = true
            pointersJustTouched[pointer] = true
        }
    }

    fun onMove(x: Float, y: Float, pointer: Pointer) {
        if (touches > 0) {
            queueManager.touchDragged(x, y, pointer, epochMillis())
        } else {
            queueManager.mouseMoved(x, y, epochMillis())
        }
    }

    fun onTouchUp(x: Float, y: Float, pointer: Pointer) {
        if (pointersTouching[pointer] == true) {
            queueManager.touchUp(x, y, pointer, epochMillis())
            touches = max(0, touches - 1)
            justTouchReleased = true
            pointersTouching[pointer] = false
            pointersJustReleased[pointer] = true
        }
    }

    fun onKeyDown(key: Key) {
        if (keysPressed[key] != true) {
            queueManager.keyDown(key, epochMillis())
            totalKeysPressed++
            anyKeyJustPressed = true
            keysPressed[key] = true
            keysJustPressed[key] = true
        }
    }

    fun onKeyUp(key: Key) {
        if (keysPressed[key] == true) {
            queueManager.keyUp(key, epochMillis())
            totalKeysPressed--
            anyKeyJustReleased = true
            keysPressed[key] = false
            keysJustReleased[key] = true
        }
    }

    fun onCharTyped(char: Char) {
        queueManager.charTyped(char, epochMillis())
    }

    fun onKeyRepeat(key: Key) {
        queueManager.keyRepeat(key, epochMillis())
    }

    fun onGamepadButtonDown(button: GameButton, pressure: Float, gamepad: GamepadInfo) {
        queueManager.gamepadButtonDown(button, pressure, gamepad, epochMillis())
        totalGamepadButtonsPressed++
        anyGamepadButtonsJustPressed = true
        gamepadButtonsLastState.getOrPut(gamepad.index) { mutableMapOf() }[button] = pressure
        gamepadButtonsJustPressed.getOrPut(gamepad.index) { mutableMapOf() }[button] = true
    }

    fun onGamepadButtonUp(button: GameButton, gamepad: GamepadInfo) {
        queueManager.gamepadButtonUp(button, gamepad, epochMillis())
        totalGamepadButtonsPressed--
        anyGamepadButtonsJustReleased = true
        gamepadButtonsLastState.getOrPut(gamepad.index) { mutableMapOf() }[button] = 0f
        gamepadButtonsJustReleased.getOrPut(gamepad.index) { mutableMapOf() }[button] = true
    }

    fun onGamepadJoystickMoved(stick: GameStick, x: Float, y: Float, gamepad: GamepadInfo) {
        val pos = gamepadStickLastPosition.getOrPut(gamepad.index) { mutableMapOf() }.getOrPut(stick) { FloatArray(2) }
        pos[0] = x
        pos[1] = y
        queueManager.gamepadJoystickMoved(stick, x, y, gamepad, epochMillis())
    }

    fun onGamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: GamepadInfo) {
        gamepadButtonsLastState.getOrPut(gamepad.index) { mutableMapOf() }[button] = pressure
        queueManager.gamepadTriggerMoved(button, pressure, gamepad, epochMillis())
    }

    fun updateGamepadButtons(gamepad: GamepadInfo) {
        gamepad.mapping.buttonListOrder.forEach {
            val state = gamepad[it]
            val lastState = gamepadButtonsLastState.getOrPut(gamepad.index) { mutableMapOf() }[it] ?: 0f

            if (state != 0f && !isFuzzyEqual(state, lastState)) {
                onGamepadButtonDown(it, state, gamepad)
            } else if (state == 0f && lastState != 0f) {
                onGamepadButtonUp(it, gamepad)
            }
        }
    }

    fun updateGamepadStick(stick: GameStick, gamepad: GamepadInfo) {
        val x = gamepad.getX(stick)
        val y = gamepad.getY(stick)
        val pos = gamepadStickLastPosition.getOrPut(gamepad.index) { mutableMapOf() }.getOrPut(stick) { FloatArray(2) }
        if (!isFuzzyEqual(pos[0], x) || !isFuzzyEqual(pos[1], y)) {
            onGamepadJoystickMoved(stick, x, y, gamepad)
        }
    }

    fun updateGamepadTrigger(button: GameButton, gamepad: GamepadInfo) {
        val state = gamepad[button]
        val lastState = gamepadButtonsLastState.getOrPut(gamepad.index) { mutableMapOf() }[button] ?: 0f
        if (state != lastState) {
            onGamepadTriggerChanged(button, state, gamepad)
        }
    }

    fun onScroll(amountX: Float, amountY: Float) {
        queueManager.scrolled(amountX, amountY, epochMillis())
    }

    fun processEvents(processors: List<InputProcessor>) {
        queueManager.drain(processors)
    }

    fun reset() {
        if (justTouched || justTouchReleased) {
            pointerCache.forEach {
                pointersJustTouched[it] = false
                pointersJustReleased[it] = false
            }
        }

        if (anyKeyJustPressed || anyKeyJustReleased) {
            keyCache.forEach {
                keysJustPressed[it] = false
                keysJustReleased[it] = false
            }
        }
        if (anyGamepadButtonsJustPressed || anyGamepadButtonsJustReleased) {
            GameButton.BUTTONS.fastForEach { button ->
                gamepadButtonsJustPressed.values.forEach {
                    it[button] = false
                }
                gamepadButtonsJustReleased.values.forEach {
                    it[button] = false
                }
            }
        }
        anyKeyJustPressed = false
        anyKeyJustReleased = false
        anyGamepadButtonsJustPressed = false
        anyGamepadButtonsJustReleased = false
        justTouched = false
        justTouchReleased = false
    }

}