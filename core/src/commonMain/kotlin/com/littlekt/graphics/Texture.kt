package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.*
import kotlin.math.log2
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
     * The current [TextureState] of this Texture. This should normally be [TextureState.LOADED]
     * unless using an asynchronous texture loading.
     */
    val state: TextureState
        get() = TextureState.LOADED

    /**
     * The [Extent3D] size of the texture. Usually, the width & height of the image with a depth of
     * `1`.
     */
    val size: Extent3D

    /** The width of the texture. */
    val width: Int
        get() = size.width

    /** The height of the texture. */
    val height: Int
        get() = size.height

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

    /**
     * Generate mipmaps for this texture. This is usually called in the `init` of a texture but may
     * be need to be manually called for custom [Texture] types.
     */
    fun generateMipMaps(device: Device) {
        val generator = device.getOrCreateMipMapGenerator()
        generator.generateMips(this)
    }

    override fun release() {
        view.release()
        sampler.release()
        // destroy after any update/postUpdate calls to ensure we aren't in the middle of a pass!
        gpuTexture.release()
    }

    companion object {
        private var lastId by atomic(0)

        fun nextId() = lastId++

        /** @return the number of mips to generate based on the given sizes. */
        fun calculateNumMips(vararg sizes: Int): Int {
            val maxSize = sizes.maxOrNull() ?: return 1
            return 1 + log2(maxSize.toDouble()).toInt()
        }
    }
}
