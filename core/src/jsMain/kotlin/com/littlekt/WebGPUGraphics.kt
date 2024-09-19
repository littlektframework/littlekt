package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.SystemCursor
import com.littlekt.graphics.webgpu.Adapter
import com.littlekt.graphics.webgpu.AlphaMode
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.GPU
import com.littlekt.graphics.webgpu.GPUAdapter
import com.littlekt.graphics.webgpu.GPUCanvasContext
import com.littlekt.graphics.webgpu.GPUDevice
import com.littlekt.graphics.webgpu.PresentMode
import com.littlekt.graphics.webgpu.Surface
import com.littlekt.graphics.webgpu.SurfaceCapabilities
import com.littlekt.graphics.webgpu.SurfaceConfiguration
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.graphics.webgpu.TextureUsage
import com.littlekt.util.internal.jsObject
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.UIEvent

/**
 * @author Colton Daily
 * @date 4/18/2024
 */
class WebGPUGraphics(val canvas: HTMLCanvasElement) : Graphics {

    internal var _width: Int = 0
    internal var _height: Int = 0

    private val canvasContext =
        canvas.getContext("webgpu").unsafeCast<GPUCanvasContext?>()
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
        Surface(window.navigator.asDynamic().gpu.unsafeCast<GPU>(), canvasContext)

    override var adapter: Adapter = Adapter(jsObject().unsafeCast<GPUAdapter>())

    override var device: Device = Device(jsObject().unsafeCast<GPUDevice>())

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

external class TouchEvent : UIEvent {
    val altKey: Boolean
    val changedTouches: TouchList
    val ctrlKey: Boolean
    val metaKey: Boolean
    val shiftKey: Boolean
    val targetTouches: TouchList
    val touches: TouchList
}

external class TouchList {
    val length: Int

    fun item(index: Int): Touch
}

external class Touch {
    val identifier: Int
    val screenX: Double
    val screenY: Double
    val clientX: Double
    val clientY: Double
    val pageX: Double
    val pageY: Double
    val target: Element
    val radiusX: Double
    val radiusY: Double
    val rotationAngle: Double
    val force: Double
}
