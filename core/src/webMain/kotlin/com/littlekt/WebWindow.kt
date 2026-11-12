package com.littlekt

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * A [Window] backed by the canvas and page. The canvas size follows the page, so only
 * [WindowMode.Windowed] and [WindowMode.Borderless] are supported. [WindowMode.Borderless] uses the
 * browser's fullscreen API and must be requested from a user input event.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class WebWindow internal constructor(private val canvas: HTMLCanvasElement) : Window {
    override val capabilities: WindowCapabilities =
        WindowCapabilities(
            resize = false,
            move = false,
            minimize = false,
            borderless = document.fullscreenEnabled,
            fullscreen = false,
            monitorSelection = false,
            closeRequests = false,
        )

    override val mode: WindowMode
        get() =
            if (document.fullscreenElement != null) WindowMode.Borderless(screenMonitor())
            else WindowMode.Windowed

    override fun setMode(mode: WindowMode): Boolean =
        when (mode) {
            WindowMode.Windowed -> {
                if (document.fullscreenElement != null) document.exitFullscreen()
                true
            }
            is WindowMode.Borderless -> {
                if (!capabilities.borderless) {
                    false
                } else {
                    if (document.fullscreenElement == null) canvas.requestFullscreen()
                    true
                }
            }
            is WindowMode.Fullscreen -> false
        }

    override var title: String
        get() = document.title
        set(value) {
            document.title = value
        }

    override val x: Int
        get() = 0

    override val y: Int
        get() = 0

    override val width: Int
        get() = canvas.clientWidth

    override val height: Int
        get() = canvas.clientHeight

    override val contentScale: Float
        get() = window.devicePixelRatio.toFloat()

    override val focused: Boolean
        get() = document.hasFocus()

    override var minimized: Boolean = false
        internal set

    override val maximized: Boolean
        get() = false

    override val monitors: List<Monitor>
        get() = listOf(screenMonitor())

    override val primaryMonitor: Monitor
        get() = screenMonitor()

    override val currentMonitor: Monitor
        get() = screenMonitor()

    override fun setSize(width: Int, height: Int) = Unit

    override fun setPosition(x: Int, y: Int) = Unit

    override fun setSizeLimits(minWidth: Int?, minHeight: Int?, maxWidth: Int?, maxHeight: Int?) =
        Unit

    override fun minimize() = Unit

    override fun maximize() = Unit

    override fun restore() = Unit

    override fun focus() {
        canvas.focus()
    }

    private fun screenMonitor(): Monitor {
        val mode = VideoMode(window.screen.width, window.screen.height, 0)
        return Monitor(0L, "Screen", 0, 0, contentScale, mode, listOf(mode))
    }
}
