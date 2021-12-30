package com.lehaine.littlekt.input

import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.geom.Point
import com.lehaine.littlekt.util.fastForEachWithIndex
import org.lwjgl.glfw.GLFW.*

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class LwjglInput(private val logger: Logger) : Input {

    private val inputManager = InputManager()

    private var mouseX = 0f
    private var mouseY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private var lastChar: Char = 0.toChar()

    override var inputProcessor: InputProcessor? = null
    override val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads

    fun attachToWindow(windowHandle: Long) {
        glfwSetKeyCallback(windowHandle) { window, key, scancode, action, mods ->
            when (action) {
                GLFW_PRESS -> inputManager.onKeyDown(key.getKey)
                GLFW_RELEASE -> inputManager.onKeyUp(key.getKey)
                GLFW_REPEAT -> {
                    if (lastChar != 0.toChar()) {
                        inputManager.onKeyType(lastChar)
                    }
                }
            }
        }

        glfwSetCharCallback(windowHandle) { window, codepoint ->
            if (codepoint and 0xff00 == 0xf700) return@glfwSetCharCallback
            lastChar = codepoint.toChar()
            inputManager.onKeyType(lastChar)
        }

        glfwSetScrollCallback(windowHandle) { window, xoffset, yoffset ->
            inputManager.onScroll(-xoffset.toFloat(), -yoffset.toFloat())
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

            inputManager.onMove(mouseX, mouseY, Pointer.POINTER1)
        }

        glfwSetMouseButtonCallback(windowHandle) { window, button, action, mods ->
            if (action == GLFW_PRESS) {
                inputManager.onTouchDown(mouseX, mouseY, button.getPointer)
            } else {
                inputManager.onTouchUp(mouseX, mouseY, button.getPointer)
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
        inputManager.processEvents(inputProcessor)
    }

    fun reset() {
        inputManager.reset()
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

    private fun updateGamepads() {
        if (connectedGamepads.isEmpty()) return

        gamepads.fastForEachWithIndex { index, gamepad ->
            if (gamepad.connected) {
                val axes = glfwGetJoystickAxes(index) ?: run {
                    logger.warn { "Gamepad $index is unable to retrieve axes states! Considering this gamepad as disconnected." }
                    gamepad.connected = false
                    return@fastForEachWithIndex
                }
                val buttons = glfwGetJoystickButtons(index) ?: run {
                    logger.warn { "Gamepad $index is unable to retrieve button states! Considering this gamepad as disconnected." }
                    gamepad.connected = false
                    return@fastForEachWithIndex
                }
                gamepad.rawButtonsPressed[GameButton.L2.index] = axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER]
                gamepad.rawButtonsPressed[GameButton.R2.index] = axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER]
                while (buttons.hasRemaining()) {
                    gamepad.rawButtonsPressed[buttons.position()] = buttons.get().toFloat()
                }
                gamepad.rawAxes[0] = axes[GLFW_GAMEPAD_AXIS_LEFT_X]
                gamepad.rawAxes[1] = axes[GLFW_GAMEPAD_AXIS_LEFT_Y]
                gamepad.rawAxes[2] = axes[GLFW_GAMEPAD_AXIS_RIGHT_X]
                gamepad.rawAxes[3] = axes[GLFW_GAMEPAD_AXIS_RIGHT_Y]
            }
        }
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
        get() = inputManager.isTouching
    override val justTouched: Boolean
        get() = inputManager.justTouched
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
        return inputManager.isTouching(pointer)
    }

    override fun getPressure(pointer: Pointer): Float {
        return if (isTouched(pointer)) 1f else 0f
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        return inputManager.isKeyJustPressed(key)
    }

    override fun isKeyPressed(key: Key): Boolean {
        return inputManager.isKeyPressed(key)
    }

    override fun isKeyJustReleased(key: Key): Boolean {
        return inputManager.isKeyJustReleased(key)
    }

    override fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean {
        return inputManager.isGamepadButtonJustPressed(button, gamepad)
    }

    override fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean {
        return inputManager.isGamepadButtonPressed(button, gamepad)
    }

    override fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        return inputManager.isGamepadButtonJustReleased(button, gamepad)
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

}