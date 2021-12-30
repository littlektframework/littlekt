package com.lehaine.littlekt.input

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

    fun gamepadJoystickMoved(stick: GameStick, distance: Float, gamepad: Int): Boolean {
        return false
    }
}