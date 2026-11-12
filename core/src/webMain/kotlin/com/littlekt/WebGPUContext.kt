package com.littlekt

import com.littlekt.async.KT
import com.littlekt.audio.Audio
import com.littlekt.audio.WebAudio
import com.littlekt.async.KtScope
import com.littlekt.async.await
import com.littlekt.file.*
import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.webgpu.Adapter
import com.littlekt.graphics.webgpu.GPUAdapter
import com.littlekt.graphics.webgpu.GPUDevice
import com.littlekt.graphics.webgpu.GPURequestAdapterOptions
import com.littlekt.graphics.webgpu.navigator
import com.littlekt.input.JsInput
import com.littlekt.log.Logger
import com.littlekt.resources.internal.InternalResources
import com.littlekt.util.datastructure.fastForEach
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

/**
 * @author Colton Daily
 * @date 4/18/2024
 */
class WebGPUContext(override val configuration: JsConfiguration, gpuAdapter: GPUAdapter, gpuDevice: GPUDevice) : Context() {

    private val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    override val stats: AppStats = AppStats()
    override val graphics: WebGPUGraphics = WebGPUGraphics(canvas, gpuAdapter, gpuDevice)
    override val input: JsInput = JsInput(canvas)
    override val window: WebWindow = WebWindow(canvas)
    override val audio: Audio = WebAudio
    override val logger: Logger = Logger(configuration.title)
    override val vfsResources: Vfs = WebLocalVfs(this, logger, configuration.resourcesPath)
    override val vfsUrl: Vfs = WebUrlVfs(this, logger)
    override val vfsApplication: Vfs = WebLocalVfs(this, logger, configuration.applicationPath)

    override val resourcesVfs: VfsFile
        get() = vfsResources.root

    override val urlVfs: VfsFile
        get() = vfsUrl.root

    override val applicationVfs: VfsFile
        get() = vfsApplication.root

    override val kvStorage: KeyValueStorage = WebKeyValueStorage(logger)

    override val platform: Platform = Platform.WEB
    override val clipboard: JsClipboard = JsClipboard()

    private lateinit var listener: ContextListener
    private var closed = false
    private var lastFrameTime = 0.0
    private var lastMode: WindowMode = WindowMode.Windowed
    private var lastContentScale = 1f

    init {
        KtScope.initiate()
        require(configuration.maxAudioClipInstances > 0) {
            "maxAudioClipInstances must be greater than 0!"
        }
        WebAudio.maxInstancesPerClip = configuration.maxAudioClipInstances
    }

    override fun start(build: (app: Context) -> ContextListener) {
        pinAutoCanvasSize()
        syncCanvasSize()

        KtScope.launch {
            val adapterOptions = GPURequestAdapterOptions().apply {
                powerPreference = configuration.powerPreference.nativeFlag
            }
            graphics.adapter = Adapter(navigator.gpu.requestAdapter(adapterOptions).await())
            graphics.device = graphics.adapter.requestDevice()
            if (configuration.loadInternalResources) {
                InternalResources.createInstance(this@WebGPUContext)
                InternalResources.INSTANCE.load()
            }
            listener = build(this@WebGPUContext)
            listener.run {
                start()
                resizeCalls.fastForEach { it.invoke(graphics.width, graphics.height) }
            }
        }
        attachWindowEvents()
        kotlinx.browser.window.requestAnimationFrame(::update)
    }

    private fun attachWindowEvents() {
        lastContentScale = window.contentScale
        kotlinx.browser.window.addEventListener("focus", { focusChangedCalls.fastForEach { it(true) } })
        kotlinx.browser.window.addEventListener("blur", { focusChangedCalls.fastForEach { it(false) } })
        document.addEventListener(
            "visibilitychange",
            {
                val hidden = !window.minimized
                window.minimized = hidden
                minimizedChangedCalls.fastForEach { it(hidden) }
            },
        )
        document.addEventListener(
            "fullscreenchange",
            {
                val mode = window.mode
                if (mode != lastMode) {
                    lastMode = mode
                    windowModeChangedCalls.fastForEach { it(mode) }
                }
            },
        )
    }

    private fun update(now: Double) {
        val limit = frameRateLimit
        if (limit != null && lastFrameTime > 0.0 && now - lastFrameTime < 1000.0 / limit - 1.0) {
            kotlinx.browser.window.requestAnimationFrame(::update)
            return
        }
        lastFrameTime = now
        val scale = window.contentScale
        if (scale != lastContentScale) {
            lastContentScale = scale
            contentScaleChangedCalls.fastForEach { it(scale) }
        }
        if (syncCanvasSize()) {
            listener.run {
                resizeCalls.fastForEach { resize -> resize(graphics.width, graphics.height) }
            }
        }
        EngineStats.resetPerFrameCounts()

        invokeAnyRunnable()

        calcFrameTimes(now.milliseconds)
        Dispatchers.KT.executePending(available)

        input.update()
        stats.update(dt)

        updateCalls.fastForEach { update -> update(dt) }
        postUpdateCalls.fastForEach { postUpdate -> postUpdate(dt) }

        input.reset()

        if (closed) {
            destroy()
        } else {
            kotlinx.browser.window.requestAnimationFrame(::update)
        }
    }

    /**
     * A canvas without a CSS size is laid out at its buffer size, which would grow every time the
     * buffer is scaled by the device pixel ratio. Such a canvas keeps its current size in CSS.
     */
    private fun pinAutoCanvasSize() {
        if (canvas.style.width.isNotEmpty() || canvas.style.height.isNotEmpty()) return
        val width = canvas.clientWidth
        val height = canvas.clientHeight
        canvas.width = width + 1
        val followsBuffer = canvas.clientWidth != width
        canvas.width = width
        if (followsBuffer) {
            canvas.style.width = "${width}px"
            canvas.style.height = "${height}px"
        }
    }

    /**
     * Sizes the canvas back buffer to its CSS size times the device pixel ratio, so it's drawn at
     * the screen's full resolution.
     *
     * @return `true` if the size changed
     */
    private fun syncCanvasSize(): Boolean {
        val width = canvas.clientWidth
        val height = canvas.clientHeight
        val ratio = kotlinx.browser.window.devicePixelRatio
        val backWidth = (width * ratio).roundToInt()
        val backHeight = (height * ratio).roundToInt()
        if (
            width == graphics.width &&
                height == graphics.height &&
                backWidth == graphics.backBufferWidth &&
                backHeight == graphics.backBufferHeight
        ) {
            return false
        }
        graphics._width = width
        graphics._height = height
        graphics._backBufferWidth = backWidth
        graphics._backBufferHeight = backHeight
        canvas.width = backWidth
        canvas.height = backHeight
        return true
    }

    private fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable -> postRunnable.invoke() }
            postRunnableCalls.clear()
        }
    }

    override fun close() {
        closed = true
    }

    override fun destroy() {
        KtScope.launch { releaseCalls.fastForEach { release -> release() } }
    }
}