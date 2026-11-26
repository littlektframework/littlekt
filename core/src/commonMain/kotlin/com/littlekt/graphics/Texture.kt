package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.*
import kotlin.math.log2
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

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

    /**
     * The id of the texture. This should be unique. Use `UniqueId.next<Texture>()` and not the
     * subclass to generate the id.
     */
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
        notifyInvalidated(id)
        view.release()
        sampler.release()
        // destroy after any update/postUpdate calls to ensure we aren't in the middle of a pass!
        gpuTexture.release()
    }

    companion object {
        private val invalidationLock = SynchronizedObject()
        private var invalidationListeners = emptyArray<TextureInvalidationListener>()

        /** @return the number of mips to generate based on the given sizes. */
        fun calculateNumMips(vararg sizes: Int): Int {
            val maxSize = sizes.maxOrNull() ?: return 1
            return 1 + log2(maxSize.toDouble()).toInt()
        }

        /**
         * Adds a [listener] that is called with the id of any texture that is released or has its
         * view recreated, so anything cached by that id, such as bind groups, can be evicted.
         */
        fun addInvalidationListener(listener: TextureInvalidationListener) {
            synchronized(invalidationLock) { invalidationListeners += listener }
        }

        /** Removes a [listener] added with [addInvalidationListener]. */
        fun removeInvalidationListener(listener: TextureInvalidationListener) {
            synchronized(invalidationLock) {
                val listeners = invalidationListeners
                val index = listeners.indexOf(listener)
                if (index < 0) return
                invalidationListeners =
                    Array(listeners.size - 1) { listeners[if (it < index) it else it + 1] }
            }
        }

        /**
         * Notifies every listener that the texture with [id] is no longer valid. Custom [Texture]
         * types call this when released or when their view is recreated. It may be called from
         * any thread.
         */
        fun notifyInvalidated(id: Int) {
            val listeners = synchronized(invalidationLock) { invalidationListeners }
            for (i in listeners.indices) listeners[i].onTextureInvalidated(id)
        }
    }
}

/**
 * Listens for textures that are released or recreated.
 *
 * @see Texture.addInvalidationListener
 * @author Colton Daily
 * @date 11/26/2026
 */
fun interface TextureInvalidationListener {
    /** Invoked with the [id] of a texture that is released or has its view recreated. */
    fun onTextureInvalidated(id: Int)
}
