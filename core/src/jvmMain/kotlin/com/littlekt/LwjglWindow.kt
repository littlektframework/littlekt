package com.littlekt

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/**
 * A [Window] backed by a GLFW window.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class LwjglWindow internal constructor(private val context: LwjglContext) : Window {
    private val handle: Long
        get() = context.windowHandle

    private val isWayland: Boolean by lazy { GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND }

    override val capabilities: WindowCapabilities by lazy {
        WindowCapabilities(
            resize = context.configuration.resizeable,
            move = !isWayland,
            minimize = true,
            borderless = true,
            fullscreen = true,
            monitorSelection = true,
            closeRequests = true,
        )
    }

    override var mode: WindowMode = WindowMode.Windowed
        private set

    private var windowedX = 0
    private var windowedY = 0
    private var windowedWidth = context.configuration.width
    private var windowedHeight = context.configuration.height

    private var _monitors: List<Monitor>? = null

    override var title: String = context.configuration.title
        set(value) {
            field = value
            if (handle != MemoryUtil.NULL) GLFW.glfwSetWindowTitle(handle, value)
        }

    override val x: Int
        get() = windowPos { x, _ -> x }

    override val y: Int
        get() = windowPos { _, y -> y }

    override val width: Int
        get() = context.graphics._logicalWidth

    override val height: Int
        get() = context.graphics._logicalHeight

    override val contentScale: Float
        get() =
            MemoryStack.stackPush().use { stack ->
                val sx = stack.mallocFloat(1)
                val sy = stack.mallocFloat(1)
                GLFW.glfwGetWindowContentScale(handle, sx, sy)
                sx[0]
            }

    override val focused: Boolean
        get() = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE

    override val minimized: Boolean
        get() = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE

    override val maximized: Boolean
        get() = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE

    override val monitors: List<Monitor>
        get() = _monitors ?: queryMonitors().also { _monitors = it }

    override val primaryMonitor: Monitor?
        get() {
            val primary = GLFW.glfwGetPrimaryMonitor()
            return monitors.firstOrNull { it.id == primary }
        }

    override val currentMonitor: Monitor?
        get() {
            when (val mode = mode) {
                is WindowMode.Borderless -> return mode.monitor
                is WindowMode.Fullscreen -> return mode.monitor
                WindowMode.Windowed -> Unit
            }
            val wx = x
            val wy = y
            val ww = width
            val wh = height
            var best: Monitor? = null
            var bestArea = 0
            monitors.forEach { monitor ->
                val mx = monitor.x
                val my = monitor.y
                val overlapW = minOf(wx + ww, mx + monitor.currentMode.width) - maxOf(wx, mx)
                val overlapH = minOf(wy + wh, my + monitor.currentMode.height) - maxOf(wy, my)
                val area = if (overlapW > 0 && overlapH > 0) overlapW * overlapH else 0
                if (area > bestArea) {
                    bestArea = area
                    best = monitor
                }
            }
            return best ?: primaryMonitor
        }

    override fun setMode(mode: WindowMode): Boolean {
        if (mode == this.mode) return true
        when (mode) {
            WindowMode.Windowed -> {
                GLFW.glfwSetWindowMonitor(
                    handle,
                    MemoryUtil.NULL,
                    windowedX,
                    windowedY,
                    windowedWidth,
                    windowedHeight,
                    GLFW.GLFW_DONT_CARE,
                )
                GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE)
            }
            is WindowMode.Borderless -> {
                if (!isConnected(mode.monitor)) return false
                saveWindowedBounds()
                val current = queryMonitor(mode.monitor.id)?.currentMode ?: mode.monitor.currentMode
                if (GLFW.glfwGetWindowMonitor(handle) != MemoryUtil.NULL) {
                    GLFW.glfwSetWindowMonitor(
                        handle,
                        MemoryUtil.NULL,
                        windowedX,
                        windowedY,
                        windowedWidth,
                        windowedHeight,
                        GLFW.GLFW_DONT_CARE,
                    )
                }
                GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE)
                GLFW.glfwSetWindowMonitor(
                    handle,
                    MemoryUtil.NULL,
                    mode.monitor.x,
                    mode.monitor.y,
                    current.width,
                    current.height,
                    GLFW.GLFW_DONT_CARE,
                )
            }
            is WindowMode.Fullscreen -> {
                if (!isConnected(mode.monitor) || mode.videoMode !in mode.monitor.modes) {
                    return false
                }
                saveWindowedBounds()
                GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE)
                GLFW.glfwSetWindowMonitor(
                    handle,
                    mode.monitor.id,
                    0,
                    0,
                    mode.videoMode.width,
                    mode.videoMode.height,
                    mode.videoMode.refreshRate,
                )
            }
        }
        this.mode = mode
        context.dispatchWindowModeChanged(mode)
        return true
    }

    override fun setSize(width: Int, height: Int) {
        if (mode == WindowMode.Windowed) GLFW.glfwSetWindowSize(handle, width, height)
        windowedWidth = width
        windowedHeight = height
    }

    override fun setPosition(x: Int, y: Int) {
        if (mode == WindowMode.Windowed && capabilities.move) GLFW.glfwSetWindowPos(handle, x, y)
        windowedX = x
        windowedY = y
    }

    override fun setSizeLimits(minWidth: Int?, minHeight: Int?, maxWidth: Int?, maxHeight: Int?) {
        GLFW.glfwSetWindowSizeLimits(
            handle,
            minWidth ?: GLFW.GLFW_DONT_CARE,
            minHeight ?: GLFW.GLFW_DONT_CARE,
            maxWidth ?: GLFW.GLFW_DONT_CARE,
            maxHeight ?: GLFW.GLFW_DONT_CARE,
        )
    }

    override fun minimize() = GLFW.glfwIconifyWindow(handle)

    override fun maximize() = GLFW.glfwMaximizeWindow(handle)

    override fun restore() = GLFW.glfwRestoreWindow(handle)

    override fun focus() = GLFW.glfwFocusWindow(handle)

    internal fun onMonitorsChanged(): List<Monitor> {
        val monitors = queryMonitors()
        _monitors = monitors
        val modeMonitor =
            when (val mode = mode) {
                is WindowMode.Borderless -> mode.monitor
                is WindowMode.Fullscreen -> mode.monitor
                WindowMode.Windowed -> null
            }
        if (modeMonitor != null && monitors.none { it.id == modeMonitor.id }) {
            setMode(WindowMode.Windowed)
        }
        return monitors
    }

    internal fun saveWindowedBounds() {
        if (mode != WindowMode.Windowed) return
        windowPos { x, y ->
            windowedX = x
            windowedY = y
        }
        windowedWidth = width
        windowedHeight = height
    }

    private fun isConnected(monitor: Monitor): Boolean = queryMonitor(monitor.id) != null

    private fun queryMonitor(id: Long): Monitor? = queryMonitors().firstOrNull { it.id == id }

    private fun queryMonitors(): List<Monitor> {
        val handles = GLFW.glfwGetMonitors() ?: return emptyList()
        return List(handles.limit()) { i -> createMonitor(handles[i]) }
    }

    private fun createMonitor(monitor: Long): Monitor =
        MemoryStack.stackPush().use { stack ->
            val px = stack.mallocInt(1)
            val py = stack.mallocInt(1)
            GLFW.glfwGetMonitorPos(monitor, px, py)
            val sx = stack.mallocFloat(1)
            val sy = stack.mallocFloat(1)
            GLFW.glfwGetMonitorContentScale(monitor, sx, sy)
            val currentMode: GLFWVidMode? = GLFW.glfwGetVideoMode(monitor)
            val current =
                if (currentMode != null) {
                    VideoMode(currentMode.width(), currentMode.height(), currentMode.refreshRate())
                } else {
                    VideoMode(0, 0, 0)
                }
            val videoModes: GLFWVidMode.Buffer? = GLFW.glfwGetVideoModes(monitor)
            val modes =
                if (videoModes != null) {
                    List(videoModes.limit()) { i ->
                            val mode = videoModes[i]
                            VideoMode(mode.width(), mode.height(), mode.refreshRate())
                        }
                        .distinct()
                } else {
                    listOf(current)
                }
            Monitor(
                id = monitor,
                name = GLFW.glfwGetMonitorName(monitor) ?: "Monitor",
                x = px[0],
                y = py[0],
                contentScale = sx[0],
                currentMode = current,
                modes = modes,
            )
        }

    private inline fun <T> windowPos(block: (x: Int, y: Int) -> T): T =
        MemoryStack.stackPush().use { stack ->
            val px = stack.mallocInt(1)
            val py = stack.mallocInt(1)
            GLFW.glfwGetWindowPos(handle, px, py)
            block(px[0], py[0])
        }
}
