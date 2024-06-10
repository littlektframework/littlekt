package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.file.FloatBuffer
import com.littlekt.file.IntBuffer
import com.littlekt.file.ShortBuffer

/**
 * An open connection to a graphic and/or compute device.
 *
 * Responsible for the creation of most rendering and compute resources. These are then used in
 * commands, which are submitted to a [Queue].
 *
 * A device may be requested from an adapter with [Adapter.requestDevice]
 */
expect class Device : Releasable {
    /** A [Queue] that has been ready from same adapter this device was created with. */
    val queue: Queue

    /** @return a newly created [ShaderModule] using WGSL source code. */
    fun createShaderModule(src: String): ShaderModule

    /** @return a newly created [RenderPipeline]. */
    fun createRenderPipeline(desc: RenderPipelineDescriptor): RenderPipeline

    /** @return a newly created [ComputePipeline] */
    fun createComputePipeline(desc: ComputePipelineDescriptor): ComputePipeline

    /** @return a newly created [PipelineLayout]. */
    fun createPipelineLayout(desc: PipelineLayoutDescriptor): PipelineLayout

    /**
     * @param debug label for a [CommandEncoder].
     * @return a newly created, empty [CommandEncoder].
     */
    fun createCommandEncoder(label: String? = null): CommandEncoder

    /** @return a newly created [GPUBuffer]. */
    fun createBuffer(desc: BufferDescriptor): GPUBuffer

    /** @return a newly created [BindGroupLayout]. */
    fun createBindGroupLayout(desc: BindGroupLayoutDescriptor): BindGroupLayout

    /** @return a newly created [BindGroup]. */
    fun createBindGroup(desc: BindGroupDescriptor): BindGroup

    /** @return a newly created [Sampler]. */
    fun createSampler(desc: SamplerDescriptor): Sampler

    /** @return a newly created [WebGPUTexture]. */
    fun createTexture(desc: TextureDescriptor): WebGPUTexture

    /**
     * This uses [createBuffer] internally, maps it at creation, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using an [ShortArray].
     */
    fun createGPUShortBuffer(label: String, data: ShortArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using a [FloatArray].
     */
    fun createGPUFloatBuffer(label: String, data: FloatArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using an [IntArray].
     */
    fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using a [ByteArray].
     */
    fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer

    override fun release()
}

/** A handle to a physical graphics and/or compute device. */
expect class Adapter : Releasable {
    /**
     * Requests a connection to a physical device, creating a logical device.
     *
     * @return the [Device] together with a [Queue] that executes command buffers.
     */
    suspend fun requestDevice(): Device

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

/**
 * Handle to a compiled shader module.
 *
 * A `ShaderModule` represents a compiled shader module on the GPU. It can be created by pass source
 * code to [Device.createShaderModule]. Shader modules are used to define programmable stages of a
 * pipeline.
 */
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
 * A handle to a presentable surface.
 *
 * A `Surface` represents a platform-specific surface (e.g. a window) onto which rendered images may
 * be presented.
 */
expect class Surface : Releasable {

    /** Initializes [Surface] for presentation. */
    fun configure(configuration: SurfaceConfiguration)

    /** @return the capabilities of the surface when used with the given [adapter]. */
    fun getCapabilities(adapter: Adapter): SurfaceCapabilities

    /** @return the optimal [TextureFormat] for displaying on the given [Adapter] */
    fun getPreferredFormat(adapter: Adapter): TextureFormat

    /**
     * In order to present the [SurfaceTexture] returned by this function, first a [Queue.submit]
     * needs to be done with some work rendering to this texture. Then [present] needs to be called.
     *
     * @return the next [SurfaceTexture] to be presented by the swapchain for drawing.
     */
    fun getCurrentTexture(): SurfaceTexture

    /**
     * Schedule the underlying [SurfaceTexture] to be presented on this surface.
     *
     * Needs to be called after any work on the texture is scheduled via [Queue.submit]
     */
    fun present()

    /** Release this memory held by this surface. */
    override fun release()
}

/** Defines the capabilities of a given surface and adapter. */
expect class SurfaceCapabilities {
    /**
     * List of supported formats to use with a given adapter. The first format is usually the
     * preferred.
     */
    val formats: List<TextureFormat>

    /** List of supported presentation modes to use with the given adapter. */
    val alphaModes: List<AlphaMode>
}

/**
 * Describes a [Surface]. For use with [Surface.configure].
 *
 * @param device the device of the surface
 * @param usage the usage of the swap chain. The only supported usage is
 *   [TextureUsage.RENDER_ATTACHMENT].
 * @param format the texture format of the swap chain. The only formats that are guaranteed are
 *   [TextureFormat.BGRA8_UNORM] and [TextureFormat.BGRA8_UNORM_SRGB].
 * @param presentMode Presentation mode of the swap chain. Fifo is the only mode guaranteed to be
 *   supported. FifoRelaxed, Imeediate, and Mailbox will crash if unsupported, while AutoVsync and
 *   AutoNoVsync will gracefully do a designed sets of fallbacks if their primary modes are
 *   unsupported.
 * @param alphaMode specifies how the alpha channel of textures should be handled during
 *   compositing.
 * @param width width of the swap chain
 * @param height height of the swap chain
 */
data class SurfaceConfiguration(
    val device: Device,
    val usage: TextureUsage,
    val format: TextureFormat,
    val presentMode: PresentMode,
    val alphaMode: AlphaMode,
    val width: Int,
    val height: Int,
)

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

/** A texture that can be rendered to. Result of a successful call to [Surface.getCurrentTexture] */
expect class SurfaceTexture {
    /** The accessible view of the frame. */
    val texture: WebGPUTexture?

    /** The current status of the texture */
    val status: TextureStatus
}

/** Handle to a texture on the GPU. It can be created with [Device.createTexture]. */
expect class WebGPUTexture : Releasable {
    /** Creates a view of this texture. */
    fun createView(desc: TextureViewDescriptor? = null): TextureView

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

/**
 * Describes a [GPUBuffer]. For use with [Device.createBuffer].
 *
 * @param label debug label of a buffer.
 * @param size size of a buffer, in bytes.
 * @param usage usages of a buffer. If the buffer is used in any way that isn't specified here, the
 *   operation will fail.
 * @param mappedAtCreation allows a buffer to be mapped immediately after they are made. It does not
 *   have to [BufferUsage.MAP_READ] or [BufferUsage.MAP_WRITE], all buffers are allowed to be mapped
 *   at creation. If this is `true`, [size] must be a multiple of `4`.
 */
data class BufferDescriptor(
    val label: String,
    val size: Long,
    val usage: BufferUsage,
    val mappedAtCreation: Boolean
)

/**
 * Handle to a GPU-accessible buffer.
 *
 * Create with [Device.createBuffer].
 *
 * @see [Device.createGPUFloatBuffer]
 * @see [Device.createGPUIntBuffer]
 * @see [Device.createGPUShortBuffer]
 * @see [Device.createGPUByteBuffer]
 */
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
