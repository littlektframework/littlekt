package com.littlekt.graphics

import com.littlekt.graphics.webgpu.*
import com.littlekt.util.UniqueId

/**
 * A [Texture] that doesn't contain any underlying raw image data, but instead, is intended to be
 * used in a render pass as an output, such as a color, depth, multisampled, or HDR target. Use
 * [com.littlekt.renderTarget] to create one the size of the window that resizes & releases with it.
 *
 * @param device the device for underlying GPU buffers creation
 * @param preferredFormat the preferred [TextureFormat]
 * @param width size of the textures width
 * @param height size of the textures height
 * @param samplerDescriptor optional [SamplerDescriptor] to use when loading initial texture.
 * @param sampleCount the number of samples per pixel for multisampling.
 * @param usage the [TextureUsage] of the texture.
 * @author Colton Daily
 * @date 5/5/2024
 */
class EmptyTexture(
    val device: Device,
    preferredFormat: TextureFormat,
    width: Int,
    height: Int,
    samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
    sampleCount: Int = 1,
    usage: TextureUsage = TextureUsage.TEXTURE or TextureUsage.RENDER_ATTACHMENT,
) : Texture {
    /**
     * The [Extent3D] size of the texture. Uses the initial width & height from the constructor and
     * a depth of `1`.
     */
    override var size: Extent3D = Extent3D(width, height, 1)
        private set

    override var id: Int = UniqueId.next<Texture>()
        private set

    /** The current [WebGPUTexture]. Changing the value of this field WILL change [id]. */
    override var textureDescriptor: TextureDescriptor =
        TextureDescriptor(size, sampleCount = sampleCount, format = preferredFormat, usage = usage)
        set(value) {
            field = value
            val textureToDestroy = gpuTexture
            textureToDestroy.release()
            gpuTexture = device.createTexture(field)
        }

    /** The current [WebGPUTexture]. Changing the value of this field WILL change [id]. */
    override var gpuTexture: WebGPUTexture = device.createTexture(textureDescriptor)
        set(value) {
            field = value
            view.release()
            view = field.createView(textureViewDescriptor)
        }

    /** The current [TextureViewDescriptor]. Changing the value of this field WILL change [id]. */
    override var textureViewDescriptor: TextureViewDescriptor? = null
        set(value) {
            field = value
            view.release()
            view = gpuTexture.createView(value)
        }

    /** The current [TextureView]. Changing the value of this field WILL change [id]. */
    override var view: TextureView = gpuTexture.createView(textureViewDescriptor)
        set(value) {
            field = value

            // we need to change the id due to the texture changing, mainly because it is used for
            // caching in other classes
            id = UniqueId.next<Texture>()
        }

    /** The [TextureFormat] of this texture. */
    val format: TextureFormat
        get() = textureDescriptor.format

    override var samplerDescriptor: SamplerDescriptor = samplerDescriptor
        set(value) {
            field = value
            sampler.release()
            sampler = device.createSampler(value)
        }

    override var sampler: Sampler = device.createSampler(this.samplerDescriptor)
        private set

    override fun writeDataToBuffer() {
        // do nothing because we are rendering to this texture, we have nothing to upload
    }

    /**
     * Resizes this texture by copying the [TextureDescriptor] and updating the [size] component, as
     * well as updating [size] field itself. This will update [gpuTexture] & [view]. This will
     * change this texture's [id]. This is essentially the same as destroying this texture and
     * creating a whole new one. Does nothing if the size is the same.
     */
    fun resize(width: Int, height: Int) {
        if (size.width == width && size.height == height) return
        size = Extent3D(width, height, 1)
        textureDescriptor = textureDescriptor.copy(size = size)
    }

    /** @return a depth attachment of this texture that clears the depth to `1`. */
    fun depthAttachment(): RenderPassDepthStencilAttachmentDescriptor {
        val stencil = textureDescriptor.format == TextureFormat.DEPTH24_PLUS_STENCIL8
        return RenderPassDepthStencilAttachmentDescriptor(
            view = view,
            stencilLoadOp = if (stencil) LoadOp.CLEAR else null,
            stencilStoreOp = if (stencil) StoreOp.STORE else null,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Texture

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}
