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
 * The context of the application. This contains instances and references to the **OpenGL** context and utilities
 * to assist in creating an OpenGL applicaiton.
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

    /**
     * The application runtime stats.
     */
    abstract val stats: AppStats

    /**
     * The configuration this context used for creation
     */
    abstract val configuration: ContextConfiguration

    /**
     * The graphics related properties and instances.
     */
    abstract val graphics: Graphics

    /**
     * The OpenGL instance to make GL calls.
     */
    open val gl: GL get() = graphics.gl

    /**
     * The [Input] of the context.
     */
    abstract val input: Input

    /**
     * The main [Logger] of the context.
     */
    abstract val logger: Logger

    /**
     * The virtual file system access property.
     */
    abstract val vfs: Vfs

    /**
     * A [VfsFile] used for accessing data based on the **resources** directory.
     */
    abstract val resourcesVfs: VfsFile

    /**
     * A [VfsFile] used for storing and reading data based on the **storage** directory.
     */
    abstract val storageVfs: VfsFile

    /**
     * The [Platform] this context is running on.
     */
    abstract val platform: Platform

    /**
     * Clipboard instance for reading and copying to a clipboard.
     */
    abstract val clipboard: Clipboard

    internal abstract fun start(build: (app: Context) -> ContextListener)

    /**
     * Closes and destroys this context.
     */
    abstract fun close()

    internal abstract fun destroy()

    protected fun calcFrameTimes(time: Duration) {
        dt = time - lastFrame
        available = counterTimePerFrame - dt
        lastFrame = time
    }

    /**
     * Creates a new render callback is invoked on every frame.
     */
    open fun onRender(action: suspend (dt: Duration) -> Unit) {
        renderCalls += action
    }

    /**
     * Creates a new post-render callback that is invoked after the _render_ method is finished.
     */
    open fun onPostRender(action: suspend (dt: Duration) -> Unit) {
        postRenderCalls += action
    }

    /**
     * Creates a new _resize_ callback that is invoked whenever the context is resized.
     */
    open fun onResize(action: suspend (width: Int, height: Int) -> Unit) {
        resizeCalls += action
    }

    /**
     * Creates a new _dispose_ callback that is invoked when the context is being destroyed.
     */
    open fun onDispose(action: suspend () -> Unit) {
        disposeCalls += action
    }

    /**
     * Creates a new _postRunnable_ that is invoked one time after the next frame.
     */
    open fun postRunnable(action: suspend () -> Unit) {
        postRunnableCalls += action
    }
}