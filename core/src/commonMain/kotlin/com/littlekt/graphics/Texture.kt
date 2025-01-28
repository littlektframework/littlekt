package com.littlekt.graphics

import com.littlekt.Releasable
import io.ygdrasil.webgpu.Sampler
import io.ygdrasil.webgpu.SamplerDescriptor
import io.ygdrasil.webgpu.Size3D
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.TextureView
import io.ygdrasil.webgpu.TextureViewDescriptor
import io.ygdrasil.webgpu.Texture as WebGPUTexture
import kotlinx.atomicfu.atomic

/**
 * Creates a [WebGPUTexture] and writes it to the [Device.queue]. Creates a [TextureView] and
 * [Sampler].
 *
 * @author Colton Daily
 * @date 4/9/2024
 */
interface Texture : Releasable {

    /**
     * The [Extent3D] size of the texture. Usually, the width & height of the image with a depth of
     * `1`.
     */
    val size: Size3D

    /** The width of the texture. */
    val width: Int
        get() = size.width.toInt()

    /** The height of the texture. */
    val height: Int
        get() = size.height.toInt()

    /** The id of the texture. */
    val id: Int

    /**
     * The [TextureDescriptor] used in [gpuTexture]. Updating this will recreate the [gpuTexture]
     * and [view]. Any bind group entries will need recreated!
     */
    var textureDescriptor: TextureDescriptor

    /** The underlying [WebGPUTexture]. Uses [textureDescriptor] in creation. */
    val gpuTexture: WebGPUTexture

    /**
     * The [TextureViewDescriptor] used [view]. Updating this will recreate the [TextureView]. Any
     * bind group entries will need recreated!
     */
    var textureViewDescriptor: TextureViewDescriptor?

    /** The underlying [TextureView]. Uses [textureViewDescriptor] in creation. */
    val view: TextureView

    /**
     * The [SamplerDescriptor] used in [sampler]. Updating this will recreate the sampler. Any bind
     * group entries will need recreated!
     */
    var samplerDescriptor: SamplerDescriptor

    /** The underlying [Sampler]. Uses [samplerDescriptor] in creation. */
    val sampler: Sampler

    /** Write this [Texture] to the GPU buffer. */
    fun writeDataToBuffer()

    override fun release() {
        view.close()
        sampler.close()
        // destroy after any update/postUpdate calls to ensure we aren't in the middle of a pass!
        gpuTexture.close()
    }

    companion object {
        private var lastId by atomic(0)

        fun nextId() = lastId++
    }
}
