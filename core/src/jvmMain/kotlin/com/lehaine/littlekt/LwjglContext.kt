package com.lehaine.littlekt

import com.lehaine.littlekt.audio.OpenALContext
import com.lehaine.littlekt.file.JvmVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.LwjglInput
import com.lehaine.littlekt.log.Logger
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import org.lwjgl.opengl.GL as LWJGL


/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LwjglContext(override val configuration: JvmConfiguration) : Context {

    override val stats: AppStats = AppStats()
    override val graphics: Graphics = LwjglGraphics(stats.engineStats)
    override val logger: Logger = Logger(configuration.title)
    override val input: Input = LwjglInput(logger, this)
    override val vfs = JvmVfs(this, logger, "./.storage", ".")
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = VfsFile(vfs, "./.storage")

    override val platform: Context.Platform = Context.Platform.DESKTOP

    private val mainThreadRunnables = mutableListOf<GpuThreadRunnable>()

    private var windowHandle: Long = 0

    private val windowShouldClose: Boolean
        get() = GLFW.glfwWindowShouldClose(windowHandle)

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalTime::class)
    override fun start(build: (app: Context) -> ContextListener) {
        val graphics = graphics as LwjglGraphics
        val input = input as LwjglInput

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        // Create temporary window for getting OpenGL Version
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        val temp: Long = GLFW.glfwCreateWindow(1, 1, "", MemoryUtil.NULL, MemoryUtil.NULL)
        GLFW.glfwMakeContextCurrent(temp)
        LWJGL.createCapabilities()
        val caps: GLCapabilities = LWJGL.getCapabilities()
        GLFW.glfwDestroyWindow(temp)

        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default

        when {
            caps.OpenGL32 -> {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL30.GL_TRUE)
                graphics._glVersion = GLVersion.GL_32_PLUS
            }
            caps.OpenGL30 -> {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL30.GL_TRUE)
                graphics._glVersion = GLVersion.GL_30
            }
            caps.OpenGL21 -> {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1)
                graphics._glVersion = GLVersion.GL_20
            }
        }

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE) // the window will be resizable

        // Create the window
        windowHandle = GLFW.glfwCreateWindow(
            configuration.width,
            configuration.height,
            configuration.title,
            MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if (windowHandle == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

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

            graphics._width = pWidth[0]
            graphics._height = pHeight[0]
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowHandle)

        if (configuration.vSync) {
            // Enable v-sync
            GLFW.glfwSwapInterval(1)
        }
        // Make the window visible
        GLFW.glfwShowWindow(windowHandle)
        input.attachToWindow(windowHandle)

        LWJGL.createCapabilities()
        // GLUtil.setupDebugMessageCallback()

        GL30C.glClearColor(0f, 0f, 0f, 0f)

        var lastFrame = System.nanoTime()

        val listener = build(this)
        GLFW.glfwSetFramebufferSizeCallback(windowHandle) { _, width, height ->
            graphics.gl.viewport(0, 0, width, height)
            graphics._width = width
            graphics._height = height

            listener.resize(width, height)
        }

        Textures.default.prepare(this)
        listener.resize(configuration.width, configuration.height)

        while (!windowShouldClose) {
            stats.engineStats.resetPerFrameCounts()
            glClear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
            invokeAnyRunnable()

            val time = System.nanoTime()
            val dt = ((time - lastFrame) / 1e9).seconds
            lastFrame = time

            input.update()
            stats.update(dt)
            listener.render(dt)

            GLFW.glfwSwapBuffers(windowHandle)
            input.reset()
            GLFW.glfwPollEvents()

            invokeAnyRunnable()
        }
        listener.dispose()
        destroy()
    }

    private fun invokeAnyRunnable() {
        synchronized(mainThreadRunnables) {
            if (mainThreadRunnables.isNotEmpty()) {
                for (r in mainThreadRunnables) {
                    r.run()
                    r.future.complete(null)
                }
                mainThreadRunnables.clear()
            }
        }
    }

    override fun close() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true)
    }

    override fun destroy() {
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowHandle)
        GLFW.glfwDestroyWindow(windowHandle)

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()

        OpenALContext.destroy()
    }

    override fun runOnMainThread(action: () -> Unit) {
        synchronized(mainThreadRunnables) {
            mainThreadRunnables += GpuThreadRunnable(action)
        }
    }

    private class GpuThreadRunnable(val run: () -> Unit) {
        val future = CompletableFuture<Void>()
    }
}