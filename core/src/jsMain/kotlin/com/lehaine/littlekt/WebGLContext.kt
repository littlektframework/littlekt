package com.lehaine.littlekt

import com.lehaine.littlekt.file.WebVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.internal.InternalResources
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.JsInput
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.fastForEach
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
class WebGLContext(override val configuration: JsConfiguration) : Context {

    private val job = Job()
    override val coroutineContext: CoroutineContext = job

    val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    override val stats: AppStats = AppStats()
    override val graphics: Graphics = WebGLGraphics(canvas, stats.engineStats)
    override val input: Input = JsInput(canvas)
    override val logger: Logger = Logger(configuration.title)
    override val vfs = WebVfs(this, logger, configuration.rootPath)
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = vfs.root
    override val platform: Context.Platform = Context.Platform.JS

    private lateinit var listener: ContextListener
    private var lastFrame = 0.0
    private var closed = false

    private val renderCalls = mutableListOf<suspend (Duration) -> Unit>()
    private val postRenderCalls = mutableListOf<suspend (Duration) -> Unit>()
    private val resizeCalls = mutableListOf<suspend (Int, Int) -> Unit>()
    private val disposeCalls = mutableListOf<suspend () -> Unit>()

    private val mainThreadRunnables = mutableListOf<GpuThreadRunnable>()

    override suspend fun start(build: (app: Context) -> ContextListener) {
        graphics as WebGLGraphics
        input as JsInput

        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        InternalResources.createInstance(this)
        listener = build(this)
        listener.run { start() }

        window.requestAnimationFrame(::render)
    }

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalTime::class)
    private fun render(now: Double) {
        if (canvas.clientWidth != graphics.width ||
            canvas.clientHeight != graphics.height
        ) {
            graphics as WebGLGraphics
            graphics._width = canvas.clientWidth
            graphics._height = canvas.clientHeight
            canvas.width = canvas.clientWidth
            canvas.height = canvas.clientHeight
            launch {
                listener.run {
                    resizeCalls.fastForEach { resize ->
                        resize(
                            graphics.width, graphics.height
                        )
                    }
                }
            }
        }
        stats.engineStats.resetPerFrameCounts()
        invokeAnyRunnable()

        input as JsInput
        val dt = ((now - lastFrame) / 1000.0).seconds
        lastFrame = now

        input.update()
        stats.update(dt)

        renderCalls.fastForEach { render ->
            launch {
                render(dt)
            }
        }
        postRenderCalls.fastForEach { postRender ->
            launch {
                postRender(dt)
            }
        }
        input.reset()

        invokeAnyRunnable()
        if (closed) {
            launch {
                destroy()
            }
        } else {
            window.requestAnimationFrame(::render)
        }
    }

    private fun invokeAnyRunnable() {
        if (mainThreadRunnables.isNotEmpty()) {
            mainThreadRunnables.forEach {
                it.run()
            }
            mainThreadRunnables.clear()
        }
    }

    override suspend fun close() {
        closed = true
    }

    override suspend fun destroy() {
        disposeCalls.fastForEach { dispose -> dispose() }
    }

    override suspend fun onRender(action: suspend (dt: Duration) -> Unit) {
        renderCalls += action
    }

    override suspend fun onPostRender(action: suspend (dt: Duration) -> Unit) {
        postRenderCalls += action
    }

    override suspend fun onResize(action: suspend (width: Int, height: Int) -> Unit) {
        resizeCalls += action
    }

    override suspend fun onDispose(action: suspend () -> Unit) {
        disposeCalls += action
    }


    private class GpuThreadRunnable(val run: () -> Unit)
}