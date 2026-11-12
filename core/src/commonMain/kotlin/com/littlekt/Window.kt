package com.littlekt

/**
 * The window, or canvas on the web, the application is drawn to. Use [capabilities] to check what
 * the current platform supports before changing anything. Changes are reported through the
 * [Context] callbacks, such as [Context.onResize], [Context.onFocusChanged] and
 * [Context.onWindowModeChanged].
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface Window {
    /** What this window supports on the current platform. */
    val capabilities: WindowCapabilities

    /** The current [WindowMode]. */
    val mode: WindowMode

    /**
     * Requests a new [WindowMode].
     *
     * On the web, [WindowMode.Borderless] has to be requested from a user input event, such as a
     * click, and the browser may still refuse it.
     *
     * @return `false` if the mode isn't supported on this platform
     */
    fun setMode(mode: WindowMode): Boolean

    /** The title of the window, or of the page on the web. */
    var title: String

    /** The x position of the window in screen coordinates. */
    val x: Int

    /** The y position of the window in screen coordinates. */
    val y: Int

    /** The logical width of the window. */
    val width: Int

    /** The logical height of the window. */
    val height: Int

    /** The ratio between the back buffer and logical size, such as `2` on a high DPI display. */
    val contentScale: Float

    /** `true` if the window has input focus. */
    val focused: Boolean

    /** `true` if the window is minimized, or the page is hidden on the web. */
    val minimized: Boolean

    /** `true` if the window is maximized. */
    val maximized: Boolean

    /** The connected monitors. */
    val monitors: List<Monitor>

    /** The primary monitor, if any. */
    val primaryMonitor: Monitor?

    /** The monitor the window is mostly on, if any. */
    val currentMonitor: Monitor?

    /** Resizes the window in logical units. Requires [WindowCapabilities.resize]. */
    fun setSize(width: Int, height: Int)

    /** Moves the window in screen coordinates. Requires [WindowCapabilities.move]. */
    fun setPosition(x: Int, y: Int)

    /**
     * Limits how far the window can be resized in logical units. Use `null` for no limit. Requires
     * [WindowCapabilities.resize].
     */
    fun setSizeLimits(minWidth: Int?, minHeight: Int?, maxWidth: Int?, maxHeight: Int?)

    /** Minimizes the window. Requires [WindowCapabilities.minimize]. */
    fun minimize()

    /** Maximizes the window. Requires [WindowCapabilities.resize]. */
    fun maximize()

    /** Restores the window from being minimized or maximized. */
    fun restore()

    /** Brings the window to the front and gives it input focus. */
    fun focus()
}

/**
 * How the window is shown.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
sealed interface WindowMode {
    /** A regular decorated window. */
    data object Windowed : WindowMode

    /** An undecorated window that covers [monitor] without changing its video mode. */
    data class Borderless(val monitor: Monitor) : WindowMode

    /** Exclusive fullscreen on [monitor] using [videoMode]. */
    data class Fullscreen(val monitor: Monitor, val videoMode: VideoMode) : WindowMode
}

/**
 * A connected display.
 *
 * @property id the platform id of the monitor, only valid while it's connected
 * @property name the name of the monitor
 * @property x the x position of the monitor in screen coordinates
 * @property y the y position of the monitor in screen coordinates
 * @property contentScale the content scale of the monitor, such as `2` on a high DPI display
 * @property currentMode the current video mode
 * @property modes every supported video mode
 * @author Colton Daily
 * @date 11/11/2026
 */
data class Monitor(
    val id: Long,
    val name: String,
    val x: Int,
    val y: Int,
    val contentScale: Float,
    val currentMode: VideoMode,
    val modes: List<VideoMode>,
)

/**
 * A video mode of a [Monitor].
 *
 * @property width the width in pixels
 * @property height the height in pixels
 * @property refreshRate the refresh rate in Hz, or `0` if unknown
 * @author Colton Daily
 * @date 11/11/2026
 */
data class VideoMode(val width: Int, val height: Int, val refreshRate: Int)

/**
 * What a [Window] supports on the current platform.
 *
 * @property resize the window can be resized, maximized and given size limits
 * @property move the window can be moved
 * @property minimize the window can be minimized
 * @property borderless [WindowMode.Borderless] is supported
 * @property fullscreen [WindowMode.Fullscreen] is supported
 * @property monitorSelection the monitor of [WindowMode.Borderless] & [WindowMode.Fullscreen] is
 *   used
 * @property closeRequests [Context.onCloseRequested] is called and can cancel closing
 * @author Colton Daily
 * @date 11/11/2026
 */
data class WindowCapabilities(
    val resize: Boolean,
    val move: Boolean,
    val minimize: Boolean,
    val borderless: Boolean,
    val fullscreen: Boolean,
    val monitorSelection: Boolean,
    val closeRequests: Boolean,
)
