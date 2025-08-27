package com.littlekt.input

import org.w3c.dom.events.KeyboardEvent

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
internal val Key.keyCode: String
    get() {
        return when (this) {
            Key.ANY_KEY -> "Unknown"
            Key.BACKSPACE -> "Backspace"
            Key.TAB -> "Tab"
            Key.ENTER -> "Enter"
            Key.SHIFT_LEFT -> "ShiftLeft"
            Key.CTRL_LEFT -> "ControlLeft"
            Key.ALT_LEFT -> "AltLeft"
            Key.SHIFT_RIGHT -> "ShiftRight"
            Key.CTRL_RIGHT -> "CtrlRight"
            Key.ALT_RIGHT -> "AltRight"
            Key.PAUSE_BREAK -> "Pause"
            Key.CAPS_LOCK -> "CapsLock"
            Key.ESCAPE -> "Escape"
            Key.PAGE_UP -> "PageUp"
            Key.SPACE -> "Space"
            Key.PAGE_DOWN -> "PageDown"
            Key.END -> "End"
            Key.HOME -> "Home"
            Key.ARROW_LEFT -> "ArrowLeft"
            Key.ARROW_UP -> "ArrowUp"
            Key.ARROW_RIGHT -> "ArrowRight"
            Key.ARROW_DOWN -> "ArrowDown"
            Key.PRINT_SCREEN -> "PrintScreen"
            Key.INSERT -> "Insert"
            Key.DELETE -> "Delete"
            Key.NUM0 -> "Digit0"
            Key.NUM1 -> "Digit1"
            Key.NUM2 -> "Digit2"
            Key.NUM3 -> "Digit3"
            Key.NUM4 -> "Digit4"
            Key.NUM5 -> "Digit5"
            Key.NUM6 -> "Digit6"
            Key.NUM7 -> "Digit7"
            Key.NUM8 -> "Digit8"
            Key.NUM9 -> "Digit9"
            Key.A -> "KeyA"
            Key.B -> "KeyB"
            Key.C -> "KeyC"
            Key.D -> "KeyD"
            Key.E -> "KeyE"
            Key.F -> "KeyF"
            Key.G -> "KeyG"
            Key.H -> "KeyH"
            Key.I -> "KeyI"
            Key.J -> "KeyJ"
            Key.K -> "KeyK"
            Key.L -> "KeyL"
            Key.M -> "KeyM"
            Key.N -> "KeyN"
            Key.O -> "KeyO"
            Key.P -> "KeyP"
            Key.Q -> "KeyQ"
            Key.R -> "KeyR"
            Key.S -> "KeyS"
            Key.T -> "KeyT"
            Key.U -> "KeyU"
            Key.V -> "KeyV"
            Key.W -> "KeyW"
            Key.X -> "KeyX"
            Key.Y -> "KeyY"
            Key.Z -> "KeyZ"
            Key.LEFT_OS -> "OSLeft"
            Key.RIGHT_OS -> "OSRight"
            Key.NUMPAD0 -> "Numpad0"
            Key.NUMPAD1 -> "Numpad1"
            Key.NUMPAD2 -> "Numpad2"
            Key.NUMPAD3 -> "Numpad3"
            Key.NUMPAD4 -> "Numpad4"
            Key.NUMPAD5 -> "Numpad5"
            Key.NUMPAD6 -> "Numpad6"
            Key.NUMPAD7 -> "Numpad7"
            Key.NUMPAD8 -> "Numpad8"
            Key.NUMPAD9 -> "Numpad9"
            Key.MULTIPLY -> "NumpadMultiply"
            Key.ADD -> "NumpadAdd"
            Key.SUBTRACT -> "NumpadSubtract"
            Key.DECIMAL_POINT -> "NumpadDecimal"
            Key.DIVIDE -> "NumpadDivide"
            Key.F1 -> "F1"
            Key.F2 -> "F2"
            Key.F3 -> "F3"
            Key.F4 -> "F4"
            Key.F5 -> "F5"
            Key.F6 -> "F6"
            Key.F7 -> "F7"
            Key.F8 -> "F8"
            Key.F9 -> "F9"
            Key.F10 -> "F10"
            Key.F11 -> "F11"
            Key.F12 -> "F12"
            Key.NUM_LOCK -> "NumLock"
            Key.SCROLL_LOCK -> "ScrollLock"
            Key.SEMI_COLON -> "Semicolon"
            Key.EQUAL_SIGN -> "Equal"
            Key.COMMA -> "Comma"
            Key.DASH -> "Minus"
            Key.PERIOD -> "Period"
            Key.FORWARD_SLASH -> "Slash"
            Key.BRACKET_LEFT -> "BracketLeft"
            Key.BACK_SLASH -> "Backslash"
            Key.BRACKET_RIGHT -> "BracketRight"
            Key.SINGLE_QUOTE -> "Quote"
        }
    }

internal val KeyboardEvent.jsKey: Key
    get() {
        return when (this.code) {
            "Backspace" -> Key.BACKSPACE
            "Tab" -> Key.TAB
            "Enter" -> Key.ENTER
            "ShiftLeft" -> Key.SHIFT_LEFT
            "ControlLeft" -> Key.CTRL_LEFT
            "AltLeft" -> Key.ALT_LEFT
            "ShiftRight" -> Key.SHIFT_RIGHT
            "CtrlRight" -> Key.CTRL_RIGHT
            "AltRight" -> Key.ALT_RIGHT
            "Pause" -> Key.PAUSE_BREAK
            "CapsLock" -> Key.CAPS_LOCK
            "Escape" -> Key.ESCAPE
            "PageUp" -> Key.PAGE_UP
            "Space" -> Key.SPACE
            "PageDown" -> Key.PAGE_DOWN
            "End" -> Key.END
            "Home" -> Key.HOME
            "ArrowLeft" -> Key.ARROW_LEFT
            "ArrowUp" -> Key.ARROW_UP
            "ArrowRight" -> Key.ARROW_RIGHT
            "ArrowDown" -> Key.ARROW_DOWN
            "PrintScreen" -> Key.PRINT_SCREEN
            "Insert" -> Key.INSERT
            "Delete" -> Key.DELETE
            "Digit0" -> Key.NUM0
            "Digit1" -> Key.NUM1
            "Digit2" -> Key.NUM2
            "Digit3" -> Key.NUM3
            "Digit4" -> Key.NUM4
            "Digit5" -> Key.NUM5
            "Digit6" -> Key.NUM6
            "Digit7" -> Key.NUM7
            "Digit8" -> Key.NUM8
            "Digit9" -> Key.NUM9
            "KeyA" -> Key.A
            "KeyB" -> Key.B
            "KeyC" -> Key.C
            "KeyD" -> Key.D
            "KeyE" -> Key.E
            "KeyF" -> Key.F
            "KeyG" -> Key.G
            "KeyH" -> Key.H
            "KeyI" -> Key.I
            "KeyJ" -> Key.J
            "KeyK" -> Key.K
            "KeyL" -> Key.L
            "KeyM" -> Key.M
            "KeyN" -> Key.N
            "KeyO" -> Key.O
            "KeyP" -> Key.P
            "KeyQ" -> Key.Q
            "KeyR" -> Key.R
            "KeyS" -> Key.S
            "KeyT" -> Key.T
            "KeyU" -> Key.U
            "KeyV" -> Key.V
            "KeyW" -> Key.W
            "KeyX" -> Key.X
            "KeyY" -> Key.Y
            "KeyZ" -> Key.Z
            "OSLeft" -> Key.LEFT_OS
            "OSRight" -> Key.RIGHT_OS
            "Numpad0" -> Key.NUMPAD0
            "Numpad1" -> Key.NUMPAD1
            "Numpad2" -> Key.NUMPAD2
            "Numpad3" -> Key.NUMPAD3
            "Numpad4" -> Key.NUMPAD4
            "Numpad5" -> Key.NUMPAD5
            "Numpad6" -> Key.NUMPAD6
            "Numpad7" -> Key.NUMPAD7
            "Numpad8" -> Key.NUMPAD8
            "Numpad9" -> Key.NUMPAD9
            "NumpadMultiply" -> Key.MULTIPLY
            "NumpadAdd" -> Key.ADD
            "NumpadSubtract" -> Key.SUBTRACT
            "NumpadDecimal" -> Key.DECIMAL_POINT
            "NumpadDivide" -> Key.DIVIDE
            "F1" -> Key.F1
            "F2" -> Key.F2
            "F3" -> Key.F3
            "F4" -> Key.F4
            "F5" -> Key.F5
            "F6" -> Key.F6
            "F7" -> Key.F7
            "F8" -> Key.F8
            "F9" -> Key.F9
            "F10" -> Key.F10
            "F11" -> Key.F11
            "F12" -> Key.F12
            "NumLock" -> Key.NUM_LOCK
            "ScrollLock" -> Key.SCROLL_LOCK
            "Semicolon" -> Key.SEMI_COLON
            "Equal" -> Key.EQUAL_SIGN
            "Comma" -> Key.COMMA
            "Minus" -> Key.DASH
            "Period" -> Key.PERIOD
            "Slash" -> Key.FORWARD_SLASH
            "BracketLeft" -> Key.BRACKET_LEFT
            "Backslash" -> Key.BACK_SLASH
            "BracketRight" -> Key.BRACKET_RIGHT
            "Quote" -> Key.SINGLE_QUOTE
            else -> Key.ANY_KEY
        }
    }

internal val Int.getPointer: Pointer
    get() =
        when (this) {
            0 -> Pointer.POINTER1
            1 -> Pointer.POINTER2
            2 -> Pointer.POINTER3
            else -> Pointer.POINTER1
        }

internal val Short.getPointer: Pointer
    get() = this.toInt().getPointer