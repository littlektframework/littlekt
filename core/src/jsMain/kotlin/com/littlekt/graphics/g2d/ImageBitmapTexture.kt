package com.littlekt.graphics.g2d

import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.Texture.Companion.nextId
import com.littlekt.graphics.webgpu.*
import org.w3c.dom.ImageBitmap

/**
 * @author Colton Daily
 * @date 1/20/2025
 */
class ImageBitmapTexture(
    val device: Device,
    preferredFormat: TextureFormat,
    val data: ImageBitmap,
    mips: Int = Texture.calculateNumMips(data.width, data.height),
    samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
) : Texture {
    init {
        check(mips >= 1) { "Mips must be >= 1!" }
    }

    /**
     * The [Extent3D] size of the texture. Uses [Pixmap.width], [Pixmap.height] and a depth of `1`.
     */
    override val size: Extent3D = Extent3D(data.width, data.height, 1)
    override var id: Int = nextId()
        private set

    override var textureDescriptor: TextureDescriptor =
        TextureDescriptor(
            size,
            mips,
            1,
            TextureDimension.D2,
            preferredFormat,
            TextureUsage.TEXTURE or TextureUsage.COPY_DST or TextureUsage.RENDER_ATTACHMENT,
        )
        set(value) {
            field = value
            val textureToDestroy = gpuTexture
            val viewToDestroy = view
            viewToDestroy.release()
            textureToDestroy.release()

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
            view.release()
            view = gpuTexture.createView(value)
        }

    override var view: TextureView = gpuTexture.createView(textureViewDescriptor)
        private set(value) {
            field = value
            // we need to change the id due to the texture changing, mainly because it is used for
            // caching in other classes
            id = nextId()
        }

    override var samplerDescriptor: SamplerDescriptor = samplerDescriptor
        set(value) {
            field = value
            sampler.release()
            sampler = device.createSampler(value)
        }

    override var sampler: Sampler = device.createSampler(this.samplerDescriptor)
        private set

    init {
        writeDataToBuffer()
        if (mips > 1) {
            generateMipMaps(device)
        }
    }

    override fun writeDataToBuffer() {
        device.queue.copyExternalImageToTexture(data, TextureCopyView(gpuTexture), size)
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
