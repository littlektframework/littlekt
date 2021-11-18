package com.lehaine.littlekt.input

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
interface InputProcessor {
    fun keyDown(key: Key): Boolean
    fun keyUp(key: Key): Boolean
    fun keyTyped(character: Char): Boolean
    fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean
    fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean
    fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean
    fun mouseMoved(screenX: Float, screenY: Float)
    fun scrolled(amountX: Float, amountY: Float): Boolean
}