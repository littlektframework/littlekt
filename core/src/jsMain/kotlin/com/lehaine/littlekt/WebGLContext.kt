package com.lehaine.littlekt

import com.lehaine.littlekt.async.KT
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.file.WebVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.internal.InternalResources
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.JsInput
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.fastForEach
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
class WebGLContext(override val configuration: JsConfiguration) : Context {

    override val coroutineContext: CoroutineContext get() = KtScope.coroutineContext

    private val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

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
    private val postRunnableCalls = mutableListOf<suspend () -> Unit>()

    private val counterTimerPerFrame: Duration get() = (1_000_000.0 / stats.fps).microseconds

    override fun start(build: (app: Context) -> ContextListener) {
        KtScope.initiate(this)
        graphics as WebGLGraphics
        input as JsInput

        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        InternalResources.createInstance(this)
        listener = build(this)
        launch {
            listener.run { start() }
        }
        window.requestAnimationFrame(::render)
    }

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalTime::class)
    private fun render(now: Double) {
        launch {
            if (canvas.clientWidth != graphics.width ||
                canvas.clientHeight != graphics.height
            ) {
                graphics as WebGLGraphics
                graphics._width = canvas.clientWidth
                graphics._height = canvas.clientHeight
                canvas.width = canvas.clientWidth
                canvas.height = canvas.clientHeight
                listener.run {
                    resizeCalls.fastForEach { resize ->
                        resize(
                            graphics.width, graphics.height
                        )
                    }
                }
            }
            stats.engineStats.resetPerFrameCounts()

            invokeAnyRunnable()

            input as JsInput
            val dt = ((now - lastFrame) / 1000.0).seconds
            val available = counterTimerPerFrame - dt
            Dispatchers.KT.executePending(available)
            lastFrame = now

            input.update()
            stats.update(dt)

            renderCalls.fastForEach { render ->
                render(dt)

            }
            postRenderCalls.fastForEach { postRender ->
                postRender(dt)
            }

            input.reset()

            if (closed) {
                destroy()

            } else {
                window.requestAnimationFrame(::render)
            }
        }
    }

    private suspend fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable ->
                postRunnable.invoke()
            }
            postRunnableCalls.clear()
        }
    }

    override fun close() {
        closed = true
    }

    override fun destroy() {
        launch {
            disposeCalls.fastForEach { dispose -> dispose() }
        }
    }

    override fun onRender(action: suspend (dt: Duration) -> Unit) {
        renderCalls += action
    }

    override fun onPostRender(action: suspend (dt: Duration) -> Unit) {
        postRenderCalls += action
    }

    override fun onResize(action: suspend (width: Int, height: Int) -> Unit) {
        resizeCalls += action
    }

    override fun onDispose(action: suspend () -> Unit) {
        disposeCalls += action
    }

    override fun postRunnable(action: suspend () -> Unit) {
        postRunnableCalls += action
    }
}