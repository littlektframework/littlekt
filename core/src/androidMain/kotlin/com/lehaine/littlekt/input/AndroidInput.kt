package com.lehaine.littlekt.input

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import com.lehaine.littlekt.math.geom.Point

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidInput : Input, OnTouchListener, OnKeyListener, OnGenericMotionListener {

    private val inputCache = InputCache()

    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors

    override val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads

    override val x: Int
        get() = TODO("Not yet implemented")
    override val y: Int
        get() = TODO("Not yet implemented")
    override val deltaX: Int
        get() = TODO("Not yet implemented")
    override val deltaY: Int
        get() = TODO("Not yet implemented")
    override val isTouching: Boolean
        get() = TODO("Not yet implemented")
    override val justTouched: Boolean
        get() = TODO("Not yet implemented")
    override val pressure: Float
        get() = TODO("Not yet implemented")
    override val axisLeftX: Float
        get() = getGamepadJoystickXDistance(GameStick.LEFT)
    override val axisLeftY: Float
        get() = getGamepadJoystickYDistance(GameStick.LEFT)
    override val axisRightX: Float
        get() = getGamepadJoystickXDistance(GameStick.RIGHT)
    override val axisRightY: Float
        get() = getGamepadJoystickYDistance(GameStick.RIGHT)

    override fun getX(pointer: Pointer): Int {
        TODO("Not yet implemented")
    }

    override fun getY(pointer: Pointer): Int {
        TODO("Not yet implemented")
    }

    override fun getDeltaX(pointer: Pointer): Int {
        TODO("Not yet implemented")
    }

    override fun getDeltaY(pointer: Pointer): Int {
        TODO("Not yet implemented")
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            val id = event.getPointerId(i)
            val pointer = Pointer.cache[i]
            onTouch(pointer, id, event)
        }
        return true
    }

    private fun onTouch(pointer: Pointer, id: Int, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                inputCache.onTouchDown(event.getX(id), event.getY(id), pointer)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_OUTSIDE -> {
                inputCache.onTouchUp(event.getX(id), event.getY(id), pointer)
            }
            MotionEvent.ACTION_MOVE -> {
                inputCache.onMove(event.getX(id), event.getY(id), pointer)
            }
        }
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onGenericMotion(v: View?, event: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTouched(pointer: Pointer): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPressure(pointer: Pointer): Float {
        TODO("Not yet implemented")
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        TODO("Not yet implemented")
    }

    override fun isKeyPressed(key: Key): Boolean {
        TODO("Not yet implemented")
    }

    override fun isKeyJustReleased(key: Key): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun getGamepadButtonPressure(button: GameButton, gamepad: Int): Float {
        TODO("Not yet implemented")
    }

    override fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int): Point {
        TODO("Not yet implemented")
    }

    override fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int): Float {
        TODO("Not yet implemented")
    }

    override fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int): Float {
        TODO("Not yet implemented")
    }

    override fun setCursorPosition(x: Int, y: Int) {
        TODO("Not yet implemented")
    }

    override fun addInputProcessor(processor: InputProcessor) {
        TODO("Not yet implemented")
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        TODO("Not yet implemented")
    }

    companion object {
        const val NUM_TOUCHES = 20
    }
}