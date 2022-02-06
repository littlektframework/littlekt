package com.lehaine.littlekt

import com.lehaine.littlekt.async.KT
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.file.WebVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.internal.InternalResources
import com.lehaine.littlekt.input.JsInput
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.fastForEach
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
class WebGLContext(override val configuration: JsConfiguration) : Context() {

    private val canvas = document.getElementById(configuration.canvasId) as HTMLCanvasElement

    override val stats: AppStats = AppStats()
    override val graphics: WebGLGraphics = WebGLGraphics(canvas, stats.engineStats)
    override val input: JsInput = JsInput(canvas)
    override val logger: Logger = Logger(configuration.title)
    override val vfs = WebVfs(this, logger, configuration.rootPath)
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = vfs.root
    override val platform: Platform = Platform.WEBGL

    private lateinit var listener: ContextListener
    private var closed = false

    override fun start(build: (app: Context) -> ContextListener) {
        graphics._width = canvas.clientWidth
        graphics._height = canvas.clientHeight

        KtScope.launch {
            InternalResources.createInstance(this@WebGLContext)
            InternalResources.INSTANCE.load()
            listener = build(this@WebGLContext)
            listener.run { start() }
        }
        window.requestAnimationFrame(::render)
    }

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalTime::class)
    private fun render(now: Double) {
        KtScope.launch {
            if (canvas.clientWidth != graphics.width ||
                canvas.clientHeight != graphics.height
            ) {
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

            calcFrameTimes(now.milliseconds)
            Dispatchers.KT.executePending(available)

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
        KtScope.launch {
            disposeCalls.fastForEach { dispose -> dispose() }
        }
    }
}