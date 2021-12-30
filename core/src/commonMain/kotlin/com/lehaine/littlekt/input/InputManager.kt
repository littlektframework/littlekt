package com.lehaine.littlekt.input

import com.lehaine.littlekt.util.internal.epochMillis
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class InputManager {

    private val pointerCache = Pointer.values()
    private val keyCache = Key.values()
    private val gamepadButtonCache = GameButton.values()
    private val gameStickCache = GameStick.values()

    private val justTouchedPointers = mutableMapOf<Pointer, Boolean>()

    private val keysPressed = mutableMapOf<Key, Boolean>()
    private val keysJustPressed = mutableMapOf<Key, Boolean>()
    private val keysJustReleased = mutableMapOf<Key, Boolean>()

    private val gamepadButtonsPressed =
        mutableMapOf<Int, MutableMap<GameButton, Boolean>>()
    private val gamepadButtonsJustPressed = mutableMapOf<Int, MutableMap<GameButton, Boolean>>()
    private val gamepadButtonsJustReleased = mutableMapOf<Int, MutableMap<GameButton, Boolean>>()

    private var totalKeysPressed = 0
    private var touches = 0
    private var totalGamepadButtonsPressed = 0

    val anyKeyPressed: Boolean get() = totalKeysPressed > 0
    val anyGamepadButtonPressed get() = totalGamepadButtonsPressed > 0

    val isTouching: Boolean get() = touches > 0

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

    private val queueManager = InputQueueManager()

    fun isTouching(pointer: Pointer): Boolean {
        return justTouchedPointers[pointer] ?: false
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
        return gamepadButtonsPressed.getOrPut(gamepad) { mutableMapOf() }[button] ?: false
    }

    fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        return gamepadButtonsJustReleased.getOrPut(gamepad) { mutableMapOf() }[button] ?: false
    }

    fun onTouchDown(x: Float, y: Float, pointer: Pointer) {
        queueManager.touchDown(x, y, pointer, epochMillis())
        touches++
        justTouched = true
        justTouchedPointers[pointer] = true
    }

    fun onMove(x: Float, y: Float, pointer: Pointer) {
        if (touches > 0) {
            queueManager.touchDragged(x, y, pointer, epochMillis())
        } else {
            queueManager.mouseMoved(x, y, epochMillis())
        }
    }

    fun onTouchUp(x: Float, y: Float, pointer: Pointer) {
        queueManager.touchUp(x, y, pointer, epochMillis())
        touches = max(0, touches - 1)
    }

    fun onKeyDown(key: Key) {
        queueManager.keyDown(key, epochMillis())
        totalKeysPressed++
        anyKeyJustPressed = true
        keysPressed[key] = true
        keysJustPressed[key] = true
    }

    fun onKeyUp(key: Key) {
        queueManager.keyUp(key, epochMillis())
        totalKeysPressed--
        anyKeyJustReleased = true
        keysPressed[key] = false
        keysJustReleased[key] = true
    }

    fun onKeyType(char: Char) {
        queueManager.keyTyped(char, epochMillis())
    }

    fun onGamepadButtonDown(button: GameButton, pressure: Float, gamepad: Int) {
        queueManager.gamepadButtonDown(button, pressure, gamepad, epochMillis())
        totalGamepadButtonsPressed++
        anyGamepadButtonsJustPressed = true
        gamepadButtonsPressed.getOrPut(gamepad) { mutableMapOf() }[button] = true
        gamepadButtonsJustPressed.getOrPut(gamepad) { mutableMapOf() }[button] = true
    }

    fun onGamepadButtonUp(button: GameButton, gamepad: Int) {
        queueManager.gamepadButtonUp(button, gamepad, epochMillis())
        totalGamepadButtonsPressed--
        anyGamepadButtonsJustReleased = true
        gamepadButtonsPressed.getOrPut(gamepad) { mutableMapOf() }[button] = false
        gamepadButtonsJustReleased.getOrPut(gamepad) { mutableMapOf() }[button] = true
    }

    fun onGamepadJoystickMoved(stick: GameStick, distance: Float, gamepad: Int) {
        queueManager.gamepadJoystickMoved(stick, distance, gamepad, epochMillis())
    }

    fun onScroll(amountX: Float, amountY: Float) {
        queueManager.scrolled(amountX, amountY, epochMillis())
    }

    fun processEvents(inputProcessor: InputProcessor? = null) {
        queueManager.drain(inputProcessor)
    }

    fun reset() {
        if (justTouched) {
            pointerCache.forEach {
                justTouchedPointers[it] = false
            }
        }

        if (anyKeyJustPressed || anyKeyJustReleased) {
            keyCache.forEach {
                keysJustPressed[it] = false
                keysJustReleased[it] = false
            }
        }
        if (anyGamepadButtonsJustPressed || anyGamepadButtonsJustReleased) {
            gamepadButtonCache.forEach { button ->
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
    }

}