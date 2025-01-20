package com.littlekt.graphics

import com.littlekt.async.VfsScope
import com.littlekt.async.onRenderingThread
import com.littlekt.graphics.Texture.Companion.nextId
import com.littlekt.graphics.webgpu.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A [Texture] that uses a [Pixmap] as the underlying data but lazily. It needs to be called with
 * [load] to initialize asynchronously.
 *
 * @param device the device for underlying GPU buffers creation
 * @param samplerDescriptor optional [SamplerDescriptor] to pass in when building initial texture.
 * @author Colton Daily
 * @date 5/5/2024
 */
class LazyPixmapTexture(
    val device: Device,
    samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
) : Texture {
    private var pixmap: Pixmap = Pixmap(1, 1)
    override var state: TextureState = TextureState.UNLOADED
        private set

    /**
     * Launches a new coroutine in the [VfsScope] that requires loading [Pixmap] as a return value.
     * This should be called only once as it will throw an error if called again.
     */
    fun load(preferredFormat: TextureFormat, pixmapLoader: suspend CoroutineScope.() -> Pixmap) =
        VfsScope.launch {
            check(state == TextureState.UNLOADED) { "This texture has already been loaded!" }
            state = TextureState.LOADING
            pixmap = pixmapLoader()
            val mips = Texture.calculateNumMips(pixmap.width, pixmap.height)

            size = Extent3D(pixmap.width, pixmap.height, 1)
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

            // we need to submit texture and generate mips on the rendering thread otherwise wgpu
            // will fail when submitting the queue  on separate thread
            onRenderingThread {
                device.queue.writeTexture(
                    pixmap.pixels.toArray(),
                    TextureCopyView(gpuTexture),
                    TextureDataLayout(textureDescriptor.format.bytes * pixmap.width, pixmap.height),
                    size,
                )
                if (mips > 1) {
                    generateMipMaps(device)
                }
            }
            state = TextureState.LOADED
        }

    /**
     * The [Extent3D] size of the texture. Uses [Pixmap.width], [Pixmap.height] and a depth of `1`.
     */
    override var size: Extent3D = Extent3D(1, 1, 1)
        private set

    override var id: Int = nextId()
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

    override fun writeDataToBuffer() {
        if (state != TextureState.LOADED) return
        device.queue.writeTexture(
            pixmap.pixels.toArray(),
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
