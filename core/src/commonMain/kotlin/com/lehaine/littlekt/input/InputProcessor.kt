package com.lehaine.littlekt.input

import com.lehaine.littlekt.util.fastForEach

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
interface InputProcessor {
    fun keyDown(key: Key): Boolean {
        return false
    }

    fun keyUp(key: Key): Boolean {
        return false
    }

    fun keyTyped(character: Char): Boolean {
        return false
    }

    fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        return false
    }

    fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        return false
    }

    fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        return false
    }

    fun mouseMoved(screenX: Float, screenY: Float): Boolean {
        return false
    }

    fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        return false
    }

    fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
        return false
    }

    fun gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int): Boolean {
        return false
    }

    fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        return false
    }
}

class InputProcessBuilder {
    private val keyDown = mutableListOf<(Key) -> Unit>()
    private val keyUp = mutableListOf<(Key) -> Unit>()
    private val keyTyped = mutableListOf<(Char) -> Unit>()
    private val touchDown = mutableListOf<(Float, Float, Pointer) -> Unit>()
    private val touchUp = mutableListOf<(Float, Float, Pointer) -> Unit>()
    private val touchDragged = mutableListOf<(Float, Float, Pointer) -> Unit>()
    private val mouseMoved = mutableListOf<(Float, Float) -> Unit>()
    private val scrolled = mutableListOf<(Float, Float) -> Unit>()
    private val gameButtonPressed = mutableListOf<(GameButton, Float, Int) -> Unit>()
    private val gameButtonReleased = mutableListOf<(GameButton, Int) -> Unit>()
    private val gameJoystickMoved = mutableListOf<(GameStick, Float, Float, Int) -> Unit>()
    private val gamepadTriggerChanged = mutableListOf<(GameButton, Float, Int) -> Unit>()

    fun onKeyDown(action: (key: Key) -> Unit) {
        keyDown += action
    }

    fun onKeyUp(action: (key: Key) -> Unit) {
        keyUp += action
    }

    fun onKeyTyped(action: (character: Char) -> Unit) {
        keyTyped += action
    }

    fun onTouchDown(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Unit) {
        touchDown += action
    }

    fun onTouchUp(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Unit) {
        touchUp += action
    }

    fun onTouchDragged(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Unit) {
        touchDragged += action
    }

    fun onMouseMoved(action: (screenX: Float, screenY: Float) -> Unit) {
        mouseMoved += action
    }

    fun onScrolled(action: (amountX: Float, amountY: Float) -> Unit) {
        scrolled += action
    }

    fun onGamepadButtonPressed(action: (button: GameButton, pressure: Float, gamepad: Int) -> Unit) {
        gameButtonPressed += action
    }

    fun onGamepadButtonReleased(action: (button: GameButton, gamepad: Int) -> Unit) {
        gameButtonReleased += action
    }

    fun onGamepadJoystickMoved(action: (stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int) -> Unit) {
        gameJoystickMoved += action
    }

    fun onGamepadTriggerChanged(action: (button:GameButton, pressure:Float, gamepad:Int) -> Unit) {
        gamepadTriggerChanged += action
    }

    fun build() = object : InputProcessor {
        override fun keyDown(key: Key): Boolean {
            keyDown.fastForEach { it.invoke(key) }
            return super.keyDown(key)
        }

        override fun keyUp(key: Key): Boolean {
            keyUp.fastForEach { it.invoke(key) }
            return super.keyUp(key)
        }

        override fun keyTyped(character: Char): Boolean {
            keyTyped.fastForEach { it.invoke(character) }
            return super.keyTyped(character)
        }

        override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
            touchDown.fastForEach { it.invoke(screenX, screenY, pointer) }
            return super.touchDown(screenX, screenY, pointer)
        }

        override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
            touchUp.fastForEach { it.invoke(screenX, screenY, pointer) }
            return super.touchUp(screenX, screenY, pointer)
        }

        override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
            touchDragged.fastForEach { it.invoke(screenX, screenY, pointer) }
            return super.touchDragged(screenX, screenY, pointer)
        }

        override fun mouseMoved(screenX: Float, screenY: Float): Boolean {
            mouseMoved.fastForEach { it.invoke(screenX, screenY) }
            return super.mouseMoved(screenX, screenY)
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            scrolled.fastForEach { it.invoke(amountX, amountY) }
            return super.scrolled(amountX, amountY)
        }

        override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
            gameButtonPressed.fastForEach { it.invoke(button, pressure, gamepad) }
            return super.gamepadButtonPressed(button, pressure, gamepad)
        }

        override fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
            gameButtonReleased.fastForEach { it.invoke(button, gamepad) }
            return super.gamepadButtonReleased(button, gamepad)
        }

        override fun gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int): Boolean {
            gameJoystickMoved.fastForEach { it.invoke(stick, xAxis, yAxis, gamepad) }
            return super.gamepadJoystickMoved(stick, xAxis, yAxis, gamepad)
        }

        override fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int): Boolean {
            gamepadTriggerChanged.fastForEach { it.invoke(button, pressure, gamepad) }
            return super.gamepadTriggerChanged(button, pressure, gamepad)
        }
    }
}