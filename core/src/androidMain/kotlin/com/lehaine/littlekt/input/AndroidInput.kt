package com.lehaine.littlekt.input

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.View.OnTouchListener
import com.lehaine.littlekt.math.geom.Point
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidInput(private val androidCtx: Context) : Input, OnTouchListener, OnKeyListener {

    private val vibrator = androidCtx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val inputCache = InputCache()

    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors

    override val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads

    private val touchX = IntArray(MAX_TOUCHES)
    private val touchY = IntArray(MAX_TOUCHES)
    private val touchDeltaX = IntArray(MAX_TOUCHES)
    private val touchDeltaY = IntArray(MAX_TOUCHES)

    override val x: Int
        get() = touchX[0]
    override val y: Int
        get() = touchY[0]
    override val deltaX: Int
        get() = touchDeltaX[0]
    override val deltaY: Int
        get() = touchDeltaY[0]
    override val isTouching: Boolean
        get() = inputCache.isTouching
    override val justTouched: Boolean
        get() = inputCache.justTouched
    override val pressure: Float
        get() = getPressure(Pointer.POINTER1)
    override val axisLeftX: Float
        get() = getGamepadJoystickXDistance(GameStick.LEFT)
    override val axisLeftY: Float
        get() = getGamepadJoystickYDistance(GameStick.LEFT)
    override val axisRightX: Float
        get() = getGamepadJoystickXDistance(GameStick.RIGHT)
    override val axisRightY: Float
        get() = getGamepadJoystickYDistance(GameStick.RIGHT)

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            val id = event.getPointerId(i)
            val pointer = Pointer.cache[i]
            onTouch(pointer, id, event)
            v.performClick()
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
                val x = event.getX(id)
                val y = event.getY(id)
                touchDeltaX[id] = (touchX[id] - x).toInt()
                touchDeltaY[id] = (touchY[id] - y).toInt()
                touchX[id] = x.toInt()
                touchY[id] = y.toInt()
                inputCache.onMove(event.getX(id), event.getY(id), pointer)
            }
        }
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> inputCache.onKeyDown(keyCode.getKey)
            KeyEvent.ACTION_UP -> {
                inputCache.onKeyUp(keyCode.getKey)
                inputCache.onKeyType(keyCode.toChar())
            }
        }
        return false
    }

    fun update() {
        inputCache.processEvents(inputProcessors)
    }

    fun reset() {
        inputCache.reset()
    }


    override fun getX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) x else touchX[pointer.index]
    }

    override fun getY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) y else touchY[pointer.index]
    }

    override fun getDeltaX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaX else touchDeltaX[pointer.index]
    }

    override fun getDeltaY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaY else touchDeltaY[pointer.index]
    }

    override fun isTouched(pointer: Pointer): Boolean {
        return inputCache.isTouching(pointer)
    }

    override fun getPressure(pointer: Pointer): Float {
        return if (isTouched(pointer)) 1f else 0f
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        return inputCache.isKeyJustPressed(key)
    }

    override fun isKeyPressed(key: Key): Boolean {
        return inputCache.isKeyPressed(key)
    }

    override fun isKeyJustReleased(key: Key): Boolean {
        return inputCache.isKeyJustReleased(key)
    }

    override fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonJustPressed(button, gamepad)
    }

    override fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonPressed(button, gamepad)
    }

    override fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonJustReleased(button, gamepad)
    }

    override fun getGamepadButtonPressure(button: GameButton, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad][button] else 0f
    }

    override fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int): Point {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad][stick] else Point.ZERO
    }

    override fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad].getX(stick) else 0f
    }

    override fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad].getY(stick) else 0f
    }

    override fun setCursorPosition(x: Int, y: Int) = Unit

    override fun addInputProcessor(processor: InputProcessor) {
        _inputProcessors += processor
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        _inputProcessors -= processor
    }

    @SuppressLint("MissingPermission")
    override fun vibrate(duration: Duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration.inWholeMilliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(duration.inWholeMilliseconds)
        }
    }

    @SuppressLint("MissingPermission")
    override fun cancelVibrate() {
        vibrator.cancel()
    }

    companion object {
        private const val MAX_TOUCHES = 20
    }
}