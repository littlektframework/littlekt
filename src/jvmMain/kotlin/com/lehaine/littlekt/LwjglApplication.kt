package com.lehaine.littlekt

import com.lehaine.littlekt.input.LwjglInput
import com.lehaine.littlekt.io.AssetManager
import com.lehaine.littlekt.log.JvmLogger
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.milliseconds
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL30C.GL_TRUE
import org.lwjgl.opengl.GL30C.glClearColor
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
class LwjglApplication(
    val title: String = "LittleKt Application",
    val width: Int = 800,
    val height: Int = 480,
    val vSync: Boolean = true
) :
    Application {

    private var windowHandle: Long = 0

    private val windowShouldClose: Boolean
        get() = glfwWindowShouldClose(windowHandle)

    private var lastFrame: Long = getTime()

    override val graphics = LwjglGraphics()

    override val logger: Logger = JvmLogger(title)

    override val input = LwjglInput(logger, this)

    override val assetManager: AssetManager
        get() = TODO("Not yet implemented")

    private fun getDelta(): Float {
        val time = getTime()
        val delta = (time - lastFrame)
        lastFrame = time
        return min(delta / 1000f, 1 / 60f)
    }

    override fun start(game: LittleKt) {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        // Create the window
        windowHandle = glfwCreateWindow(800, 480, title, NULL, NULL)
        if (windowHandle == NULL) throw RuntimeException("Failed to create the GLFW window")

        input.attachHandler(windowHandle)

        glfwSetFramebufferSizeCallback(windowHandle) { _, width, height ->

        }

        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowHandle, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            check(vidmode != null) { "Unable to retrieve GLFW video mode" }

            // Center the window
            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle)

        if (vSync) {
            // Enable v-sync
            glfwSwapInterval(1)
        }
        // Make the window visible
        glfwShowWindow(windowHandle)

        org.lwjgl.opengl.GL.createCapabilities()

        glClearColor(0f, 0f, 0f, 0f)
        //     glEnable(GL_DEPTH_TEST)
        //   glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)

        game._application = this
        game.create()
        while (!windowShouldClose) {
            val delta = getDelta()
            input.record()
            game.render(delta.milliseconds, input)
            glfwSwapBuffers(windowHandle)
            input.reset()
            glfwPollEvents()
        }
        close()
        destroy()
    }

    override fun close() {
        glfwSetWindowShouldClose(windowHandle, true)
    }

    override fun destroy() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(windowHandle)
        glfwDestroyWindow(windowHandle)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun getTime(): Long {
        return System.nanoTime() / 1_000_000
    }
}