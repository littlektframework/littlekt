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

    private val justTouchedPointers = mutableMapOf<Pointer, Boolean>()
    private val keysPressed = mutableMapOf<Key, Boolean>()
    private val keysJustPressed = mutableMapOf<Key, Boolean>()

    private var totalKeysPressed = 0
    private var touches = 0

    val anyKeyPressed: Boolean get() = totalKeysPressed > 0

    val isTouching: Boolean get() = touches > 0

    var anyKeyJustPressed = false
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
        keysPressed[key] = false
    }

    fun onKeyType(char: Char) {
        queueManager.keyTyped(char, epochMillis())
    }

    fun onScroll(amountX: Float, amountY: Float) {
        queueManager.scrolled(amountX, amountY, epochMillis())
    }

    fun processEvents(inputProcessor: InputProcessor? = null) {
        queueManager.drain(inputProcessor)
    }

    fun reset() {
        if(justTouched) {
            pointerCache.forEach {
                justTouchedPointers[it] = false
            }
        }

        if(anyKeyJustPressed) {
            keyCache.forEach {
                keysJustPressed[it] = false
            }
        }
        anyKeyJustPressed = false
        justTouched = false
    }

}