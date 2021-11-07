package com.lehaine.littlekt.input

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Vector2
import com.lehaine.littlekt.util.internal.convert
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWCursorEnterCallback
import org.lwjgl.glfw.GLFWKeyCallback
import java.nio.DoubleBuffer

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class LwjglInput(private val logger: Logger, private val application: Application) : Input, InputManager {

    private val touchManager = TouchManager(GLFW_KEY_LAST)

    private var window: Long = 0

    private val b1: DoubleBuffer = BufferUtils.createDoubleBuffer(1)
    private val b2: DoubleBuffer = BufferUtils.createDoubleBuffer(1)

    private var mousePosition: Vector2 = Vector2(0f, 0f)
    private var isMouseInsideGameScreen: Boolean = false
    private var isMouseInsideWindow: Boolean = false

    private fun keyDown(event: Int) {
        logger.debug("INPUT_HANDLER") { "${Thread.currentThread().name} Key pushed $event" }
        touchManager.onKeyPressed(event)
    }

    private fun keyUp(event: Int) {
        logger.debug("INPUT_HANDLER") { "Key release $event" }
        touchManager.onKeyReleased(event)
    }

    fun attachHandler(windowAddress: Long) {
        window = windowAddress
        glfwSetInputMode(windowAddress, GLFW_STICKY_KEYS, GLFW_TRUE)
        glfwSetKeyCallback(
            windowAddress,
            object : GLFWKeyCallback() {
                override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                    if (action == GLFW_PRESS) {
                        keyDown(key)
                    } else if (action == GLFW_RELEASE) {
                        keyUp(key)
                    }
                }
            }
        )
        glfwSetCursorEnterCallback(
            windowAddress,
            object : GLFWCursorEnterCallback() {
                override fun invoke(window: Long, entered: Boolean) {
                    isMouseInsideWindow = entered
                }
            }
        )
    }

    override fun record() {
        fun touchStatus(glfwMouseButton: Int, touchSignal: TouchSignal) {
            // see https://github.com/LWJGL/lwjgl3-wiki/wiki/2.6.3-Input-handling-with-GLFW
            if (glfwGetMouseButton(window, glfwMouseButton) == GLFW_PRESS) {
                glfwGetCursorPos(window, b1, b2)
                if (touchManager.isTouched(touchSignal) != null) {
                    val gamePosition = application.convert(b1[0].toFloat(), b2[0].toFloat())
                    gamePosition?.let { (x, y) ->
                        touchManager.onTouchMove(touchSignal, x, y)
                    }
                } else {
                    val gamePosition = application.convert(b1[0].toFloat(), b2[0].toFloat())
                    gamePosition?.let { (x, y) ->
                        touchManager.onTouchDown(touchSignal, x, y)
                    }
                }
            } else if (glfwGetMouseButton(window, glfwMouseButton) == GLFW_RELEASE) {
                touchManager.onTouchUp(touchSignal)
            }
        }
        // Update mouse position
        // https://www.glfw.org/docs/3.3/input_guide.html#cursor_pos
        if (isMouseInsideWindow) {
            glfwGetCursorPos(window, b1, b2)
            val gamePosition = application.convert(b1[0].toFloat(), b2[0].toFloat())
            if (gamePosition == null) {
                // the mouse is in the window but NOT in the game screen
                isMouseInsideGameScreen = false
            } else {
                isMouseInsideGameScreen = true
                mousePosition.x = gamePosition.first
                mousePosition.y = gamePosition.second
            }
        } else {
            isMouseInsideGameScreen = false
        }

        // Update touch status
        touchStatus(GLFW_MOUSE_BUTTON_1, TouchSignal.TOUCH1)
        touchStatus(GLFW_MOUSE_BUTTON_2, TouchSignal.TOUCH2)
        touchStatus(GLFW_MOUSE_BUTTON_3, TouchSignal.TOUCH3)
    }

    override fun reset() = touchManager.processReceivedEvent()

    override fun isKeyJustPressed(key: Key): Boolean = if (key == Key.ANY_KEY) {
        touchManager.isAnyKeyJustPressed
    } else {
        touchManager.isKeyJustPressed(key.keyCode)
    }

    override fun isKeyPressed(key: Key): Boolean = if (key == Key.ANY_KEY) {
        touchManager.isAnyKeyPressed
    } else {
        touchManager.isKeyPressed(key.keyCode)
    }

    override fun isTouched(signal: TouchSignal): Vector2? = touchManager.isTouched(signal)

    override fun isJustTouched(signal: TouchSignal): Vector2? = touchManager.isJustTouched(signal)

    override fun touchIdlePosition(): Vector2? {
        return if (isMouseInsideGameScreen) {
            mousePosition
        } else {
            null
        }
    }
}