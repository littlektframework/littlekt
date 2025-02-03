package com.littlekt.graphics

import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceTexture
import io.ygdrasil.webgpu.TextureFormat

expect class Surface : AutoCloseable {

    val width: UInt
    val height: UInt

    val preferredCanvasFormat: TextureFormat?

    val supportedFormats: Set<TextureFormat>
    val supportedAlphaMode: Set<CompositeAlphaMode>

    fun getCurrentTexture(): SurfaceTexture

    fun present()

    fun configure(surfaceConfiguration: SurfaceConfiguration)

    override fun close()
}