package com.littlekt

import com.littlekt.file.KeyValueStorage
import com.littlekt.file.Vfs
import com.littlekt.file.vfs.VfsFile
import com.littlekt.input.Input
import com.littlekt.log.Logger
import com.littlekt.util.Clipboard
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * The context of the application. This contains instances and references to the **WebGPU** context
 * and utilities to assist in creating an OpenGL application.
 *
 * @author Colton Daily
 * @date 10/5/2021
 */
abstract class Context {

    /** Denotes the platform the context is currently running on. */
    enum class Platform {
        /** Desktop (e.g JVM) */
        DESKTOP,

        /** Web browser. */
        WEB,

        /** Android mobile platform. */
        ANDROID,

        /** iOS mobile platform. */
        IOS;

        /** @return `true` if the value is either [ANDROID] or [IOS]. */
        val isMobile: Boolean
            get() = this == ANDROID || this == IOS
    }

    protected val updateCalls = mutableListOf<(Duration) -> Unit>()
    protected val postUpdateCalls = mutableListOf<(Duration) -> Unit>()
    protected val resizeCalls = mutableListOf<(Int, Int) -> Unit>()
    protected val focusChangedCalls = mutableListOf<(Boolean) -> Unit>()
    protected val minimizedChangedCalls = mutableListOf<(Boolean) -> Unit>()
    protected val contentScaleChangedCalls = mutableListOf<(Float) -> Unit>()
    protected val windowModeChangedCalls = mutableListOf<(WindowMode) -> Unit>()
    protected val monitorsChangedCalls = mutableListOf<(List<Monitor>) -> Unit>()
    protected val closeRequestedCalls = mutableListOf<() -> Boolean>()
    protected val releaseCalls = mutableListOf<suspend () -> Unit>()
    protected val postRunnableCalls = mutableListOf<() -> Unit>()

    protected var lastFrame: Duration = now().milliseconds
    protected var dt: Duration = Duration.ZERO
    protected var available: Duration = Duration.ZERO

    protected val counterTimePerFrame
        get() = (1_000_000.0 / stats.fps).microseconds

    /** The application runtime stats. */
    abstract val stats: AppStats

    /** The configuration this context used for creation */
    abstract val configuration: ContextConfiguration

    /** The graphics related properties and instances. */
    abstract val window: Window

    /**
     * The maximum number of frames per second, or `null` for no limit. The limit is on top of any
     * limit from the surface present mode, such as vsync.
     */
    var frameRateLimit: Int? = null
        set(value) {
            require(value == null || value > 0) { "frameRateLimit must be greater than 0!" }
            field = value
        }

    abstract val graphics: Graphics

    /** The [Input] of the context. */
    abstract val input: Input

    /** The main [Logger] of the context. */
    abstract val logger: Logger

    /** The virtual file system access property. */
    abstract val vfsResources: Vfs

    /** The virtual file system access property. */
    abstract val vfsUrl: Vfs

    /** The virtual file system access property. */
    abstract val vfsApplication: Vfs

    /** A [VfsFile] used for accessing data based on the **resources** directory. */
    abstract val resourcesVfs: VfsFile

    /** A [VfsFile] used for accessing data from the web or data urls. */
    abstract val urlVfs: VfsFile

    /** A [VfsFile] used for accessing data based on the application working directory. */
    abstract val applicationVfs: VfsFile

    /**
     * A [KeyValueStorage] used for storing and reading simple key-value data based on the
     * **storage** directory.
     */
    abstract val kvStorage: KeyValueStorage

    /** The [Platform] this context is running on. */
    abstract val platform: Platform

    /** Clipboard instance for reading and copying to a clipboard. */
    abstract val clipboard: Clipboard

    internal abstract fun start(build: (app: Context) -> ContextListener)

    /** Closes and destroys this context. */
    abstract fun close()

    internal open fun destroy() {
        EngineStats.clear()
    }

    protected fun calcFrameTimes(time: Duration) {
        dt = time - lastFrame
        available = counterTimePerFrame - dt
        lastFrame = time
    }

    /**
     * Creates a new update callback is invoked on every frame.
     *
     * @return a lambda that can be invoked to remove the callback
     */
    open fun onUpdate(action: (dt: Duration) -> Unit): RemoveContextCallback {
        updateCalls += action
        return {
            check(updateCalls.contains(action)) {
                "the 'onUpdate' action has already been removed!"
            }
            updateCalls -= action
        }
    }

    /**
     * Creates a new post-update callback that is invoked after the _render_ method is finished.
     *
     * @return a lambda that can be invoked to remove the callback
     */
    open fun onPostUpdate(action: (dt: Duration) -> Unit): RemoveContextCallback {
        postUpdateCalls += action
        return {
            check(postUpdateCalls.contains(action)) {
                "the 'onPostUpdate' action has already been removed!"
            }
            postUpdateCalls -= action
        }
    }

    /**
     * Creates a new _resize_ callback that is invoked whenever the context is resized.
     *
     * @return a lambda that can be invoked to remove the callback
     */
    open fun onResize(action: (width: Int, height: Int) -> Unit): RemoveContextCallback {
        resizeCalls += action
        return {
            check(resizeCalls.contains(action)) {
                "the 'onResize' action has already been removed!"
            }
            resizeCalls -= action
        }
    }

    /** Invoked when the window gains (`true`) or loses (`false`) input focus. */
    open fun onFocusChanged(action: (focused: Boolean) -> Unit): RemoveContextCallback =
        addCallback(focusChangedCalls, action, "onFocusChanged")

    /** Invoked when the window is minimized (`true`) or restored (`false`). */
    open fun onMinimizedChanged(action: (minimized: Boolean) -> Unit): RemoveContextCallback =
        addCallback(minimizedChangedCalls, action, "onMinimizedChanged")

    /** Invoked when the content scale of the window changes, such as moving to another monitor. */
    open fun onContentScaleChanged(action: (scale: Float) -> Unit): RemoveContextCallback =
        addCallback(contentScaleChangedCalls, action, "onContentScaleChanged")

    /** Invoked when the [WindowMode] changes, including by the user, such as leaving fullscreen. */
    open fun onWindowModeChanged(action: (mode: WindowMode) -> Unit): RemoveContextCallback =
        addCallback(windowModeChangedCalls, action, "onWindowModeChanged")

    /** Invoked when a monitor is connected or disconnected. */
    open fun onMonitorsChanged(action: (monitors: List<Monitor>) -> Unit): RemoveContextCallback =
        addCallback(monitorsChangedCalls, action, "onMonitorsChanged")

    /**
     * Invoked when the user tries to close the window. Return `false` to keep it open, such as to
     * ask to save first. See [WindowCapabilities.closeRequests].
     */
    open fun onCloseRequested(action: () -> Boolean): RemoveContextCallback =
        addCallback(closeRequestedCalls, action, "onCloseRequested")

    /** @return `true` if every [onCloseRequested] callback allows closing. */
    protected fun allowClose(): Boolean {
        var allow = true
        closeRequestedCalls.fastForEach { if (!it()) allow = false }
        return allow
    }

    private fun <T> addCallback(calls: MutableList<T>, action: T, name: String): RemoveContextCallback {
        calls += action
        return {
            check(calls.contains(action)) { "the '$name' action has already been removed!" }
            calls -= action
        }
    }

    /**
     * Creates a new _release_ callback that is invoked when the context is being destroyed.
     *
     * @return a lambda that can be invoked to remove the callback
     */
    open fun onRelease(action: suspend () -> Unit): RemoveContextCallback {
        releaseCalls += action
        return {
            check(releaseCalls.contains(action)) {
                "the 'onRelease' action has already been removed!"
            }
            releaseCalls -= action
        }
    }

    /**
     * Creates a new _postRunnable_ that is invoked one time after the next frame.
     *
     * @return a lambda that can be invoked to remove the callback
     */
    open fun postRunnable(action: () -> Unit): RemoveContextCallback {
        postRunnableCalls += action
        return {
            check(postRunnableCalls.contains(action)) {
                "the 'postRunnable' action has already been removed!"
            }
            postRunnableCalls -= action
        }
    }

    companion object
}

/** A lambda that can be invoked to remove a callback from a [Context]. */
typealias RemoveContextCallback = () -> Unit
