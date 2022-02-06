package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.SystemCursor
import com.lehaine.littlekt.util.internal.jsObject
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Float32Array
import org.khronos.webgl.WebGLObject
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageData
import org.w3c.dom.events.Event
import org.w3c.dom.events.UIEvent

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class WebGLGraphics(val canvas: HTMLCanvasElement, engineStats: EngineStats) : Graphics {

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    override val gl: WebGL

    internal var _width: Int = 0
    internal var _height: Int = 0
    private var platform: Context.Platform = Context.Platform.WEBGL2

    private val ctxOptions: dynamic = jsObject { stencil = true }

    init {
        var webGlCtx = canvas.getContext("webgl2", ctxOptions)
        if (webGlCtx == null) {
            webGlCtx = canvas.getContext("experimental-webgl2", ctxOptions)
        }
        if (webGlCtx == null) {
            console.warn("WebGL2 not available. Attempting to fallback to WebGL.")
            webGlCtx = canvas.getContext("webgl", ctxOptions)
            platform = Context.Platform.WEBGL
        }

        if (webGlCtx != null) {
            gl = WebGL(webGlCtx as WebGL2RenderingContext, engineStats)
            gl.glVersion = GLVersion(platform, if (platform == Context.Platform.WEBGL2) "3.0" else "2.0")
        } else {
            js("alert(\"Unable to initialize WebGL or WebGL2 context. Your browser may not support it.\")")
            throw RuntimeException("WebGL2 context required")
        }
        webGlCtx.getExtension("OES_standard_derivatives")
        if (platform == Context.Platform.WEBGL) {
            webGlCtx.getExtension("OES_vertex_array_object")
        }
        // suppress context menu
        canvas.oncontextmenu = Event::preventDefault

        _width = canvas.clientWidth
        _height = canvas.clientHeight
    }

    override val width: Int
        get() = _width

    override val height: Int
        get() = _height

    override fun supportsExtension(extension: String): Boolean {
        return gl.gl.getExtension("extension") != null
    }

    override fun setCursor(cursor: Cursor) {
        canvas.style.cursor = cursor.cssCursorProperty
    }

    override fun setCursor(cursor: SystemCursor) {
        canvas.style.cursor = when (cursor) {
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

abstract external class WebGL2RenderingContext : WebGLRenderingContext {
    fun bufferData(target: Int, srcData: ArrayBufferView, usage: Int, srcOffset: Int, length: Int)
    fun clearBufferfv(buffer: Int, drawBuffer: Int, values: Float32Array)
    fun drawBuffers(buffers: IntArray)
    fun drawElementsInstanced(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int)
    fun readBuffer(src: Int)
    fun renderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int)
    fun texImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        format: Int,
        type: Int,
        srcData: ArrayBufferView?
    )

    fun texImage3D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        depth: Int,
        border: Int,
        format: Int,
        type: Int,
        source: HTMLImageElement?
    )

    fun texSubImage3D(
        target: Int,
        level: Int,
        xoffset: Int,
        yoffset: Int,
        zoffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        type: Int,
        pixels: ImageData?
    )

    fun texStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    fun texStorage3D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int, depth: Int)
    fun vertexAttribDivisor(index: Int, divisor: Int)
    fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)
    fun createVertexArray(): WebGLVertexArrayObject
    fun bindVertexArray(vao: WebGLVertexArrayObject?)
    fun deleteVertexArray(vao: WebGLVertexArrayObject?)

    companion object {
        val COLOR: Int
        val DEPTH: Int
        val STENCIL: Int
        val DEPTH_STENCIL: Int

        val DEPTH_COMPONENT24: Int
        val TEXTURE_3D: Int
        val TEXTURE_WRAP_R: Int
        val TEXTURE_COMPARE_MODE: Int
        val COMPARE_REF_TO_TEXTURE: Int
        val TEXTURE_COMPARE_FUNC: Int

        val RED: Int
        val RG: Int

        val R8: Int
        val RG8: Int
        val RGB8: Int
        val RGBA8: Int

        val R8UI: Int
        val RG8UI: Int
        val RGB8UI: Int
        val RGBA8UI: Int

        val R16F: Int
        val RG16F: Int
        val RGB16F: Int
        val RGBA16F: Int
    }
}

abstract external class WebGLVertexArrayObject : WebGLObject
