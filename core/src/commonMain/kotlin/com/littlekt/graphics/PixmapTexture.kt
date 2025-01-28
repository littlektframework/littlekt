package com.littlekt.graphics

import com.littlekt.graphics.Texture.Companion.nextId
import io.ygdrasil.webgpu.Texture as WebGPUTexture
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.ImageCopyTexture
import io.ygdrasil.webgpu.Sampler
import io.ygdrasil.webgpu.SamplerDescriptor
import io.ygdrasil.webgpu.Size3D
import io.ygdrasil.webgpu.TextureDataLayout
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.TextureDimension
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.TextureUsage
import io.ygdrasil.webgpu.TextureView
import io.ygdrasil.webgpu.TextureViewDescriptor

/**
 * A [Texture] that uses a [Pixmap] as the underlying data.
 *
 * @param device the device for underlying GPU buffers creation
 * @param preferredFormat the preferred [TextureFormat]
 * @param pixmap the underlying texture data
 * @author Colton Daily
 * @date 5/5/2024
 */
class PixmapTexture(val device: Device, preferredFormat: TextureFormat, val pixmap: Pixmap) :
    Texture {
    /**
     * The [Extent3D] size of the texture. Uses [Pixmap.width], [Pixmap.height] and a depth of `1`.
     */
    override val size: Size3D = Size3D(pixmap.width.toUInt(), pixmap.height.toUInt())
    override var id: Int = nextId()
        private set

    override var textureDescriptor: TextureDescriptor =
        TextureDescriptor(
            size,
            preferredFormat,
            setOf(TextureUsage.TextureBinding, TextureUsage.CopyDst)
        )
        set(value) {
            field = value
            val textureToDestroy = gpuTexture
            val viewToDestroy = view
            viewToDestroy.close()
            textureToDestroy.close()

            gpuTexture = device.createTexture(textureDescriptor)
        }

    override var gpuTexture: WebGPUTexture = device.createTexture(textureDescriptor)
        private set(value) {
            field = value
            view = field.createView(textureViewDescriptor)
            writeDataToBuffer()
        }

    override var textureViewDescriptor: TextureViewDescriptor? = null
        set(value) {
            field = value
            view.close()
            view = gpuTexture.createView(value)
        }

    override var view: TextureView = gpuTexture.createView(textureViewDescriptor)
        private set(value) {
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

    init {
        writeDataToBuffer()
    }

    override fun writeDataToBuffer() {
        device.queue.writeTexture(
            ImageCopyTexture(gpuTexture),
            pixmap.pixels.toArray(),
            TextureDataLayout(0uL, 4u * pixmap.width.toUInt(), pixmap.height.toUInt()),
            size
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
