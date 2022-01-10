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

enum class GameStick(val index: Int) {
    LEFT(0), RIGHT(1);
}

enum class GameAxis(val index: Int) {
    LX(0), LY(1),
    RX(2), RY(3),
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
    operator fun get(axis: GameAxis): Float = mapping.get(axis, this)
    operator fun get(stick: GameStick): Point = axesData[stick.index].apply {
        this.x = getX(stick)
        this.y = getY(stick)
    }

    fun getX(stick: GameStick) = when (stick) {
        GameStick.LEFT -> get(GameAxis.LX)
        GameStick.RIGHT -> get(GameAxis.RX)
    }

    fun getY(stick: GameStick) = when (stick) {
        GameStick.LEFT -> get(GameAxis.LY)
        GameStick.RIGHT -> get(GameAxis.RY)
    }

    override fun toString(): String = "Gamepad[$index][$name]" + mapping.toString(this)
}

abstract class GamepadMapping {
    abstract val id: String
    abstract val buttonListOrder: List<GameButton>
    abstract val axisListOrder: List<GameAxis>

    val buttonToIndex by lazy { buttonListOrder.mapIndexed { index, gameButton -> gameButton to index }.toMap() }
    val indexToButton by lazy { buttonListOrder.mapIndexed { index, gameButton -> index to gameButton }.toMap() }
    val axisToIndex by lazy { axisListOrder.mapIndexed { index, axis -> axis to index }.toMap() }
    val indexToAxis by lazy { axisListOrder.mapIndexed { index, axis -> index to axis }.toMap() }

    fun get(button: GameButton, info: GamepadInfo): Float =
        buttonToIndex[button]?.let { info.getRawButton(it) } ?: 0f

    fun get(axis: GameAxis, info: GamepadInfo): Float = axisToIndex[axis]?.let { info.getRawAxis(it) } ?: 0f

    fun GamepadInfo.getRawButton(index: Int): Float = this.rawButtonsPressed[index]
    fun GamepadInfo.getRawAxis(index: Int) = this.rawAxes.getOrElse(index) { 0f }

    fun toString(info: GamepadInfo) = "$id(" + GameButton.values().joinToString(", ") {
        "${it.name}=${get(it, info)}"
    } + ")"
}

open class StandardGamepadMapping : GamepadMapping() {
    companion object : StandardGamepadMapping()

    override val id = "Standard"

    override val buttonListOrder: List<GameButton> = listOf(
        GameButton.BUTTON0,
        GameButton.BUTTON1,
        GameButton.BUTTON2,
        GameButton.BUTTON3,
        GameButton.L1,
        GameButton.R1,
        GameButton.L2,
        GameButton.R2,
        GameButton.SELECT,
        GameButton.START,
        GameButton.L3,
        GameButton.R3,
        GameButton.UP,
        GameButton.DOWN,
        GameButton.LEFT,
        GameButton.RIGHT,
        GameButton.SYSTEM
    )

    override val axisListOrder: List<GameAxis> = listOf(
        GameAxis.LX,
        GameAxis.LY,
        GameAxis.RX,
        GameAxis.RY
    )
}

interface Input {

    val x: Int
    val y: Int
    val deltaX: Int
    val deltaY: Int

    val isTouching: Boolean
    val justTouched: Boolean

    val pressure: Float

    val inputProcessors: List<InputProcessor>

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

    /**
     * Determines if the key is was just pressed within the past frame.
     * @return `true` if the [Key] was just pressed; `false` otherwise.
     */
    fun isKeyJustPressed(key: Key): Boolean

    /**
     * Determines if the key is currently pressed down.
     * @return `true` if the [Key] is currently pressed; `false` otherwise.
     */
    fun isKeyPressed(key: Key): Boolean

    /**
     * Determines if the key was just released within the past frame.
     * @return `true` if the [Key] was just released; `false` otherwise.
     */
    fun isKeyJustReleased(key: Key): Boolean

    /**
     * Determines if any of the specified [keys] are currently pressed.
     * @return `true` if any of the keys are pressed; `false` otherwise.
     */
    fun areAnyKeysPressed(vararg keys: Key): Boolean = keys.any { isKeyPressed(it) }

    /**
     * Determines if all of the specified [keys] are currently pressed.
     * @return `true` if all of the keys are pressed; `false` otherwise.
     */
    fun areAllKeysPressed(vararg keys: Key): Boolean = keys.all { isKeyPressed(it) }

    /**
     * Determines if all of the specified [keys] are **NOT** currently pressed.
     * @return `true` if no keys are pressed; `false` otherwise.
     */
    fun areNoKeysPressed(vararg keys: Key): Boolean = keys.none { isKeyPressed(it) }

    fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int = 0): Boolean
    fun isGamepadButtonPressed(button: GameButton, gamepad: Int = 0): Boolean
    fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int = 0): Boolean
    fun getGamepadButtonPressure(button: GameButton, gamepad: Int = 0): Float
    fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int = 0): Point
    fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int = 0): Float
    fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int = 0): Float

    fun setCursorPosition(x: Int, y: Int)

    /**
     * Add a [InputProcessor] to receive input callbacks.
     */
    fun addInputProcessor(processor: InputProcessor)

    /**
     * Creates and adds a new [InputProcessor] to receive input callbacks using a [InputProcessBuilder].
     * @return the newly created [InputProcessor]
     */
    fun inputProcessor(setup: InputProcessBuilder.() -> Unit): InputProcessor {
        val builder = InputProcessBuilder()
        builder.setup()
        return builder.build().also { addInputProcessor(it) }
    }

    /**
     * Remove a [InputProcessor] to from receiving input callbacks.
     */
    fun removeInputProcessor(processor: InputProcessor)
}