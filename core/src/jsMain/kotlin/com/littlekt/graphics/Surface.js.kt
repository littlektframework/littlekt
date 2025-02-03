package com.littlekt.graphics

import io.ygdrasil.webgpu.CanvasSurface
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceTexture
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.getCanvasSurface
import org.w3c.dom.HTMLCanvasElement

actual class Surface(private val handler: CanvasSurface) : AutoCloseable {
    actual val width: UInt
        get() = handler.width
    actual val height: UInt
        get() = handler.height

    actual val preferredCanvasFormat: TextureFormat?
        get() = handler.preferredCanvasFormat

    // @see https://gpuweb.github.io/gpuweb/#canvas-configuration
    actual val supportedFormats: Set<TextureFormat> =
        setOf(TextureFormat.BGRA8Unorm, TextureFormat.RGBA8Unorm, TextureFormat.RGBA16Float)
    actual val supportedAlphaMode: Set<CompositeAlphaMode> =
        setOf(CompositeAlphaMode.Opaque, CompositeAlphaMode.Premultiplied)

    actual fun getCurrentTexture(): SurfaceTexture {
        return handler.getCurrentTexture()
    }

    actual fun present() {
        handler.present()
    }

    actual fun configure(surfaceConfiguration: SurfaceConfiguration) {
        handler.configure(surfaceConfiguration)
    }

    actual override fun close() {
        handler.close()
    }
}

fun HTMLCanvasElement.getSurface() = getCanvasSurface()
    ?.let(::Surface)
