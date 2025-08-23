package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.SystemCursor
import com.littlekt.graphics.webgpu.*
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.UIEvent

/**
 * @author Colton Daily
 * @date 4/18/2024
 */
class WebGPUGraphics(val canvas: HTMLCanvasElement, gpuAdapter: GPUAdapter, gpuDevice: GPUDevice) : Graphics {

    internal var _width: Int = 0
    internal var _height: Int = 0

    private val canvasContext =
        canvas.getContext("webgpu") as? GPUCanvasContext
            ?: error("WebGPU context required")

    init {
        // suppress context menu
        canvas.oncontextmenu = Event::preventDefault
        canvas.onscroll = Event::preventDefault

        _width = canvas.clientWidth
        _height = canvas.clientHeight
    }

    override val width: Int
        get() = _width

    override val height: Int
        get() = _height

    override val backBufferWidth: Int
        get() = width

    override val backBufferHeight: Int
        get() = height

    override val surface: Surface =
        Surface(navigator.gpu, canvasContext)

    override var adapter: Adapter = Adapter(gpuAdapter)

    override var device: Device = Device(gpuDevice)

    override val preferredFormat: TextureFormat by lazy { surface.getPreferredFormat(adapter) }

    override val surfaceCapabilities: SurfaceCapabilities by lazy {
        surface.getCapabilities(adapter)
    }

    override fun configureSurface(
        usage: TextureUsage,
        format: TextureFormat,
        presentMode: PresentMode,
        alphaMode: AlphaMode
    ) {
        surface.configure(
            SurfaceConfiguration(device, usage, format, presentMode, alphaMode, width, height)
        )
    }

    override fun supportsExtension(extension: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun setCursor(cursor: Cursor) {
        canvas.style.cursor = cursor.cssCursorProperty
    }

    override fun setCursor(cursor: SystemCursor) {
        canvas.style.cursor =
            when (cursor) {
                SystemCursor.ARROW -> "default"
                SystemCursor.I_BEAM -> "text"
                SystemCursor.CROSSHAIR -> "crosshair"
                SystemCursor.HAND -> "pointer"
                SystemCursor.HORIZONTAL_RESIZE -> "ew-resize"
                SystemCursor.VERTICAL_RESIZE -> "ns-resize"
            }
    }
}