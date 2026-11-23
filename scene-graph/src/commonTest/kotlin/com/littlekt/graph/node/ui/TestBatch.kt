package com.littlekt.graph.node.ui

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.BlendState
import com.littlekt.graphics.webgpu.Extent3D
import com.littlekt.graphics.webgpu.RenderPassEncoder
import com.littlekt.graphics.webgpu.Sampler
import com.littlekt.graphics.webgpu.SamplerDescriptor
import com.littlekt.graphics.webgpu.TextureDescriptor
import com.littlekt.graphics.webgpu.TextureView
import com.littlekt.graphics.webgpu.TextureViewDescriptor
import com.littlekt.graphics.webgpu.WebGPUTexture
import com.littlekt.math.Mat4
import com.littlekt.math.geom.Angle

internal class TestTexture(width: Int, height: Int) : Texture {
    override val size: Extent3D = Extent3D(width, height, 1)
    override val id: Int = 0
    override var textureDescriptor: TextureDescriptor
        get() = error("No GPU")
        set(_) = Unit

    override val gpuTexture: WebGPUTexture
        get() = error("No GPU")

    override var textureViewDescriptor: TextureViewDescriptor? = null
    override val view: TextureView
        get() = error("No GPU")

    override var samplerDescriptor: SamplerDescriptor
        get() = error("No GPU")
        set(_) = Unit

    override val sampler: Sampler
        get() = error("No GPU")

    override fun writeDataToBuffer() = Unit

    override fun release() = Unit
}

internal data class SliceDraw(
    val x: Float,
    val y: Float,
    val originX: Float,
    val originY: Float,
    val width: Float,
    val height: Float,
    val srcX: Int,
    val srcY: Int,
    val srcWidth: Int,
    val srcHeight: Int,
)

internal class TestBatch : Batch {
    val draws = mutableListOf<SliceDraw>()

    override var shader: Shader
        get() = error("No GPU")
        set(_) = Unit

    override val defaultShader: Shader
        get() = error("No GPU")

    override var transformMatrix: Mat4 = Mat4()
    override var viewProjection: Mat4 = Mat4()
    override val drawing: Boolean = true
    override var lastMeshIdx: Int = 0

    override fun begin(viewProjection: Mat4?) = Unit

    override fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        draw(
            slice,
            x,
            y,
            originX,
            originY,
            width,
            height,
            scaleX,
            scaleY,
            rotation,
            color,
            slice.x,
            slice.y,
            slice.width,
            slice.height,
            flipX,
            flipY,
        )
    }

    override fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        draws += SliceDraw(x, y, originX, originY, width, height, srcX, srcY, srcWidth, srcHeight)
    }

    override fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        color: Color,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        draws += SliceDraw(x, y, originX, originY, width, height, srcX, srcY, srcWidth, srcHeight)
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) = Unit

    override fun flush(renderPassEncoder: RenderPassEncoder, viewProjection: Mat4?) = Unit

    override fun end() = Unit

    override fun setBlendState(newBlendState: BlendState) = Unit

    override fun swapToPreviousBlendState() = Unit

    override fun useDefaultShader() = Unit

    override fun release() = Unit
}
