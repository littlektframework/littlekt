package com.littlekt.graphics.g2d

import com.littlekt.async.VfsScope
import com.littlekt.graphics.LazyTexture
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.TextureState
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.UniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.ImageBitmap

/**
 * @author Colton Daily
 * @date 1/20/2025
 */
class LazyImageBitmapTexture(
    val device: Device,
    samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
) : LazyTexture {
    private lateinit var bitmap: ImageBitmap

    override var state: TextureState = TextureState.UNLOADED
        private set

    /**
     * Launches a new coroutine in the [VfsScope] that requires loading [Pixmap] as a return value.
     * This should be called only once as it will throw an error if called again.
     */
    override fun load(
        preferredFormat: TextureFormat,
        dataLoader: suspend CoroutineScope.() -> LazyTexture.ImageData<*>,
    ) {
        VfsScope.launch {
            check(state == TextureState.UNLOADED) { "This texture has already been loaded!" }
            state = TextureState.LOADING
            bitmap =
                dataLoader().data as? ImageBitmap
                    ?: error("LazyImageBitmapTexture requires an ImageData of type ImageBitmap!")
            val mips = Texture.calculateNumMips(bitmap.width, bitmap.height)

            size = Extent3D(bitmap.width, bitmap.height, 1)
            // strange flow but setting the texture descriptor will recreate all the underlying
            // webgpu textures & views and then automatically write the data to the buffer
            textureDescriptor =
                TextureDescriptor(
                    size,
                    mips,
                    1,
                    TextureDimension.D2,
                    preferredFormat,
                    TextureUsage.TEXTURE or TextureUsage.COPY_DST or TextureUsage.RENDER_ATTACHMENT,
                )

            device.queue.copyExternalImageToTexture(bitmap, TextureCopyView(gpuTexture), size)

            if (mips > 1) {
                generateMipMaps(device)
            }

            state = TextureState.LOADED
        }
    }

    /**
     * The [Extent3D] size of the texture. Uses [Pixmap.width], [Pixmap.height] and a depth of `1`.
     */
    override var size: Extent3D = Extent3D(1, 1, 1)
        private set

    override var id: Int = UniqueId.next<Texture>()
        private set

    override var textureDescriptor: TextureDescriptor =
        TextureDescriptor(
            size,
            1,
            1,
            TextureDimension.D2,
            TextureFormat.RGBA8_UNORM,
            TextureUsage.TEXTURE or TextureUsage.COPY_DST or TextureUsage.RENDER_ATTACHMENT,
        )
        set(value) {
            if (state == TextureState.UNLOADED) return
            field = value
            val textureToDestroy = gpuTexture
            val viewToDestroy = view
            viewToDestroy.release()
            textureToDestroy.release()

            gpuTexture = device.createTexture(textureDescriptor)
        }

    override var gpuTexture: WebGPUTexture = device.createTexture(textureDescriptor)
        private set(value) {
            if (state == TextureState.UNLOADED) return
            field = value
            view = field.createView(textureViewDescriptor)
            writeDataToBuffer()
        }

    override var textureViewDescriptor: TextureViewDescriptor? = null
        set(value) {
            if (state == TextureState.UNLOADED) return
            field = value
            view.release()
            view = gpuTexture.createView(value)
        }

    override var view: TextureView = gpuTexture.createView(textureViewDescriptor)
        private set(value) {
            if (state == TextureState.UNLOADED) return
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

    override fun writeDataToBuffer() {
        if (state != TextureState.LOADED) return
        device.queue.copyExternalImageToTexture(bitmap, TextureCopyView(gpuTexture), size)
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
