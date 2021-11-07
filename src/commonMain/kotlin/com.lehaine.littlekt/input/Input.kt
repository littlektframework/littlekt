package com.lehaine.littlekt.input

import com.lehaine.littlekt.math.Vector2

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
enum class TouchSignal {
    TOUCH1, TOUCH2, TOUCH3
}

enum class Key {
    ANY_KEY,
    BACKSPACE,
    TAB,
    ENTER,
    SHIFT,
    CTRL,
    ALT,
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
    LEFT_WINDOW_KEY,
    RIGHT_WINDOW_KEY,
    SELECT_KEY,
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
    MY_COMPUTER,
    MY_CALCULATOR,
    SEMI_COLON,
    EQUAL_SIGN,
    COMMA,
    DASH,
    PERIOD,
    FORWARD_SLASH,
    OPEN_BRACKET,
    BACK_SLASH,
    CLOSE_BRACKET,
    SINGLE_QUOTE,
    DPAD_CENTER
}

interface InputManager {

    fun record()
    fun reset()
}

interface Input {

    /**
     * Is the [key] just pressed?
     *
     * It returns true once and will return true only if
     * the key is released then pressed again.
     *
     * This method should be used to count action trigger only
     * once (ie: starting an action like opening a door)
     */
    fun isKeyJustPressed(key: Key): Boolean

    /**
     * Is the [key] currently actually pressed?
     *
     * This method should be used to know when the key is pressed
     * and running an action until the key is not released.
     * (ie: running while the key is pressed, stop when it's not)
     */
    fun isKeyPressed(key: Key): Boolean

    /**
     * Is any of [keys] passed in parameter are actually pressed?
     */
    fun isAnyKeysPressed(vararg keys: Key): Boolean = keys.any { isKeyPressed(it) }

    /**
     * Is all of [keys] passed in parameter are been just pressed?
     */
    fun isAllKeysPressed(vararg keys: Key): Boolean = keys.all { isKeyPressed(it) }

    /**
     * Is none of [keys] passed in parameter has been pressed?
     */
    fun isNoneKeysPressed(vararg keys: Key): Boolean = keys.none { isKeyPressed(it) }

    /**
     * Is [signal] touched on the screen?
     *
     * @return null if not touched, coordinates otherwise.
     */
    fun isTouched(signal: TouchSignal): Vector2?

    /**
     * Is [signal] just touched on the screen?
     *
     * @return null if not touched, coordinates of the touch in game screen coordinate.
     */
    fun isJustTouched(signal: TouchSignal): Vector2?

    /**
     * Keys pressed by the user but rendered as text.
     * Useful to capture text from the user.
     */
    fun textJustTyped(): String? = null

    /**
     * Position of the touch when there is no signal.
     *
     * It will be the mouse position on the web platform or the mouse
     * position on the desktop platform.
     *
     * The position can be keep by the consumer as the position vector will be updated.
     * If the cursor is outside of the game area, the position vector will not be updated,
     * it will keep the last position value of the touch position.
     *
     * On mobile devise, as this information is not available, it returns null.
     *
     * The position of the upper left corner is (0, 0) while the
     * bottom right corner will depends of the size of your game screen.
     *
     * Please note that the position will be regarding the game screen and not the device screen.
     * Which mean that even if the window of your game is resized, the coordinate of the bottom right
     * corner will NOT change.
     *
     * @return: position of the touch when idle.
     *          or null if not available on the current platform
     *          or null if outside of the game area.
     */
    fun touchIdlePosition(): Vector2? = null
}