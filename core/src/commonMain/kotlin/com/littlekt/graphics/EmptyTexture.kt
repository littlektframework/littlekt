package com.littlekt.graphics

import com.littlekt.graphics.Texture.Companion.nextId
import io.ygdrasil.wgpu.Device
import io.ygdrasil.wgpu.Sampler
import io.ygdrasil.wgpu.SamplerDescriptor
import io.ygdrasil.wgpu.Size3D
import io.ygdrasil.wgpu.TextureDescriptor
import io.ygdrasil.wgpu.TextureDimension
import io.ygdrasil.wgpu.TextureFormat
import io.ygdrasil.wgpu.TextureUsage
import io.ygdrasil.wgpu.TextureView
import io.ygdrasil.wgpu.TextureViewDescriptor

import io.ygdrasil.wgpu.Texture as WebGPUTexture

/**
 * A [Texture] that doesn't contain any underlying raw image data, but instead, is intended to be
 * used in a render pass as an output. By default, the [textureDescriptor] uses
 * `TextureUsage.TEXTURE or TextureUsage.RENDER_ATTACHMENT` as usage.
 *
 * @param device the device for underlying GPU buffers creation
 * @param preferredFormat the preferred [TextureFormat]
 * @author Colton Daily
 * @date 5/5/2024
 */
class EmptyTexture(val device: Device, preferredFormat: TextureFormat, width: Int, height: Int) :
    Texture {
    /**
     * The [Extent3D] size of the texture. Uses the initial width & height from the constructor and
     * a depth of `1`.
     */
    override var size: Size3D = Size3D(width, height, 1)
        private set

    override var id: Int = nextId()
        private set

    /** The current [WebGPUTexture]. Changing the value of this field WILL change [id]. */
    override var textureDescriptor: TextureDescriptor =
        TextureDescriptor(
            size,
            preferredFormat,
            setOf(TextureUsage.textureBinding, TextureUsage.renderAttachment)
        )
        set(value) {
            field = value
            val textureToDestroy = gpuTexture
            textureToDestroy.close()
            gpuTexture = device.createTexture(field)
        }

    /** The current [WebGPUTexture]. Changing the value of this field WILL change [id]. */
    override var gpuTexture: WebGPUTexture = device.createTexture(textureDescriptor)
        set(value) {
            field = value
            view.close()
            view = field.createView(textureViewDescriptor)
        }

    /** The current [TextureViewDescriptor]. Changing the value of this field WILL change [id]. */
    override var textureViewDescriptor: TextureViewDescriptor? = null
        set(value) {
            field = value
            view.close()
            view = gpuTexture.createView(value)
        }

    /** The current [TextureView]. Changing the value of this field WILL change [id]. */
    override var view: TextureView = gpuTexture.createView(textureViewDescriptor)
        set(value) {
            field = value

            // we need to change the id due to the texture changing, mainly because it is used for
            // caching in other classes
            id = nextId()
        }

    override var samplerDescriptor: SamplerDescriptor = SamplerDescriptor()
        set(value) {
            field = value
            sampler.close()
            sampler = device.createSampler(value)
        }

    override var sampler: Sampler = device.createSampler(samplerDescriptor)
        private set

    override fun writeDataToBuffer() {
        // do nothing because we are rendering to this texture, we have nothing to upload
    }

    /**
     * Resizes this texture by copying the [TextureDescriptor] and updating the [size] component, as
     * well as updating [size] field itself. This will update [gpuTexture] & [view]. This will
     * change this texture's [id]. This is essentially the same as destroying this texture and
     * creating a whole new one.
     */
    fun resize(width: Int, height: Int) {
        size = Size3D(width, height, 1)
        textureDescriptor = textureDescriptor.copy(size = size)
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
