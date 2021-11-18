package com.lehaine.littlekt

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.LwjglInput
import com.lehaine.littlekt.io.AssetManager
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.io.JvmFileHandler
import com.lehaine.littlekt.log.JvmLogger
import com.lehaine.littlekt.log.Logger
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class PlatformApplication actual constructor(actual override val configuration: ApplicationConfiguration) :
    Application {
    actual override val graphics: Graphics = LwjglGraphics()
    actual override val logger: Logger = JvmLogger(configuration.title)
    actual override val input: Input = LwjglInput(logger, this)
    actual override val assetManager: AssetManager = AssetManager(this)
    actual override val fileHandler: FileHandler = JvmFileHandler(this, logger)

    private var windowHandle: Long = 0

    private val windowShouldClose: Boolean
        get() = GLFW.glfwWindowShouldClose(windowHandle)

    private var lastFrame: Long = getTime()

    private fun getDelta(): Float {
        val time = getTime()
        val delta = (time - lastFrame)
        lastFrame = time
        return min(delta / 1000f, 1 / 60f)
    }

    actual override fun start(gameBuilder: (app: Application) -> LittleKt) {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE) // the window will be resizable

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL30C.GL_TRUE);

        // Create the window
        windowHandle = GLFW.glfwCreateWindow(
            configuration.width,
            configuration.height,
            configuration.title,
            MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if (windowHandle == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        val input = input as LwjglInput
        val graphics = graphics as LwjglGraphics
        val game = gameBuilder(this)

        MemoryStack.stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(windowHandle, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

            check(vidmode != null) { "Unable to retrieve GLFW video mode" }

            // Center the window
            GLFW.glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )

            graphics._backBufferWidth = pWidth[0]
            graphics._backBufferHeight = pHeight[0]
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowHandle)

        if (configuration.vSync) {
            // Enable v-sync
            GLFW.glfwSwapInterval(1)
        }
        // Make the window visible
        GLFW.glfwShowWindow(windowHandle)

        input.attachHandler(windowHandle)

        GLFW.glfwSetFramebufferSizeCallback(windowHandle) { _, width, height ->
            graphics.GL.viewport(0, 0, width, height)
            graphics._backBufferWidth = width
            graphics._backBufferHeight = height
            game.resize(width, height)
        }

        org.lwjgl.opengl.GL.createCapabilities()

        GL30C.glClearColor(0f, 0f, 0f, 0f)
        //     glEnable(GL_DEPTH_TEST)
        //   glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)

        game.create()
        game.resize(configuration.width, configuration.height)

        while (!windowShouldClose) {
            val delta = getDelta()
            input.update()
            game.render(delta)
            GLFW.glfwSwapBuffers(windowHandle)
            input.reset()
            GLFW.glfwPollEvents()
        }
        close()
        destroy()
    }

    actual override fun close() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true)
    }

    actual override fun destroy() {
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowHandle)
        GLFW.glfwDestroyWindow(windowHandle)

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()
    }

    private fun getTime(): Long {
        return System.nanoTime() / 1_000_000
    }
}