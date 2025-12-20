package com.littlekt.input

import android.view.KeyEvent
import android.view.MotionEvent
import com.littlekt.math.geom.Point

class AndroidInput : Input {
    private val inputCache = InputCache()
    private var touchX = 0f
    private var touchY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private val pointerIdByAssignedIndex: IntArray = IntArray(Pointer.entries.size) { -1 }
    private val touchedPointers = mutableListOf<Pointer>()
    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors
    override val gamepads: Array<GamepadInfo> = Array(4) { GamepadInfo(it) }
    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads
    override val catchKeys: MutableList<Key> = mutableListOf()

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val actionIndex = event.actionIndex
                val androidPointerId = event.getPointerId(actionIndex)
                val assignedIndex = assignPointerIndex(androidPointerId)
                if (assignedIndex >= 0) {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)
                    touchX = x
                    touchY = y
                    inputCache.onTouchDown(x, y, Pointer[assignedIndex])
                    touchedPointers += Pointer[assignedIndex]
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Update deltas based on primary pointer movement
                val primaryIndex = event.actionIndex.takeIf { it >= 0 } ?: 0
                val x = event.getX(primaryIndex)
                val y = event.getY(primaryIndex)
                _deltaX = x - touchX
                _deltaY = y - touchY
                touchX = x
                touchY = y

                // Dispatch move for all active pointers
                for (i in 0 until event.pointerCount) {
                    val androidPointerId = event.getPointerId(i)
                    val assignedIndex = findAssignedIndex(androidPointerId)
                    if (assignedIndex >= 0) {
                        inputCache.onMove(event.getX(i), event.getY(i), Pointer[assignedIndex])
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val actionIndex = event.actionIndex.takeIf { it >= 0 } ?: 0
                val androidPointerId = event.getPointerId(actionIndex)
                val assignedIndex = findAssignedIndex(androidPointerId)
                if (assignedIndex >= 0) {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)
                    touchX = x
                    touchY = y
                    inputCache.onTouchUp(x, y, Pointer[assignedIndex])
                    touchedPointers -= Pointer[assignedIndex]
                    releasePointerIndex(assignedIndex)
                }
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    // release any remaining pointers
                    for (i in pointerIdByAssignedIndex.indices) {
                        if (pointerIdByAssignedIndex[i] != -1) {
                            releasePointerIndex(i)
                        }
                    }
                }
            }
        }
        return true
    }

    fun onKeyDown(event: KeyEvent): Boolean {
        val mappedKey = event.toMappedKey()
        inputCache.onKeyDown(mappedKey)
        val unicode = event.unicodeChar
        if (unicode > 0) inputCache.onCharTyped(unicode.toChar())
        return true
    }

    fun onKeyUp(event: KeyEvent): Boolean {
        val mappedKey = event.toMappedKey()
        inputCache.onKeyUp(mappedKey)
        return true
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val amountX = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val amountY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            inputCache.onScroll(-amountX, -amountY)
            return true
        }
        return false
    }

    fun update() {
        // No gamepad polling implemented yet for Android
        inputCache.processEvents(inputProcessors)
    }

    fun reset() {
        inputCache.reset()
        _deltaX = 0f
        _deltaY = 0f
    }

    override val x: Int
        get() = touchX.toInt()

    override val y: Int
        get() = touchY.toInt()

    override val deltaX: Int
        get() = _deltaX.toInt()

    override val deltaY: Int
        get() = _deltaY.toInt()

    override val isTouching: Boolean
        get() = inputCache.isTouching

    override val justTouched: Boolean
        get() = inputCache.justTouched

    override val pressure: Float
        get() = getPressure(Pointer.POINTER1)

    override val currentEventTime: Long
        get() = inputCache.currentEventTime

    override val axisLeftX: Float
        get() = getGamepadJoystickXDistance(GameStick.LEFT)

    override val axisLeftY: Float
        get() = getGamepadJoystickYDistance(GameStick.LEFT)

    override val axisRightX: Float
        get() = getGamepadJoystickXDistance(GameStick.RIGHT)

    override val axisRightY: Float
        get() = getGamepadJoystickYDistance(GameStick.RIGHT)

    override val cursorLocked: Boolean
        get() = false

    override fun getX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) x else 0
    }

    override fun getY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) y else 0
    }

    override fun getDeltaX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaX else 0
    }

    override fun getDeltaY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaY else 0
    }

    override fun isJustTouched(pointer: Pointer): Boolean {
        return inputCache.isJustTouched(pointer)
    }

    override fun isTouching(pointer: Pointer): Boolean {
        return inputCache.isTouching(pointer)
    }

    override fun isTouching(totalPointers: Int): Boolean {
        return inputCache.isTouching(totalPointers)
    }

    override fun isTouchJustReleased(pointer: Pointer): Boolean {
        return inputCache.isTouchJustReleased(pointer)
    }

    override fun getPressure(pointer: Pointer): Float {
        return if (isJustTouched(pointer)) 1f else 0f
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

    override fun setCursorPosition(x: Int, y: Int) {
        // no-op on Android
    }

    override fun lockCursor() {
        // no-op on Android
    }

    override fun releaseCursor() {
        // no-op on Android
    }

    override fun addInputProcessor(processor: InputProcessor) {
        _inputProcessors += processor
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        _inputProcessors -= processor
    }

    override fun showSoftKeyboard() = Unit

    override fun hideSoftKeyboard() = Unit

    private fun assignPointerIndex(androidPointerId: Int): Int {
        val existing = findAssignedIndex(androidPointerId)
        if (existing >= 0) return existing
        for (i in pointerIdByAssignedIndex.indices) {
            if (pointerIdByAssignedIndex[i] == -1) {
                pointerIdByAssignedIndex[i] = androidPointerId
                return i
            }
        }
        return -1
    }

    private fun findAssignedIndex(androidPointerId: Int): Int {
        for (i in pointerIdByAssignedIndex.indices) {
            if (pointerIdByAssignedIndex[i] == androidPointerId) return i
        }
        return -1
    }

    private fun releasePointerIndex(assignedIndex: Int) {
        if (assignedIndex in pointerIdByAssignedIndex.indices) {
            pointerIdByAssignedIndex[assignedIndex] = -1
        }
    }
}

private fun KeyEvent.toMappedKey(): Key {
    return when (this.keyCode) {
        KeyEvent.KEYCODE_DEL -> Key.BACKSPACE
        KeyEvent.KEYCODE_TAB -> Key.TAB
        KeyEvent.KEYCODE_ENTER -> Key.ENTER
        KeyEvent.KEYCODE_SHIFT_LEFT -> Key.SHIFT_LEFT
        KeyEvent.KEYCODE_SHIFT_RIGHT -> Key.SHIFT_RIGHT
        KeyEvent.KEYCODE_CTRL_LEFT -> Key.CTRL_LEFT
        KeyEvent.KEYCODE_CTRL_RIGHT -> Key.CTRL_RIGHT
        KeyEvent.KEYCODE_ALT_LEFT -> Key.ALT_LEFT
        KeyEvent.KEYCODE_ALT_RIGHT -> Key.ALT_RIGHT
        KeyEvent.KEYCODE_BREAK -> Key.PAUSE_BREAK
        KeyEvent.KEYCODE_CAPS_LOCK -> Key.CAPS_LOCK
        KeyEvent.KEYCODE_ESCAPE -> Key.ESCAPE
        KeyEvent.KEYCODE_PAGE_UP -> Key.PAGE_UP
        KeyEvent.KEYCODE_SPACE -> Key.SPACE
        KeyEvent.KEYCODE_PAGE_DOWN -> Key.PAGE_DOWN
        KeyEvent.KEYCODE_MOVE_END -> Key.END
        KeyEvent.KEYCODE_MOVE_HOME -> Key.HOME
        KeyEvent.KEYCODE_DPAD_LEFT -> Key.ARROW_LEFT
        KeyEvent.KEYCODE_DPAD_UP -> Key.ARROW_UP
        KeyEvent.KEYCODE_DPAD_RIGHT -> Key.ARROW_RIGHT
        KeyEvent.KEYCODE_DPAD_DOWN -> Key.ARROW_DOWN
        KeyEvent.KEYCODE_SYSRQ -> Key.PRINT_SCREEN
        KeyEvent.KEYCODE_INSERT -> Key.INSERT
        KeyEvent.KEYCODE_FORWARD_DEL -> Key.DELETE
        KeyEvent.KEYCODE_0 -> Key.NUM0
        KeyEvent.KEYCODE_1 -> Key.NUM1
        KeyEvent.KEYCODE_2 -> Key.NUM2
        KeyEvent.KEYCODE_3 -> Key.NUM3
        KeyEvent.KEYCODE_4 -> Key.NUM4
        KeyEvent.KEYCODE_5 -> Key.NUM5
        KeyEvent.KEYCODE_6 -> Key.NUM6
        KeyEvent.KEYCODE_7 -> Key.NUM7
        KeyEvent.KEYCODE_8 -> Key.NUM8
        KeyEvent.KEYCODE_9 -> Key.NUM9
        KeyEvent.KEYCODE_A -> Key.A
        KeyEvent.KEYCODE_B -> Key.B
        KeyEvent.KEYCODE_C -> Key.C
        KeyEvent.KEYCODE_D -> Key.D
        KeyEvent.KEYCODE_E -> Key.E
        KeyEvent.KEYCODE_F -> Key.F
        KeyEvent.KEYCODE_G -> Key.G
        KeyEvent.KEYCODE_H -> Key.H
        KeyEvent.KEYCODE_I -> Key.I
        KeyEvent.KEYCODE_J -> Key.J
        KeyEvent.KEYCODE_K -> Key.K
        KeyEvent.KEYCODE_L -> Key.L
        KeyEvent.KEYCODE_M -> Key.M
        KeyEvent.KEYCODE_N -> Key.N
        KeyEvent.KEYCODE_O -> Key.O
        KeyEvent.KEYCODE_P -> Key.P
        KeyEvent.KEYCODE_Q -> Key.Q
        KeyEvent.KEYCODE_R -> Key.R
        KeyEvent.KEYCODE_S -> Key.S
        KeyEvent.KEYCODE_T -> Key.T
        KeyEvent.KEYCODE_U -> Key.U
        KeyEvent.KEYCODE_V -> Key.V
        KeyEvent.KEYCODE_W -> Key.W
        KeyEvent.KEYCODE_X -> Key.X
        KeyEvent.KEYCODE_Y -> Key.Y
        KeyEvent.KEYCODE_Z -> Key.Z
        KeyEvent.KEYCODE_META_LEFT -> Key.LEFT_OS
        KeyEvent.KEYCODE_META_RIGHT -> Key.RIGHT_OS
        KeyEvent.KEYCODE_NUMPAD_0 -> Key.NUMPAD0
        KeyEvent.KEYCODE_NUMPAD_1 -> Key.NUMPAD1
        KeyEvent.KEYCODE_NUMPAD_2 -> Key.NUMPAD2
        KeyEvent.KEYCODE_NUMPAD_3 -> Key.NUMPAD3
        KeyEvent.KEYCODE_NUMPAD_4 -> Key.NUMPAD4
        KeyEvent.KEYCODE_NUMPAD_5 -> Key.NUMPAD5
        KeyEvent.KEYCODE_NUMPAD_6 -> Key.NUMPAD6
        KeyEvent.KEYCODE_NUMPAD_7 -> Key.NUMPAD7
        KeyEvent.KEYCODE_NUMPAD_8 -> Key.NUMPAD8
        KeyEvent.KEYCODE_NUMPAD_9 -> Key.NUMPAD9
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> Key.MULTIPLY
        KeyEvent.KEYCODE_NUMPAD_ADD -> Key.ADD
        KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> Key.SUBTRACT
        KeyEvent.KEYCODE_NUMPAD_DOT -> Key.DECIMAL_POINT
        KeyEvent.KEYCODE_NUMPAD_DIVIDE -> Key.DIVIDE
        KeyEvent.KEYCODE_F1 -> Key.F1
        KeyEvent.KEYCODE_F2 -> Key.F2
        KeyEvent.KEYCODE_F3 -> Key.F3
        KeyEvent.KEYCODE_F4 -> Key.F4
        KeyEvent.KEYCODE_F5 -> Key.F5
        KeyEvent.KEYCODE_F6 -> Key.F6
        KeyEvent.KEYCODE_F7 -> Key.F7
        KeyEvent.KEYCODE_F8 -> Key.F8
        KeyEvent.KEYCODE_F9 -> Key.F9
        KeyEvent.KEYCODE_F10 -> Key.F10
        KeyEvent.KEYCODE_F11 -> Key.F11
        KeyEvent.KEYCODE_F12 -> Key.F12
        KeyEvent.KEYCODE_NUM_LOCK -> Key.NUM_LOCK
        KeyEvent.KEYCODE_SCROLL_LOCK -> Key.SCROLL_LOCK
        KeyEvent.KEYCODE_SEMICOLON -> Key.SEMI_COLON
        KeyEvent.KEYCODE_EQUALS -> Key.EQUAL_SIGN
        KeyEvent.KEYCODE_COMMA -> Key.COMMA
        KeyEvent.KEYCODE_MINUS -> Key.DASH
        KeyEvent.KEYCODE_PERIOD -> Key.PERIOD
        KeyEvent.KEYCODE_SLASH -> Key.FORWARD_SLASH
        KeyEvent.KEYCODE_LEFT_BRACKET -> Key.BRACKET_LEFT
        KeyEvent.KEYCODE_BACKSLASH -> Key.BACK_SLASH
        KeyEvent.KEYCODE_RIGHT_BRACKET -> Key.BRACKET_RIGHT
        KeyEvent.KEYCODE_APOSTROPHE -> Key.SINGLE_QUOTE
        else -> Key.ANY_KEY
    }
}