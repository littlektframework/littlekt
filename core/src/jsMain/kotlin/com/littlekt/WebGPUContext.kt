package com.littlekt

import com.littlekt.async.KT
import com.littlekt.async.KtScope
import com.littlekt.file.*
import com.littlekt.file.vfs.VfsFile
import com.littlekt.input.JsInput
import com.littlekt.log.Logger
import com.littlekt.resources.internal.InternalResources
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.internal.jsObject
import io.ygdrasil.webgpu.internal.js.GPURequestAdapterOptions
import io.ygdrasil.webgpu.requestAdapter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

/**
 * @author Colton Daily
 * @date 4/18/2024
 */
class WebGPUContext(override val configuration: JsConfiguration) : Context() {

    private val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    override val stats: AppStats = AppStats()
    override val graphics: WebGPUGraphics = WebGPUGraphics(canvas)
    override val input: JsInput = JsInput(canvas)
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

    init {
        KtScope.initiate()
    }

    override fun start(build: (app: Context) -> ContextListener) {
        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        KtScope.launch {
            val adapterOptions: GPURequestAdapterOptions = jsObject {
                powerPreference = configuration.powerPreference.value
            }
            graphics.adapter = requestAdapter(adapterOptions) ?: error("No appropriate Adapter found.")
            graphics.device = graphics.adapter.requestDevice() ?: error("No appropriate Device found.")
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
        window.requestAnimationFrame(::update)
    }

    private fun update(now: Double) {
        if (canvas.clientWidth != graphics.width || canvas.clientHeight != graphics.height) {
            graphics._width = canvas.clientWidth
            graphics._height = canvas.clientHeight
            canvas.width = canvas.clientWidth
            canvas.height = canvas.clientHeight
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
            window.requestAnimationFrame(::update)
        }
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
