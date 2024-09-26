package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.file.FloatBuffer
import com.littlekt.file.IntBuffer
import com.littlekt.file.ShortBuffer
import com.littlekt.graphics.webgpu.*

/**
 * An open connection to a graphic and/or compute device.
 *
 * Responsible for the creation of most rendering and compute resources. These are then used in
 * commands, which are submitted to a [Queue].
 *
 * A device may be requested from an adapter with [Adapter.requestDevice]
 */
expect class Device : Releasable {


    /** @return a newly created [Sampler]. */
    fun createSampler(desc: SamplerDescriptor): Sampler

    /** @return a newly created [WebGPUTexture]. */
    fun createTexture(desc: TextureDescriptor): WebGPUTexture

    override fun release()
}

/** A handle to a physical graphics and/or compute device. */
expect class Adapter : Releasable {


    /** Release the memory held by the adapter. */
    override fun release()
}

/**
 * A handle to a command queue on a [Device].
 *
 * A `Queue` executes recorded [CommandBuffer] objects and provides convenience methods for writing
 * to buffers and textures. It can be created along with a [Device] by calling
 * [Adapter.requestDevice].
 */
expect class Queue : Releasable {

    /** Submit a series of finished command buffers for execution. */
    fun submit(vararg cmdBuffers: CommandBuffer)

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong()
    )

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong()
    )

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong()
    )

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong()
    )

    /**
     * Schedule a write of some data into a texture.
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     * However, [data] will be immediately copied into staging memory; so the caller may discard it
     * any time after this call completes.
     *
     * This method fails if [size] overruns the size of [destination], or if [data] is too short.
     *
     * @param data contains the texels to be written, which must be in the same format as the
     *   texture.
     * @param destination specifies the texture to write into, and the location within the texture
     *   (coordinate offset, mip level) that will be overwritten.
     * @param layout describes the memory layout of data, which does not necessarily have to have
     *   tightly packed rows.
     * @param size is the size, in texels, of the region to be written.
     */
    fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long = data.capacity.toLong()
    )

    /**
     * Schedule a write of some data into a texture.
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     * However, [data] will be immediately copied into staging memory; so the caller may discard it
     * any time after this call completes.
     *
     * This method fails if [size] overruns the size of [destination], or if [data] is too short.
     *
     * @param data contains the texels to be written, which must be in the same format as the
     *   texture.
     * @param destination specifies the texture to write into, and the location within the texture
     *   (coordinate offset, mip level) that will be overwritten.
     * @param layout describes the memory layout of data, which does not necessarily have to have
     *   tightly packed rows.
     * @param size is the size, in texels, of the region to be written.
     */
    fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long = data.size.toLong()
    )

    override fun release()
}


expect class ShaderModule : Releasable {
    override fun release()
}

/**
 * Extent of a texture related operation.
 *
 * @param width width of the extent
 * @param height height of the extent
 * @param depth depth of the extent or number of array layers.
 */
data class Extent3D(val width: Int, val height: Int, val depth: Int)

/**
 * Origin of a copy to/from a texture.
 *
 * @param x x-position of the origin
 * @param y y-position of the origin
 * @param z z-position of the origin
 */
data class Origin3D(val x: Int, val y: Int, val z: Int)

/**
 * Describes a [TextureView].
 *
 * @param format the format of the texture view. Either must be the same as the texture format or in
 *   the list of `viewFormats` in the texture's descriptor.
 * @param dimension the dimension of the texture view. For 1D textures, this must be
 *   [TextureViewDimension.D1]. For 2D textures, it must be one of [TextureViewDimension.D2],
 *   [TextureViewDimension.D2_ARRAY], [TextureViewDimension.CUBE], and
 *   [TextureViewDimension.CUBE_ARRAY]. For 3D textures it must be [TextureViewDimension.D3].
 * @param aspect the aspect of the texture. Color textures must be [TextureAspect.ALL].
 * @param baseMipLevel base mip level.
 * @param mipLevelCount mip level count. `baseMipLevel + mipLevelCount` must b e less or equal to
 *   underlying texture mip count. If `0` considered to include the rest of the mipmap levels, but
 *   at least 1 in total.
 * @param baseArrayLayer base array layer.
 * @param arrayLayerCount Layer count. `baseArrayLayer + arrayLayerCount` must be less or equal to
 *   the underlying array count. If `0`, considered to include the rest of the array layers, but at
 *   least 1 in total.
 * @param label debug label of a texture view.
 */
data class TextureViewDescriptor(
    val format: TextureFormat,
    val dimension: TextureViewDimension,
    val aspect: TextureAspect = TextureAspect.ALL,
    val baseMipLevel: Int = 0,
    val mipLevelCount: Int = 0,
    val baseArrayLayer: Int = 0,
    val arrayLayerCount: Int = 0,
    val label: String? = null
)

/**
 * Describes a [WebGPUTexture].
 *
 * @param size of the texture. All components must be greater than zero. For a regular 1D/2D
 *   texture, the unused sizes will be 1. For 2DArray textures, Z is the number of 2D textures in
 *   that array.
 * @param mipLevelCount mip count of the texture. For a texture with no extra mips, this must be 1.
 * @param sampleCount sample count ofa texture. If this is not 1, texture must have
 *   `TextureBindingLayout.multisampled` set to true.
 * @param dimension dimensions of the texture.
 * @param format format of the texture.
 * @param usage allowed usages of the texture. If used in other ways, the operation will fail.
 * @param label debug label of a texture.
 */
data class TextureDescriptor(
    val size: Extent3D,
    val mipLevelCount: Int,
    val sampleCount: Int,
    val dimension: TextureDimension,
    val format: TextureFormat,
    val usage: TextureUsage,
    val label: String? = null
)

/** Handle to a texture on the GPU. It can be created with [Device.createTexture]. */
expect class WebGPUTexture : Releasable {

    override fun release()

    /** Destroy the associated native resources as soon as possible. */
    fun destroy()
}

/**
 * A handle to a texture view.
 *
 * A `TextureView` object describes a texture and associated metadata needed by a [RenderPipeline]
 * or [BindGroup].
 */
expect class TextureView : IntoBindingResource {
    fun release()
}

expect class GPUBuffer : Releasable {

    /** The length of the buffer allocation in bytes. */
    val size: Long

    /**
     * Use only a portion of this buffer for a given operation.
     *
     * @return the underlying [ByteBuffer]
     */
    // see the empty param getMappedRange function for a reason why we aren't using default values
    // here.
    fun getMappedRange(offset: Long, size: Long): ByteBuffer

    /**
     * Use the entire buffer as the portion for a given operation.
     *
     * @return the underlying [ByteBuffer]
     */
    // we are using an empty parameter function here instead of a extension
    // because of a JS IR compiler error throwing an IrLinkageError when referencing size as a
    // default param
    fun getMappedRange(): ByteBuffer

    /** Flushes any pending write operations and unmaps the buffer from host memory. */
    fun unmap()

    override fun release()

    /** Destroy the associated native resources as soon as possible. */
    fun destroy()
}

/**
 * Describes a [Sampler]. For use with [Device.createSampler].
 *
 * @param compare if enabled, this is a comparison sampler using the given comparison function.
 * @param addressModeU how to detail with out of bounds accesses in the u (i.e. x) direction
 * @param addressModeV how to detail with out of bounds accesses in the v (i.e. y) direction
 * @param addressModeW how to detail with out of bounds accesses in the w (i.e. z) direction
 * @param magFilter how to filter the texture when it needs to be magnified (made larger)
 * @param minFilter how to filter the texture when it needs to be minified (made smaller)
 * @param mipmapFilter how to filter between mip map levels
 * @param lodMinClamp minimum level of detail (i.e. mip level) to use
 * @param lodMaxClamp maximum level of detail (i.e. mip level) to use
 * @param maxAnisotropy must be at least 1. if this is not 1, all filter must e linear.
 * @param label debug label of a sampler.
 */
data class SamplerDescriptor(
    val compare: CompareFunction? = null,
    val addressModeU: AddressMode = AddressMode.CLAMP_TO_EDGE,
    val addressModeV: AddressMode = AddressMode.CLAMP_TO_EDGE,
    val addressModeW: AddressMode = AddressMode.CLAMP_TO_EDGE,
    val magFilter: FilterMode = FilterMode.NEAREST,
    val minFilter: FilterMode = FilterMode.NEAREST,
    val mipmapFilter: FilterMode = FilterMode.NEAREST,
    val lodMinClamp: Float = 0f,
    val lodMaxClamp: Float = 100000000f,
    val maxAnisotropy: Short = 1,
    val label: String? = null
)

/**
 * A handle to a sampler.
 *
 * A `Sampler` object defines how a pipeline will sample from a [TextureView]. Samplers define image
 * filters (including anisotropy) and address (wrapping) modes, among other things. See
 * [SamplerDescriptor].
 *
 * It can be created with [Device.createSampler].
 */
expect class Sampler : IntoBindingResource, Releasable {
    override fun release()
}
