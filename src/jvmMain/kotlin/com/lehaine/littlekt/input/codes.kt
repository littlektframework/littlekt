package com.lehaine.littlekt.input

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_KEY_LAST

/**
 * @author Colton Daily
 * @date 11/7/2021
 */

internal const val UNKNOWN_KEY = GLFW_KEY_LAST + 1

internal val Key.keyCode: Int
    get() {
        return when (this) {
            Key.ANY_KEY -> UNKNOWN_KEY
            Key.BACKSPACE -> GLFW.GLFW_KEY_BACKSPACE
            Key.TAB -> GLFW.GLFW_KEY_TAB
            Key.ENTER -> GLFW.GLFW_KEY_ENTER
            Key.SHIFT_LEFT -> GLFW.GLFW_KEY_LEFT_SHIFT
            Key.CTRL_LEFT -> GLFW.GLFW_KEY_LEFT_CONTROL
            Key.ALT_LEFT -> GLFW.GLFW_KEY_LEFT_ALT
            Key.SHIFT_RIGHT -> GLFW.GLFW_KEY_RIGHT_SHIFT
            Key.CTRL_RIGHT -> GLFW.GLFW_KEY_RIGHT_CONTROL
            Key.ALT_RIGHT -> GLFW.GLFW_KEY_RIGHT_ALT
            Key.PAUSE_BREAK -> GLFW.GLFW_KEY_PAUSE
            Key.CAPS_LOCK -> GLFW.GLFW_KEY_CAPS_LOCK
            Key.ESCAPE -> GLFW.GLFW_KEY_ESCAPE
            Key.PAGE_UP -> GLFW.GLFW_KEY_PAGE_UP
            Key.SPACE -> GLFW.GLFW_KEY_SPACE
            Key.PAGE_DOWN -> GLFW.GLFW_KEY_PAGE_DOWN
            Key.END -> GLFW.GLFW_KEY_END
            Key.HOME -> GLFW.GLFW_KEY_HOME
            Key.ARROW_LEFT -> GLFW.GLFW_KEY_LEFT
            Key.ARROW_UP -> GLFW.GLFW_KEY_UP
            Key.ARROW_RIGHT -> GLFW.GLFW_KEY_RIGHT
            Key.ARROW_DOWN -> GLFW.GLFW_KEY_DOWN
            Key.PRINT_SCREEN -> GLFW.GLFW_KEY_PRINT_SCREEN
            Key.INSERT -> GLFW.GLFW_KEY_INSERT
            Key.DELETE -> GLFW.GLFW_KEY_DELETE
            Key.NUM0 -> GLFW.GLFW_KEY_0
            Key.NUM1 -> GLFW.GLFW_KEY_1
            Key.NUM2 -> GLFW.GLFW_KEY_2
            Key.NUM3 -> GLFW.GLFW_KEY_3
            Key.NUM4 -> GLFW.GLFW_KEY_4
            Key.NUM5 -> GLFW.GLFW_KEY_5
            Key.NUM6 -> GLFW.GLFW_KEY_6
            Key.NUM7 -> GLFW.GLFW_KEY_7
            Key.NUM8 -> GLFW.GLFW_KEY_8
            Key.NUM9 -> GLFW.GLFW_KEY_9
            Key.A -> GLFW.GLFW_KEY_A
            Key.B -> GLFW.GLFW_KEY_B
            Key.C -> GLFW.GLFW_KEY_C
            Key.D -> GLFW.GLFW_KEY_D
            Key.E -> GLFW.GLFW_KEY_E
            Key.F -> GLFW.GLFW_KEY_F
            Key.G -> GLFW.GLFW_KEY_G
            Key.H -> GLFW.GLFW_KEY_H
            Key.I -> GLFW.GLFW_KEY_I
            Key.J -> GLFW.GLFW_KEY_J
            Key.K -> GLFW.GLFW_KEY_K
            Key.L -> GLFW.GLFW_KEY_L
            Key.M -> GLFW.GLFW_KEY_M
            Key.N -> GLFW.GLFW_KEY_N
            Key.O -> GLFW.GLFW_KEY_O
            Key.P -> GLFW.GLFW_KEY_P
            Key.Q -> GLFW.GLFW_KEY_Q
            Key.R -> GLFW.GLFW_KEY_R
            Key.S -> GLFW.GLFW_KEY_S
            Key.T -> GLFW.GLFW_KEY_T
            Key.U -> GLFW.GLFW_KEY_U
            Key.V -> GLFW.GLFW_KEY_V
            Key.W -> GLFW.GLFW_KEY_W
            Key.X -> GLFW.GLFW_KEY_X
            Key.Y -> GLFW.GLFW_KEY_Y
            Key.Z -> GLFW.GLFW_KEY_Z
            Key.LEFT_OS -> GLFW.GLFW_KEY_WORLD_1
            Key.RIGHT_OS -> GLFW.GLFW_KEY_WORLD_2
            Key.NUMPAD0 -> GLFW.GLFW_KEY_KP_0
            Key.NUMPAD1 -> GLFW.GLFW_KEY_KP_1
            Key.NUMPAD2 -> GLFW.GLFW_KEY_KP_2
            Key.NUMPAD3 -> GLFW.GLFW_KEY_KP_3
            Key.NUMPAD4 -> GLFW.GLFW_KEY_KP_4
            Key.NUMPAD5 -> GLFW.GLFW_KEY_KP_5
            Key.NUMPAD6 -> GLFW.GLFW_KEY_KP_6
            Key.NUMPAD7 -> GLFW.GLFW_KEY_KP_7
            Key.NUMPAD8 -> GLFW.GLFW_KEY_KP_8
            Key.NUMPAD9 -> GLFW.GLFW_KEY_KP_9
            Key.MULTIPLY -> GLFW.GLFW_KEY_KP_MULTIPLY
            Key.ADD -> GLFW.GLFW_KEY_KP_ADD
            Key.SUBTRACT -> GLFW.GLFW_KEY_KP_SUBTRACT
            Key.DECIMAL_POINT -> GLFW.GLFW_KEY_KP_DECIMAL
            Key.DIVIDE -> GLFW.GLFW_KEY_KP_DIVIDE
            Key.F1 -> GLFW.GLFW_KEY_F1
            Key.F2 -> GLFW.GLFW_KEY_F2
            Key.F3 -> GLFW.GLFW_KEY_F3
            Key.F4 -> GLFW.GLFW_KEY_F4
            Key.F5 -> GLFW.GLFW_KEY_F5
            Key.F6 -> GLFW.GLFW_KEY_F6
            Key.F7 -> GLFW.GLFW_KEY_F7
            Key.F8 -> GLFW.GLFW_KEY_F8
            Key.F9 -> GLFW.GLFW_KEY_F9
            Key.F10 -> GLFW.GLFW_KEY_F10
            Key.F11 -> GLFW.GLFW_KEY_F11
            Key.F12 -> GLFW.GLFW_KEY_F12
            Key.NUM_LOCK -> GLFW.GLFW_KEY_NUM_LOCK
            Key.SCROLL_LOCK -> GLFW.GLFW_KEY_SCROLL_LOCK
            Key.SEMI_COLON -> GLFW.GLFW_KEY_SEMICOLON
            Key.EQUAL_SIGN -> GLFW.GLFW_KEY_EQUAL
            Key.COMMA -> GLFW.GLFW_KEY_COMMA
            Key.DASH -> GLFW.GLFW_KEY_MINUS
            Key.PERIOD -> GLFW.GLFW_KEY_PERIOD
            Key.FORWARD_SLASH -> GLFW.GLFW_KEY_SLASH
            Key.BRACKET_LEFT -> GLFW.GLFW_KEY_LEFT_BRACKET
            Key.BACK_SLASH -> GLFW.GLFW_KEY_BACKSLASH
            Key.BRACKET_RIGHT -> GLFW.GLFW_KEY_RIGHT_BRACKET
            Key.SINGLE_QUOTE -> GLFW.GLFW_KEY_APOSTROPHE
        }
    }


internal val Int.getKey: Key
    get() {
        return when (this) {
            UNKNOWN_KEY -> Key.ANY_KEY
            GLFW.GLFW_KEY_BACKSPACE -> Key.BACKSPACE
            GLFW.GLFW_KEY_TAB -> Key.TAB
            GLFW.GLFW_KEY_ENTER -> Key.ENTER
            GLFW.GLFW_KEY_LEFT_SHIFT -> Key.SHIFT_LEFT
            GLFW.GLFW_KEY_LEFT_CONTROL -> Key.CTRL_LEFT
            GLFW.GLFW_KEY_LEFT_ALT -> Key.ALT_LEFT
            GLFW.GLFW_KEY_RIGHT_SHIFT -> Key.SHIFT_RIGHT
            GLFW.GLFW_KEY_RIGHT_CONTROL -> Key.CTRL_RIGHT
            GLFW.GLFW_KEY_RIGHT_ALT -> Key.ALT_RIGHT
            GLFW.GLFW_KEY_PAUSE -> Key.PAUSE_BREAK
            GLFW.GLFW_KEY_CAPS_LOCK -> Key.CAPS_LOCK
            GLFW.GLFW_KEY_ESCAPE -> Key.ESCAPE
            GLFW.GLFW_KEY_PAGE_UP -> Key.PAGE_UP
            GLFW.GLFW_KEY_SPACE -> Key.SPACE
            GLFW.GLFW_KEY_PAGE_DOWN -> Key.PAGE_DOWN
            GLFW.GLFW_KEY_END -> Key.END
            GLFW.GLFW_KEY_HOME -> Key.HOME
            GLFW.GLFW_KEY_LEFT -> Key.ARROW_LEFT
            GLFW.GLFW_KEY_UP -> Key.ARROW_UP
            GLFW.GLFW_KEY_RIGHT -> Key.ARROW_RIGHT
            GLFW.GLFW_KEY_DOWN -> Key.ARROW_DOWN
            GLFW.GLFW_KEY_PRINT_SCREEN -> Key.PRINT_SCREEN
            GLFW.GLFW_KEY_INSERT -> Key.INSERT
            GLFW.GLFW_KEY_DELETE -> Key.DELETE
            GLFW.GLFW_KEY_0 -> Key.NUM0
            GLFW.GLFW_KEY_1 -> Key.NUM1
            GLFW.GLFW_KEY_2 -> Key.NUM2
            GLFW.GLFW_KEY_3 -> Key.NUM3
            GLFW.GLFW_KEY_4 -> Key.NUM4
            GLFW.GLFW_KEY_5 -> Key.NUM5
            GLFW.GLFW_KEY_6 -> Key.NUM6
            GLFW.GLFW_KEY_7 -> Key.NUM7
            GLFW.GLFW_KEY_8 -> Key.NUM8
            GLFW.GLFW_KEY_9 -> Key.NUM9
            GLFW.GLFW_KEY_A -> Key.A
            GLFW.GLFW_KEY_B -> Key.B
            GLFW.GLFW_KEY_C -> Key.C
            GLFW.GLFW_KEY_D -> Key.D
            GLFW.GLFW_KEY_E -> Key.E
            GLFW.GLFW_KEY_F -> Key.F
            GLFW.GLFW_KEY_G -> Key.G
            GLFW.GLFW_KEY_H -> Key.H
            GLFW.GLFW_KEY_I -> Key.I
            GLFW.GLFW_KEY_J -> Key.J
            GLFW.GLFW_KEY_K -> Key.K
            GLFW.GLFW_KEY_L -> Key.L
            GLFW.GLFW_KEY_M -> Key.M
            GLFW.GLFW_KEY_N -> Key.N
            GLFW.GLFW_KEY_O -> Key.O
            GLFW.GLFW_KEY_P -> Key.P
            GLFW.GLFW_KEY_Q -> Key.Q
            GLFW.GLFW_KEY_R -> Key.R
            GLFW.GLFW_KEY_S -> Key.S
            GLFW.GLFW_KEY_T -> Key.T
            GLFW.GLFW_KEY_U -> Key.U
            GLFW.GLFW_KEY_V -> Key.V
            GLFW.GLFW_KEY_W -> Key.W
            GLFW.GLFW_KEY_X -> Key.X
            GLFW.GLFW_KEY_Y -> Key.Y
            GLFW.GLFW_KEY_Z -> Key.Z
            GLFW.GLFW_KEY_WORLD_1 -> Key.LEFT_OS
            GLFW.GLFW_KEY_WORLD_2 -> Key.RIGHT_OS
            GLFW.GLFW_KEY_KP_0 -> Key.NUMPAD0
            GLFW.GLFW_KEY_KP_1 -> Key.NUMPAD1
            GLFW.GLFW_KEY_KP_2 -> Key.NUMPAD2
            GLFW.GLFW_KEY_KP_3 -> Key.NUMPAD3
            GLFW.GLFW_KEY_KP_4 -> Key.NUMPAD4
            GLFW.GLFW_KEY_KP_5 -> Key.NUMPAD5
            GLFW.GLFW_KEY_KP_6 -> Key.NUMPAD6
            GLFW.GLFW_KEY_KP_7 -> Key.NUMPAD7
            GLFW.GLFW_KEY_KP_8 -> Key.NUMPAD8
            GLFW.GLFW_KEY_KP_9 -> Key.NUMPAD9
            GLFW.GLFW_KEY_KP_MULTIPLY -> Key.MULTIPLY
            GLFW.GLFW_KEY_KP_ADD -> Key.ADD
            GLFW.GLFW_KEY_KP_SUBTRACT -> Key.SUBTRACT
            GLFW.GLFW_KEY_KP_DECIMAL -> Key.DECIMAL_POINT
            GLFW.GLFW_KEY_KP_DIVIDE -> Key.DIVIDE
            GLFW.GLFW_KEY_F1 -> Key.F1
            GLFW.GLFW_KEY_F2 -> Key.F2
            GLFW.GLFW_KEY_F3 -> Key.F3
            GLFW.GLFW_KEY_F4 -> Key.F4
            GLFW.GLFW_KEY_F5 -> Key.F5
            GLFW.GLFW_KEY_F6 -> Key.F6
            GLFW.GLFW_KEY_F7 -> Key.F7
            GLFW.GLFW_KEY_F8 -> Key.F8
            GLFW.GLFW_KEY_F9 -> Key.F9
            GLFW.GLFW_KEY_F10 -> Key.F10
            GLFW.GLFW_KEY_F11 -> Key.F11
            GLFW.GLFW_KEY_F12 -> Key.F12
            GLFW.GLFW_KEY_NUM_LOCK -> Key.NUM_LOCK
            GLFW.GLFW_KEY_SCROLL_LOCK -> Key.SCROLL_LOCK
            GLFW.GLFW_KEY_SEMICOLON -> Key.SEMI_COLON
            GLFW.GLFW_KEY_EQUAL -> Key.EQUAL_SIGN
            GLFW.GLFW_KEY_COMMA -> Key.COMMA
            GLFW.GLFW_KEY_MINUS -> Key.DASH
            GLFW.GLFW_KEY_PERIOD -> Key.PERIOD
            GLFW.GLFW_KEY_SLASH -> Key.FORWARD_SLASH
            GLFW.GLFW_KEY_LEFT_BRACKET -> Key.BRACKET_LEFT
            GLFW.GLFW_KEY_BACKSLASH -> Key.BACK_SLASH
            GLFW.GLFW_KEY_RIGHT_BRACKET -> Key.BRACKET_RIGHT
            GLFW.GLFW_KEY_APOSTROPHE -> Key.SINGLE_QUOTE
            else -> Key.ANY_KEY
        }
    }

internal val Int.getPointer: Pointer
    get() = when (this) {
        GLFW.GLFW_MOUSE_BUTTON_1 -> Pointer.POINTER1
        GLFW.GLFW_MOUSE_BUTTON_2 -> Pointer.POINTER2
        GLFW.GLFW_MOUSE_BUTTON_3 -> Pointer.POINTER3
        else -> Pointer.POINTER1
    }