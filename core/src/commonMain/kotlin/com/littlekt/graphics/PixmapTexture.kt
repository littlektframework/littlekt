package com.littlekt.graphics

import com.littlekt.graphics.webgpu.*
import com.littlekt.util.UniqueId

/**
 * A [Texture] that uses a [Pixmap] as the underlying data.
 *
 * @param device the device for underlying GPU buffers creation
 * @param preferredFormat the preferred [TextureFormat]
 * @param pixmap the underlying texture data
 * @param mips number of mip map levels to generate. Set this value to `1` to not generate any
 *   additional levels. See [Texture.calculateNumMips] to calculate levels based on size. Must
 *   be >= 1.
 * @param samplerDescriptor optional [SamplerDescriptor] to pass in when building initial texture.
 * @author Colton Daily
 * @date 5/5/2024
 */
class PixmapTexture(
    val device: Device,
    preferredFormat: TextureFormat,
    val pixmap: Pixmap,
    mips: Int = Texture.calculateNumMips(pixmap.width, pixmap.height),
    samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
) : Texture {
    init {
        check(mips >= 1) { "Mips must be >= 1!" }
    }

    /**
     * The [Extent3D] size of the texture. Uses [Pixmap.width], [Pixmap.height] and a depth of `1`.
     */
    override val size: Extent3D = Extent3D(pixmap.width, pixmap.height, 1)
    override var id: Int = UniqueId.next<Texture>()
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
            id = UniqueId.next<Texture>()
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
        device.queue.writeTexture(
            pixmap.pixels,
            TextureCopyView(gpuTexture),
            TextureDataLayout(textureDescriptor.format.bytes * pixmap.width, pixmap.height),
            size,
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
