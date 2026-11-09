package com.littlekt.input

import com.littlekt.math.Vec2f
import com.littlekt.math.geom.Point

internal class FakeInput(val cache: InputCache = InputCache(), gamepadCount: Int = 2) : Input {
    var eventTime = 0L

    override val x: Int = 0
    override val y: Int = 0
    override val deltaX: Int = 0
    override val deltaY: Int = 0
    override val isTouching: Boolean
        get() = cache.isTouching

    override val justTouched: Boolean
        get() = cache.justTouched

    override val pressure: Float = 0f
    override val currentEventTime: Long
        get() = eventTime

    override val inputProcessors = mutableListOf<InputProcessor>()
    override val gamepads: Array<GamepadInfo> = Array(gamepadCount) { GamepadInfo(it) }
    override val connectedGamepads: List<GamepadInfo>
        get() = gamepads.filter { it.connected }

    override val catchKeys: MutableList<Key> = mutableListOf()
    override val axisLeftX: Float = 0f
    override val axisLeftY: Float = 0f
    override val axisRightX: Float = 0f
    override val axisRightY: Float = 0f
    override val cursorLocked: Boolean = false

    override fun getX(pointer: Pointer): Int = 0

    override fun getY(pointer: Pointer): Int = 0

    override fun getDeltaX(pointer: Pointer): Int = 0

    override fun getDeltaY(pointer: Pointer): Int = 0

    override fun isJustTouched(pointer: Pointer): Boolean = cache.isJustTouched(pointer)

    override fun isTouching(pointer: Pointer): Boolean = cache.isTouching(pointer)

    override fun isTouching(totalPointers: Int): Boolean = cache.isTouching(totalPointers)

    override fun isTouchJustReleased(pointer: Pointer): Boolean = cache.isTouchJustReleased(pointer)

    override fun getPressure(pointer: Pointer): Float = 0f

    override fun isKeyJustPressed(key: Key): Boolean = cache.isKeyJustPressed(key)

    override fun isKeyPressed(key: Key): Boolean = cache.isKeyPressed(key)

    override fun isKeyJustReleased(key: Key): Boolean = cache.isKeyJustReleased(key)

    override fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean =
        cache.isGamepadButtonJustPressed(button, gamepad)

    override fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean =
        cache.isGamepadButtonPressed(button, gamepad)

    override fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean =
        cache.isGamepadButtonJustReleased(button, gamepad)

    override fun getGamepadButtonPressure(button: GameButton, gamepad: Int): Float =
        gamepads[gamepad][button]

    override fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int): Point =
        gamepads[gamepad][stick]

    override fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int): Float =
        gamepads[gamepad].getX(stick)

    override fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int): Float =
        gamepads[gamepad].getY(stick)

    override fun setCursorPosition(x: Int, y: Int) = Unit

    override fun lockCursor() = Unit

    override fun releaseCursor() = Unit

    override fun addInputProcessor(processor: InputProcessor) {
        inputProcessors += processor
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        inputProcessors -= processor
    }

    override fun showSoftKeyboard() = Unit

    override fun hideSoftKeyboard() = Unit

    fun drain() = cache.processEvents(inputProcessors)

    fun keyDown(vararg keys: Key) = keys.forEach { cache.onKeyDown(it) }

    fun keyUp(vararg keys: Key) = keys.forEach { cache.onKeyUp(it) }

    fun pressButton(button: GameButton, pressure: Float = 1f, gamepad: Int = 0) {
        val info = gamepads[gamepad]
        info.connected = true
        info.rawButtonsPressed[info.mapping.buttonToIndex.getValue(button)] = pressure
        cache.updateGamepadButtons(info)
    }

    fun setAxis(axis: GameAxis, value: Float, gamepad: Int = 0) {
        val info = gamepads[gamepad]
        info.connected = true
        info.rawAxes[info.mapping.axisToIndex.getValue(axis)] = value
    }
}

internal fun Float.str(): String = if (this % 1f == 0f) "${toInt()}.0" else toString()

internal fun Vec2f.str(): String = "(${x.str()}, ${y.str()})"
