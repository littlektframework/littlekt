package com.littlekt.graphics

import com.littlekt.graphics.webgpu.*
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a [WebGPUTexture] and writes it to the [Device.queue]. Creates a [TextureView] and
 * [Sampler]. It needs to be called with [load] to initialize asynchronously.
 *
 * @author Colton Daily
 * @date 4/9/2024
 */
interface LazyTexture : Texture {
    override val state: TextureState
        get() = TextureState.UNLOADED

    /**
     * Call to load and upload texture data. Usually called within a coroutine or separate thread.
     */
    fun load(preferredFormat: TextureFormat, dataLoader: suspend CoroutineScope.() -> ImageData<*>)

    data class ImageData<T>(val data: T)
}

/** Creates a platform default [LazyTexture]. */
expect fun LazyTexture(
    device: Device,
    samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
): LazyTexture
