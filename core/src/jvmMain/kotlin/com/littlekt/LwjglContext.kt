package com.littlekt

import com.littlekt.async.KtScope
import com.littlekt.async.MainDispatcher
import com.littlekt.async.mainThread
import com.littlekt.audio.Audio
import com.littlekt.audio.OpenALAudioContext
import com.littlekt.file.*
import com.littlekt.graphics.HdpiMode
import com.littlekt.file.Base64.decodeFromBase64
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.readPixmap
import com.littlekt.input.LwjglInput
import com.littlekt.log.Logger
import com.littlekt.resources.internal.InternalResources
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.now
import ffi.globalMemory
import io.ygdrasil.wgpu.WGPULogCallback
import io.ygdrasil.wgpu.WGPULogLevel_Debug
import io.ygdrasil.wgpu.WGPULogLevel_Error
import io.ygdrasil.wgpu.WGPULogLevel_Info
import io.ygdrasil.wgpu.WGPULogLevel_Trace
import io.ygdrasil.wgpu.WGPULogLevel_Warn
import io.ygdrasil.wgpu.wgpuSetLogCallback
import io.ygdrasil.wgpu.wgpuSetLogLevel
import java.lang.foreign.Arena
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.system.MemoryUtil

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LwjglContext(override val configuration: JvmConfiguration) : Context() {

    override val stats: AppStats = AppStats()
    override val graphics: LwjglGraphics = LwjglGraphics(this)
    override val logger: Logger = Logger(configuration.title)
    override val vfsResources: Vfs = JvmResourcesVfs(this, logger)
    override val vfsUrl: Vfs = JvmUrlVfs(this, logger)
    override val vfsApplication: Vfs = JvmApplicationVfs(this, logger, ".")
    override val input: LwjglInput = LwjglInput(this)
    override val window: LwjglWindow = LwjglWindow(this)
    override val resourcesVfs: VfsFile
        get() = vfsResources.root

    override val urlVfs: VfsFile
        get() = vfsUrl.root

    override val applicationVfs: VfsFile
        get() = vfsApplication.root

    override val kvStorage: KeyValueStorage = JvmKeyValueStorage(logger, "./.storage")

    override val platform: Platform = Platform.DESKTOP

    override val clipboard: JvmClipboard by lazy { JvmClipboard(windowHandle) }

    internal var windowHandle: Long = 0
        private set

    internal val audioContext = OpenALAudioContext(configuration.maxAudioSources)

    override val audio: Audio
        get() = audioContext

    private val windowShouldClose: Boolean
        get() = GLFW.glfwWindowShouldClose(windowHandle)

    private val tempBuffer: IntBuffer = BufferUtils.createIntBuffer(1)
    private val tempBuffer2: IntBuffer = BufferUtils.createIntBuffer(1)

    private val scope = Arena.ofConfined()

    private val wgpuLogger = Logger("WGPU")

    /**
     * `true` when the platform measures windows in pixels, such as Windows & X11, and logical
     * units are wanted. Window coordinates are then divided by [windowScale].
     */
    internal var scalesWindowCoordinates = false
        private set

    /** The number of window coordinates per logical unit. */
    internal val windowScale: Float
        get() = if (scalesWindowCoordinates) window.contentScale.coerceAtLeast(0.01f) else 1f

    private var started = false
    private var inFrame = false
    private var resizePending = false

    init {
        KtScope.initiate()
        mainThread = Thread.currentThread()
        if (configuration.enableWGPULogging) {
            initLogging()
        }
    }

    override fun start(build: (app: Context) -> ContextListener) = runBlocking {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(
            GLFW.GLFW_VISIBLE,
            GLFW.GLFW_FALSE,
        ) // the window will stay hidden after creation
        GLFW.glfwWindowHint(
            GLFW.GLFW_RESIZABLE,
            configuration.resizeable.glfw,
        ) // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, configuration.maximized.glfw)
        val platform = GLFW.glfwGetPlatform()
        scalesWindowCoordinates =
            configuration.hdpiMode == HdpiMode.LOGICAL &&
                (platform == GLFW.GLFW_PLATFORM_WIN32 || platform == GLFW.GLFW_PLATFORM_X11)
        GLFW.glfwWindowHint(GLFW.GLFW_SCALE_TO_MONITOR, scalesWindowCoordinates.glfw)
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API) // prevent opengl

        val isMac = System.getProperty("os.name").lowercase().contains("mac")

        // Create the window
        windowHandle =
            GLFW.glfwCreateWindow(
                configuration.width,
                configuration.height,
                configuration.title,
                MemoryUtil.NULL,
                MemoryUtil.NULL,
            )
        if (windowHandle == MemoryUtil.NULL)
            throw RuntimeException("Failed to create the GLFW window")

        graphics.createInstance(configuration)
        graphics.configureSurfaceToWindow(windowHandle)
        graphics.requestAdapterAndDevice(configuration.powerPreference)

        updateFramebufferInfo()

        // Get the resolution of the primary monitor
        val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

        check(vidmode != null) { "Unable to retrieve GLFW video mode" }

        // Center the window
        GLFW.glfwSetWindowPos(
            windowHandle,
            configuration.windowPosX ?: ((vidmode.width() - graphics.width) / 2),
            configuration.windowPosY ?: ((vidmode.height() - graphics.height) / 2),
        )

        // set window icon
        if (!isMac) {
            if (configuration.icons.isNotEmpty()) {
                val buffer = GLFWImage.malloc(configuration.icons.size)
                configuration.icons.forEach {
                    val pixmap = resourcesVfs[it].readPixmap()
                    val icon = GLFWImage.malloc()
                    icon.set(
                        pixmap.width,
                        pixmap.height,
                        ByteBuffer.allocateDirect(pixmap.pixels.capacity)
                            .put(pixmap.pixels.toArray())
                            .flip(),
                    )
                    buffer.put(icon)
                    icon.free()
                }
                buffer.flip()
                GLFW.glfwSetWindowIcon(windowHandle, buffer)
                buffer.free()
            } else {
                val pixmap = ktHead32x32.decodeFromBase64().readPixmap()
                val icon = GLFWImage.malloc()
                icon.set(
                    pixmap.width,
                    pixmap.height,
                    ByteBuffer.allocateDirect(pixmap.pixels.capacity)
                        .put(pixmap.pixels.toArray())
                        .flip(),
                )
                val buffer = GLFWImage.malloc(1)
                buffer.put(icon)
                icon.free()
                buffer.flip()
                GLFW.glfwSetWindowIcon(windowHandle, buffer)
                buffer.free()
            }
        }

        applyStartWindowMode()

        // Make the window visible
        GLFW.glfwShowWindow(windowHandle)
        input.attachToWindow(windowHandle)
        attachWindowCallbacks()

        if (configuration.loadInternalResources) {
            InternalResources.createInstance(this@LwjglContext)
            InternalResources.INSTANCE.load()
        }

        val listener: ContextListener = build(this@LwjglContext)

        GLFW.glfwSetFramebufferSizeCallback(windowHandle) { _, _, _ ->
            updateFramebufferInfo()
            resizePending = true
        }
        GLFW.glfwSetWindowRefreshCallback(windowHandle) { _ ->
            // Windows blocks inside glfwPollEvents while the window is resized. Running frames from
            // here keeps the window drawing during the resize instead of freezing.
            if (started && resizePending && !inFrame) runBlocking { frame() }
        }

        listener.run { start() }
        updateFramebufferInfo()
        resizePending = true
        started = true

        while (!windowShouldClose) {
            val frameStart = System.nanoTime()
            frame()
            GLFW.glfwPollEvents()
            limitFrameRate(frameStart)
        }

        releaseCalls.fastForEach { release -> release() }
        destroy()
    }

    private fun initLogging() {
        val callback = WGPULogCallback.allocate(globalMemory) { level, cMessage, userdata ->
            val message = cMessage?.data?.toKString(cMessage.length) ?: "empty message"
            when (level) {
                WGPULogLevel_Error -> logger.error { message }
                WGPULogLevel_Warn -> logger.warn { message }
                WGPULogLevel_Info -> logger.info { message }
                WGPULogLevel_Debug -> logger.debug { message }
                WGPULogLevel_Trace -> logger.trace { message }
                else -> logger.warn { "Unknown log level $level with message $message" }
            }
        }

        wgpuSetLogLevel(WGPULogLevel_Trace)
        wgpuSetLogCallback(callback, globalMemory.bufferOfAddress(callback.handler).handler)
    }

    private fun updateFramebufferInfo() {
        GLFW.glfwGetWindowSize(windowHandle, tempBuffer, tempBuffer2)

        val scale = windowScale
        graphics._logicalWidth = (tempBuffer[0] / scale).roundToInt()
        graphics._logicalHeight = (tempBuffer2[0] / scale).roundToInt()

        GLFW.glfwGetFramebufferSize(windowHandle, tempBuffer, tempBuffer2)

        graphics._backBufferWidth = tempBuffer[0]
        graphics._backBufferHeight = tempBuffer2[0]
    }

    private suspend fun frame() {
        inFrame = true
        calcFrameTimes(now().milliseconds)
        if (resizePending && graphics.backBufferWidth > 0 && graphics.backBufferHeight > 0) {
            resizePending = false
            resizeCalls.fastForEach { resize -> resize(graphics.width, graphics.height) }
        }
        MainDispatcher.INSTANCE.executePending(available)
        update(dt)
        inFrame = false
    }

    private fun limitFrameRate(frameStart: Long) {
        val limit = frameRateLimit ?: return
        val end = frameStart + 1_000_000_000L / limit
        while (true) {
            val remaining = end - System.nanoTime()
            if (remaining <= 0L) break
            if (remaining > 2_000_000L) Thread.sleep((remaining - 1_000_000L) / 1_000_000L)
            else Thread.onSpinWait()
        }
    }

    private fun applyStartWindowMode() {
        val primary = window.primaryMonitor ?: return
        when (configuration.windowMode) {
            StartWindowMode.WINDOWED -> Unit
            StartWindowMode.BORDERLESS -> window.setMode(WindowMode.Borderless(primary))
            StartWindowMode.FULLSCREEN ->
                window.setMode(WindowMode.Fullscreen(primary, primary.currentMode))
        }
    }

    private fun attachWindowCallbacks() {
        GLFW.glfwSetWindowFocusCallback(windowHandle) { _, focused ->
            focusChangedCalls.fastForEach { it(focused) }
        }
        GLFW.glfwSetWindowIconifyCallback(windowHandle) { _, minimized ->
            minimizedChangedCalls.fastForEach { it(minimized) }
        }
        GLFW.glfwSetWindowContentScaleCallback(windowHandle) { _, scale, _ ->
            updateFramebufferInfo()
            resizePending = true
            contentScaleChangedCalls.fastForEach { it(scale) }
        }
        GLFW.glfwSetWindowCloseCallback(windowHandle) { handle ->
            if (!allowClose()) GLFW.glfwSetWindowShouldClose(handle, false)
        }
        GLFW.glfwSetMonitorCallback { _, _ ->
            val monitors = window.onMonitorsChanged()
            monitorsChangedCalls.fastForEach { it(monitors) }
        }
    }

    internal fun dispatchWindowModeChanged(mode: WindowMode) {
        windowModeChangedCalls.fastForEach { it(mode) }
    }

    private suspend fun update(dt: Duration) {
        audioContext.update()
        if (audioContext.pollDevicesChanged()) {
            val devices = audioContext.outputDevices
            audioDevicesChangedCalls.fastForEach { it(devices) }
        }

        EngineStats.resetPerFrameCounts()

        input.update()
        stats.update(dt)

        updateCalls.fastForEach { update -> update(dt) }
        postUpdateCalls.fastForEach { postUpdate -> postUpdate(dt) }

        input.reset()

        invokeAnyRunnable()
    }

    private fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable -> postRunnable.invoke() }
            postRunnableCalls.clear()
        }
    }

    override fun close() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true)
    }

    override fun destroy() {
        super.destroy()
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowHandle)
        GLFW.glfwSetMonitorCallback(null)?.free()
        GLFW.glfwDestroyWindow(windowHandle)

        graphics.release()

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()

        scope.close()

        audioContext.release()
    }

    private val Boolean.glfw: Int
        get() = if (this) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE
}
