package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.Surface
import com.littlekt.graphics.SystemCursor
import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.requestAdapter
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.TextureUsage
import io.ygdrasil.webgpu.CompositeAlphaMode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
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

    override lateinit var adapter: Adapter
    override lateinit var device: Device
    override val surface: Surface = canvas.getSurface() ?: error("fail to get context")
    override val preferredFormat: TextureFormat = surface.preferredCanvasFormat  ?: error("fail to get preferredCanvasFormat")

    init {
        // suppress context menu
        canvas.oncontextmenu = Event::preventDefault
        canvas.onscroll = Event::preventDefault

        _width = canvas.clientWidth
        _height = canvas.clientHeight

        GlobalScope.async {
            adapter = requestAdapter() ?: error("No appropriate Adapter found.")
            device = adapter.requestDevice() ?: error("No appropriate Device found.")
        }.onAwait
    }

    override val width: Int
        get() = _width

    override val height: Int
        get() = _height

    override val backBufferWidth: Int
        get() = width

    override val backBufferHeight: Int
        get() = height

    override fun configureSurface(
        usages: Set<TextureUsage>,
        format: TextureFormat,
        alphaMode: CompositeAlphaMode
    ) {
        surface.configure(
            CanvasConfiguration(device, format,usages, alphaMode = alphaMode)
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
