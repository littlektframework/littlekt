package com.lehaine.littlekt.input

import com.lehaine.littlekt.math.geom.MutablePoint
import com.lehaine.littlekt.math.geom.Point

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
enum class Pointer {
    POINTER1, POINTER2, POINTER3
}

enum class Key {
    ANY_KEY,
    BACKSPACE,
    TAB,
    ENTER,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    CTRL_LEFT,
    CTRL_RIGHT,
    ALT_LEFT,
    ALT_RIGHT,
    PAUSE_BREAK,
    CAPS_LOCK,
    ESCAPE,
    PAGE_UP,
    SPACE,
    PAGE_DOWN,
    END,
    HOME,
    ARROW_LEFT,
    ARROW_UP,
    ARROW_RIGHT,
    ARROW_DOWN,
    PRINT_SCREEN,
    INSERT,
    DELETE,
    NUM0,
    NUM1,
    NUM2,
    NUM3,
    NUM4,
    NUM5,
    NUM6,
    NUM7,
    NUM8,
    NUM9,
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,
    J,
    K,
    L,
    M,
    N,
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W,
    X,
    Y,
    Z,
    LEFT_OS,
    RIGHT_OS,
    NUMPAD0,
    NUMPAD1,
    NUMPAD2,
    NUMPAD3,
    NUMPAD4,
    NUMPAD5,
    NUMPAD6,
    NUMPAD7,
    NUMPAD8,
    NUMPAD9,
    MULTIPLY,
    ADD,
    SUBTRACT,
    DECIMAL_POINT,
    DIVIDE,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12,
    NUM_LOCK,
    SCROLL_LOCK,
    SEMI_COLON,
    EQUAL_SIGN,
    COMMA,
    DASH,
    PERIOD,
    FORWARD_SLASH,
    BRACKET_LEFT,
    BACK_SLASH,
    BRACKET_RIGHT,
    SINGLE_QUOTE,
}

enum class GameStick(val id: Int) {
    LEFT(0), RIGHT(1);

    companion object {
        val STICKS = values()
    }
}

enum class GameButton(val index: Int) {
    LEFT(0), RIGHT(1), UP(2), DOWN(3),
    BUTTON0(4), // XBox: A, Playstation: Cross
    BUTTON1(5), // XBox: B, Playstation: Circle
    BUTTON2(6), // XBox: X, Playstation: Square
    BUTTON3(7), // XBox: Y, Playstation: Triangle
    SELECT(8), START(9), SYSTEM(10),
    L1(11), R1(12),
    L2(13), R2(14),
    L3(15), R3(16),
    LX(17), LY(18),
    RX(19), RY(20),
    BUTTON4(24), BUTTON5(25), BUTTON6(26), BUTTON7(27), BUTTON8(28);

    companion object {
        val BUTTONS = values()
        val MAX = 32

        val LEFT_TRIGGER get() = L2
        val RIGHT_TRIGGER get() = R2

        val LEFT_THUMB get() = L3
        val RIGHT_THUMB get() = R3

        val BACK get() = SELECT

        val XBOX_A get() = BUTTON0
        val XBOX_B get() = BUTTON1
        val XBOX_X get() = BUTTON2
        val XBOX_Y get() = BUTTON3

        val PS_CROSS get() = BUTTON0
        val PS_CIRCLE get() = BUTTON1
        val PS_SQUARE get() = BUTTON2
        val PS_TRIANGLE get() = BUTTON3
    }
}


class GamepadInfo(
    val index: Int = 0,
    var connected: Boolean = false,
    var name: String = "unknown",
    var mapping: GamepadMapping = StandardGamepadMapping,
    val rawButtonsPressed: FloatArray = FloatArray(64),
    val rawAxes: FloatArray = FloatArray(16),
) {
    private val axesData: Array<MutablePoint> = Array(2) { MutablePoint(0f, 0f) }

    operator fun get(button: GameButton): Float = mapping.get(button, this)
    operator fun get(stick: GameStick): Point = axesData[stick.id].apply {
        this.x = getX(stick)
        this.y = getY(stick)
    }

    fun getX(stick: GameStick) = when (stick) {
        GameStick.LEFT -> get(GameButton.LX)
        GameStick.RIGHT -> get(GameButton.RX)
    }

    fun getY(stick: GameStick) = when (stick) {
        GameStick.LEFT -> get(GameButton.LY)
        GameStick.RIGHT -> get(GameButton.RY)
    }

    override fun toString(): String = "Gamepad[$index][$name]" + mapping.toString(this)
}

abstract class GamepadMapping {
    abstract val id: String
    abstract fun get(button: GameButton, info: GamepadInfo): Float

    fun GamepadInfo.getRawButton(index: Int): Float = this.rawButtonsPressed[index]
    fun GamepadInfo.getRawPressureButton(index: Int) = getRawButton(index)
    fun GamepadInfo.getRawAxis(index: Int) = this.rawAxes.getOrElse(index) { 0f }

    fun toString(info: GamepadInfo) = "$id(" + GameButton.values().joinToString(", ") {
        "${it.name}=${get(it, info)}"
    } + ")"
}

open class StandardGamepadMapping : GamepadMapping() {
    companion object : StandardGamepadMapping()

    override val id = "Standard"

    override fun get(button: GameButton, info: GamepadInfo): Float {
        return when (button) {
            GameButton.BUTTON0 -> info.getRawButton(0)
            GameButton.BUTTON1 -> info.getRawButton(1)
            GameButton.BUTTON2 -> info.getRawButton(2)
            GameButton.BUTTON3 -> info.getRawButton(3)
            GameButton.L1 -> info.getRawButton(4)
            GameButton.R1 -> info.getRawButton(5)
            GameButton.L2 -> info.getRawButton(6)
            GameButton.R2 -> info.getRawButton(7)
            GameButton.SELECT -> info.getRawButton(8)
            GameButton.START -> info.getRawButton(9)
            GameButton.L3 -> info.getRawButton(10)
            GameButton.R3 -> info.getRawButton(11)
            GameButton.UP -> info.getRawButton(12)
            GameButton.DOWN -> info.getRawButton(13)
            GameButton.LEFT -> info.getRawButton(14)
            GameButton.RIGHT -> info.getRawButton(15)
            GameButton.SYSTEM -> info.getRawButton(16)
            GameButton.LX -> info.getRawAxis(0)
            GameButton.LY -> info.getRawAxis(1)
            GameButton.RX -> info.getRawAxis(2)
            GameButton.RY -> info.getRawAxis(3)
            else -> 0f
        }
    }
}

interface Input {

    val x: Int
    val y: Int
    val deltaX: Int
    val deltaY: Int

    val isTouching: Boolean
    val justTouched: Boolean

    val pressure: Float

    var inputProcessor: InputProcessor?

    val gamepads: Array<GamepadInfo>
    val connectedGamepads: List<GamepadInfo>

    val axisLeftX: Float
    val axisLeftY: Float
    val axisRightX: Float
    val axisRightY: Float

    fun getX(pointer: Pointer): Int
    fun getY(pointer: Pointer): Int

    fun getDeltaX(pointer: Pointer): Int
    fun getDeltaY(pointer: Pointer): Int

    fun isTouched(pointer: Pointer): Boolean

    fun getPressure(pointer: Pointer): Float

    fun isKeyJustPressed(key: Key): Boolean
    fun isKeyPressed(key: Key): Boolean
    fun isKeyJustReleased(key: Key): Boolean

    fun areAnyKeysPressed(vararg keys: Key): Boolean = keys.any { isKeyPressed(it) }
    fun areAllKeysPressed(vararg keys: Key): Boolean = keys.all { isKeyPressed(it) }
    fun areNoKeysPressed(vararg keys: Key): Boolean = keys.none { isKeyPressed(it) }

    fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int = 0): Boolean
    fun isGamepadButtonPressed(button: GameButton, gamepad: Int = 0): Boolean
    fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int = 0): Boolean
    fun getGamepadButtonPressure(button: GameButton, gamepad: Int = 0): Float
    fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int = 0): Point
    fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int = 0): Float
    fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int = 0): Float

    fun setCursorPosition(x: Int, y: Int)
}