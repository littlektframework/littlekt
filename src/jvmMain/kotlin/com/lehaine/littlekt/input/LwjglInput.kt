package com.lehaine.littlekt.input

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.log.Logger
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.*

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class LwjglInput(private val logger: Logger, private val application: Application) : Input {

    private val inputManager = InputManager()

    private var mouseX = 0f
    private var mouseY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private var lastChar: Char = 0.toChar()

    var inputProcessor: InputProcessor? = null

    fun attachHandler(windowHandle: Long) {
        glfwSetKeyCallback(
            windowHandle,
            object : GLFWKeyCallback() {
                override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
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
            }
        )
        glfwSetCharCallback(windowHandle, object : GLFWCharCallback() {
            override fun invoke(window: Long, codepoint: Int) {
                if (codepoint and 0xff00 == 0xf700) return
                lastChar = codepoint.toChar()
                inputManager.onKeyType(lastChar)
            }

        })

        glfwSetScrollCallback(windowHandle, object : GLFWScrollCallback() {
            override fun invoke(window: Long, xoffset: Double, yoffset: Double) {
                inputManager.onScroll(-xoffset.toFloat(), -yoffset.toFloat())
            }
        })
        glfwSetCursorPosCallback(windowHandle, object : GLFWCursorPosCallback() {
            private var logicalMouseY = 0f
            private var logicalMouseX = 0f

            override fun invoke(window: Long, xpos: Double, ypos: Double) {
                _deltaX = xpos.toFloat() - logicalMouseX
                _deltaY = ypos.toFloat() - logicalMouseY
                mouseX = xpos.toFloat()
                mouseY = ypos.toFloat()
                logicalMouseX = mouseX
                logicalMouseY = mouseY

                // todo handle hdpi mode pixels vs logical

                inputManager.onMove(mouseX, mouseY, Pointer.POINTER1)
            }

        })

        glfwSetMouseButtonCallback(windowHandle, object : GLFWMouseButtonCallback() {
            override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
                if (action == GLFW_PRESS) {
                    inputManager.onTouchDown(mouseX, mouseY, button.getPointer)
                } else {
                    inputManager.onTouchUp(mouseX, mouseY, button.getPointer)
                }
            }

        })
    }

    fun update() {
        inputManager.processEvents(inputProcessor)
    }

    fun reset() {
        inputManager.reset()
        _deltaX = 0f
        _deltaY = 0f
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

    override fun setCursorPosition(x: Int, y: Int) {
        TODO("Not yet implemented")
    }

}