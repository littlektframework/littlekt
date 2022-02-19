package com.lehaine.littlekt.input.internal

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
internal enum class InternalInputEventType {
    KEY_DOWN,
    KEY_UP,
    KEY_REPEAT,
    CHAR_TYPED,
    TOUCH_DOWN,
    TOUCH_UP,
    TOUCH_DRAGGED,
    MOUSE_MOVED,
    SCROLLED,
    GAMEPAD_BUTTON_DOWN,
    GAMEPAD_BUTTON_UP,
    GAMEPAD_JOYSTICK_MOVED,
    GAMEPAD_TRIGGER_CHANGED
}