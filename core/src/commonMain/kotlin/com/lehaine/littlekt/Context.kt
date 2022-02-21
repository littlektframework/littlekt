package com.lehaine.littlekt

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.Clipboard
import com.lehaine.littlekt.util.internal.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
abstract class Context {

    enum class Platform {
        DESKTOP,
        WEBGL,
        WEBGL2,
        ANDROID,
        IOS;

        val isWebGl get() = this == WEBGL || this == WEBGL2
        val isMobile get() = this == ANDROID || this == IOS
    }

    protected val renderCalls = mutableListOf<suspend (Duration) -> Unit>()
    protected val postRenderCalls = mutableListOf<suspend (Duration) -> Unit>()
    protected val resizeCalls = mutableListOf<suspend (Int, Int) -> Unit>()
    protected val disposeCalls = mutableListOf<suspend () -> Unit>()
    protected val postRunnableCalls = mutableListOf<suspend () -> Unit>()

    protected var lastFrame: Duration = now().milliseconds
    protected var dt: Duration = Duration.ZERO
    protected var available: Duration = Duration.ZERO

    protected val counterTimePerFrame get() = (1_000_000.0 / stats.fps).microseconds

    abstract val stats: AppStats

    abstract val configuration: ContextConfiguration

    abstract val graphics: Graphics

    open val gl: GL get() = graphics.gl

    abstract val input: Input

    abstract val logger: Logger

    abstract val vfs: Vfs

    abstract val resourcesVfs: VfsFile

    abstract val storageVfs: VfsFile

    abstract val platform: Platform

    abstract val clipboard: Clipboard

    internal abstract fun start(build: (app: Context) -> ContextListener)

    abstract fun close()

    internal abstract fun destroy()

    protected fun calcFrameTimes(time: Duration) {
        dt = time - lastFrame
        available = counterTimePerFrame - dt
        lastFrame = time
    }

    open fun onRender(action: suspend (dt: Duration) -> Unit) {
        renderCalls += action
    }

    open fun onPostRender(action: suspend (dt: Duration) -> Unit) {
        postRenderCalls += action
    }

    open fun onResize(action: suspend (width: Int, height: Int) -> Unit) {
        resizeCalls += action
    }

    open fun onDispose(action: suspend () -> Unit) {
        disposeCalls += action
    }

    open fun postRunnable(action: suspend () -> Unit) {
        postRunnableCalls += action
    }
}