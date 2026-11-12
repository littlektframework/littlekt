package com.littlekt.examples.ui

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.VideoMode
import com.littlekt.WindowMode
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.graph.node.ui.*
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.graphics.webgpu.colorAttachment
import com.littlekt.renderFrame
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example of the low level window and display API: window modes, monitors, video modes, present
 * modes (vsync), a frame rate limit, window events and confirming before closing. A game or engine
 * would build its own settings menu and saving on top of these.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class DisplaySettingsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val caps = window.capabilities
        var monitorIndex = 0
        var videoModeIndex = 0
        var confirmClose = false
        val limits = listOf(null, 30, 60, 144)
        var limitIndex = 0
        lateinit var info: Label
        lateinit var events: Label
        lateinit var fps: Label

        fun monitor() = window.monitors.getOrNull(monitorIndex) ?: window.monitors.first()

        fun videoMode(): VideoMode {
            val modes = monitor().modes
            return modes.getOrNull(videoModeIndex) ?: monitor().currentMode
        }

        fun log(message: String) {
            events.text = message
        }

        val graph =
            sceneGraph(this, ExtendViewport(960, 540)) {
                    column(separation = 8, modifier = Modifier.fillParent(16f)) {
                        label("Display Settings")
                        info = label()
                        fps = label()
                        events = label("Events show up here")
                        row(separation = 8) {
                            button("Windowed") { onPressed += { window.setMode(WindowMode.Windowed) } }
                            button("Borderless") {
                                disabled = !caps.borderless
                                onPressed += { window.setMode(WindowMode.Borderless(monitor())) }
                            }
                            button("Fullscreen") {
                                disabled = !caps.fullscreen
                                onPressed += {
                                    window.setMode(WindowMode.Fullscreen(monitor(), videoMode()))
                                }
                            }
                        }
                        row(separation = 8) {
                            button("Next monitor") {
                                disabled = !caps.monitorSelection
                                onPressed += {
                                    monitorIndex = (monitorIndex + 1) % window.monitors.size
                                    videoModeIndex = monitor().modes.indexOf(monitor().currentMode)
                                }
                            }
                            button("Next video mode") {
                                disabled = !caps.fullscreen
                                onPressed += {
                                    videoModeIndex = (videoModeIndex + 1) % monitor().modes.size
                                }
                            }
                        }
                        row(separation = 8) {
                            button("Next present mode") {
                                onPressed += {
                                    val modes = graphics.surfaceCapabilities.presentModes
                                    val next =
                                        modes[(modes.indexOf(graphics.presentMode) + 1) % modes.size]
                                    graphics.configureSurface(presentMode = next)
                                }
                            }
                            button("Next frame limit") {
                                onPressed += {
                                    limitIndex = (limitIndex + 1) % limits.size
                                    frameRateLimit = limits[limitIndex]
                                }
                            }
                        }
                        row(separation = 8) {
                            button("Resize to 1280x720") {
                                disabled = !caps.resize
                                onPressed += { window.setSize(1280, 720) }
                            }
                            button("Minimize") {
                                disabled = !caps.minimize
                                onPressed += { window.minimize() }
                            }
                            button("Confirm close") {
                                disabled = !caps.closeRequests
                                toggleMode = true
                                onToggled += { confirmClose = it }
                            }
                        }
                        progressBar {
                            onUpdate += { dt -> value = (value + dt.seconds * 50f) % max }
                        }
                    }
                }
                .also { it.initialize() }

        videoModeIndex = monitor().modes.indexOf(monitor().currentMode)

        onFocusChanged { log("Focus changed: $it") }
        onMinimizedChanged { log("Minimized changed: $it") }
        onContentScaleChanged { log("Content scale changed: $it") }
        onWindowModeChanged { log("Window mode changed: ${it::class.simpleName}") }
        onMonitorsChanged { log("Monitors changed: ${it.size} connected") }
        onCloseRequested {
            if (confirmClose) {
                confirmClose = false
                log("Close requested! Close again to quit.")
                false
            } else {
                true
            }
        }

        onResize { width, height -> graph.resize(width, height) }

        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)
        onUpdate { dt ->
            val monitor = monitor()
            val mode = videoMode()
            info.text =
                "Window: ${window.width}x${window.height} at ${window.x},${window.y} " +
                    "scale ${window.contentScale} | mode ${window.mode::class.simpleName}\n" +
                    "Monitor ${monitorIndex + 1}/${window.monitors.size}: ${monitor.name} | " +
                    "video mode ${mode.width}x${mode.height}@${mode.refreshRate}\n" +
                    "Present mode ${graphics.presentMode} of ${graphics.surfaceCapabilities.presentModes} | " +
                    "frame limit ${frameRateLimit ?: "none"}"
            fps.text = "FPS: ${stats.fps.toInt()}"
            renderFrame("display settings") { frame, commandEncoder ->
                graph.update(dt)
                graph.render(
                    commandEncoder,
                    RenderPassDescriptor(colorAttachment(frame, clearColor), label = "UI pass"),
                )
            }
        }

        onRelease { graph.release() }
    }
}
