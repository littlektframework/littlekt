package com.lehaine.littlekt.input

import com.lehaine.littlekt.math.geom.Point
import com.lehaine.littlekt.util.fastForEachWithIndex
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import java.nio.ByteBuffer

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class LwjglInput : Input {

    private val inputCache = InputCache()

    private var mouseX = 0f
    private var mouseY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private var lastChar: Char = 0.toChar()

    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors

    override val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads

    fun attachToWindow(windowHandle: Long) {
        glfwSetKeyCallback(windowHandle) { window, key, scancode, action, mods ->
            when (action) {
                GLFW_PRESS -> {
                    lastChar = 0.toChar()
                    inputCache.onKeyDown(key.getKey)
                }
                GLFW_RELEASE -> inputCache.onKeyUp(key.getKey)
                GLFW_REPEAT -> {
                    if (lastChar != 0.toChar()) {
                        inputCache.onCharTyped(lastChar)
                    }
                    inputCache.onKeyRepeat(key.getKey)
                }
            }
        }

        glfwSetCharCallback(windowHandle) { window, codepoint ->
            if (codepoint and 0xff00 == 0xf700) return@glfwSetCharCallback
            lastChar = codepoint.toChar()
            inputCache.onCharTyped(lastChar)
        }

        glfwSetScrollCallback(windowHandle) { window, xoffset, yoffset ->
            inputCache.onScroll(-xoffset.toFloat(), -yoffset.toFloat())
        }

        var logicalMouseY = 0f
        var logicalMouseX = 0f
        glfwSetCursorPosCallback(windowHandle) { window, xpos, ypos ->
            _deltaX = xpos.toFloat() - logicalMouseX
            _deltaY = ypos.toFloat() - logicalMouseY
            mouseX = xpos.toFloat()
            mouseY = ypos.toFloat()
            logicalMouseX = mouseX
            logicalMouseY = mouseY

            // todo handle hdpi mode pixels vs logical


            inputCache.onMove(mouseX, mouseY, Pointer.POINTER1)
        }

        glfwSetMouseButtonCallback(windowHandle) { window, button, action, mods ->
            if (action == GLFW_PRESS) {
                inputCache.onTouchDown(mouseX, mouseY, button.getPointer)
            } else {
                inputCache.onTouchUp(mouseX, mouseY, button.getPointer)
            }
        }

        glfwSetJoystickCallback { jid, event ->
            if (!glfwJoystickPresent(jid)) return@glfwSetJoystickCallback

            if (event == GLFW_CONNECTED) {
                // add joystick
                gamepads[jid].connected = true
                _connectedGamepads += gamepads[jid]

            } else if (event == GLFW_DISCONNECTED) {
                // remove joystick
                gamepads[jid].connected = false
                _connectedGamepads -= gamepads[jid]
            }
        }
        checkForGamepads()
    }

    fun update() {
        updateGamepads()
        inputCache.processEvents(inputProcessors)
    }

    fun reset() {
        inputCache.reset()
        _deltaX = 0f
        _deltaY = 0f
    }

    fun checkForGamepads() {
        for (i in gamepads.indices) {
            if (!glfwJoystickPresent(i)) {
                if (gamepads[i].connected) {
                    gamepads[i].connected = false
                    _connectedGamepads -= gamepads[i]
                }
                continue
            }

            if (!gamepads[i].connected) {
                gamepads[i].connected = true
                _connectedGamepads += gamepads[i]
            }
        }
    }

    val state = GLFWGamepadState(ByteBuffer.allocateDirect(64))
    private fun updateGamepads() {
        if (connectedGamepads.isEmpty()) return

        gamepads.fastForEachWithIndex { index, gamepad ->
            if (gamepad.connected) {
                glfwGetGamepadState(index, state)

                gamepad.mapping.buttonListOrder.forEachIndexed { btnIdx, button ->
                    val glfwIdx = button.glfwIndex
                    if (glfwIdx != -1) {
                        gamepad.rawButtonsPressed[btnIdx] = state.buttons(glfwIdx).toFloat()
                    }
                }
                gamepad.mapping.buttonToIndex[GameButton.L2]?.let {
                    gamepad.rawButtonsPressed[it] = state.axes(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER)
                }
                gamepad.mapping.buttonToIndex[GameButton.R2]?.let {
                    gamepad.rawButtonsPressed[it] = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)
                }
                inputCache.updateGamepadTrigger(
                    GameButton.L2,
                    gamepad
                )
                inputCache.updateGamepadTrigger(
                    GameButton.R2,
                    gamepad
                )
                inputCache.updateGamepadButtons(gamepad)

                gamepad.rawAxes[0] = state.axes(GLFW_GAMEPAD_AXIS_LEFT_X)
                gamepad.rawAxes[1] = state.axes(GLFW_GAMEPAD_AXIS_LEFT_Y)
                gamepad.rawAxes[2] = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_X)
                gamepad.rawAxes[3] = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_Y)
                inputCache.updateGamepadStick(
                    GameStick.LEFT,
                    gamepad
                )
                inputCache.updateGamepadStick(
                    GameStick.RIGHT,
                    gamepad
                )
            }
        }
    }

    private val GameButton.glfwIndex
        get() = when (this) {
            GameButton.XBOX_A -> GLFW_GAMEPAD_BUTTON_A
            GameButton.XBOX_B -> GLFW_GAMEPAD_BUTTON_B
            GameButton.XBOX_X -> GLFW_GAMEPAD_BUTTON_X
            GameButton.XBOX_Y -> GLFW_GAMEPAD_BUTTON_Y
            GameButton.L1 -> GLFW_GAMEPAD_BUTTON_LEFT_BUMPER
            GameButton.R1 -> GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER
            GameButton.SELECT -> GLFW_GAMEPAD_BUTTON_BACK
            GameButton.START -> GLFW_GAMEPAD_BUTTON_START
            GameButton.SYSTEM -> GLFW_GAMEPAD_BUTTON_GUIDE
            GameButton.LEFT_THUMB -> GLFW_GAMEPAD_BUTTON_LEFT_THUMB
            GameButton.RIGHT_THUMB -> GLFW_GAMEPAD_BUTTON_RIGHT_THUMB
            GameButton.UP -> GLFW_GAMEPAD_BUTTON_DPAD_UP
            GameButton.RIGHT -> GLFW_GAMEPAD_BUTTON_DPAD_RIGHT
            GameButton.DOWN -> GLFW_GAMEPAD_BUTTON_DPAD_DOWN
            GameButton.LEFT -> GLFW_GAMEPAD_BUTTON_DPAD_LEFT
            else -> -1
        }

    override val x: Int
        get() = mouseX.toInt()
    override val y: Int
        get() = mouseY.toInt()
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

    override val axisLeftX: Float
        get() = getGamepadJoystickXDistance(GameStick.LEFT)
    override val axisLeftY: Float
        get() = getGamepadJoystickYDistance(GameStick.LEFT)
    override val axisRightX: Float
        get() = getGamepadJoystickXDistance(GameStick.RIGHT)
    override val axisRightY: Float
        get() = getGamepadJoystickYDistance(GameStick.RIGHT)

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

    override fun setCursorPosition(x: Int, y: Int) {
        TODO("Not yet implemented")
    }

    override fun addInputProcessor(processor: InputProcessor) {
        _inputProcessors += processor
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        _inputProcessors -= processor
    }

    override fun showSoftKeyboard() = Unit
    override fun hideSoftKeyboard() = Unit
}