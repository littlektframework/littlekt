package com.lehaine.littlekt.input

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

interface Input {

    val x: Int
    val y: Int
    val deltaX: Int
    val deltaY: Int

    val isTouching: Boolean
    val justTouched: Boolean

    val pressure: Float

    var inputProcessor: InputProcessor?

    fun getX(pointer: Pointer): Int
    fun getY(pointer: Pointer): Int

    fun getDeltaX(pointer: Pointer): Int
    fun getDeltaY(pointer: Pointer): Int

    fun isTouched(pointer: Pointer): Boolean

    fun getPressure(pointer: Pointer): Float

    fun isKeyJustPressed(key: Key): Boolean
    fun isKeyPressed(key: Key): Boolean
    fun areAnyKeysPressed(vararg keys: Key): Boolean = keys.any { isKeyPressed(it) }
    fun areAllKeysPressed(vararg keys: Key): Boolean = keys.all { isKeyPressed(it) }
    fun areNoKeysPressed(vararg keys: Key): Boolean = keys.none { isKeyPressed(it) }

    fun setCursorPosition(x: Int, y: Int)
}