package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.async.await
import com.littlekt.file.*
import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import org.khronos.webgl.Uint8Array
import org.w3c.dom.ImageBitmap

actual class Device(val delegate: GPUDevice) : Releasable {
    actual val queue: Queue by lazy { Queue(delegate.queue) }

    actual val features: List<Feature> by lazy {
        val results = mutableListOf<Feature>()
        val jsFeatures = delegate.features
        Feature.entries.forEach { feature ->
            if (jsFeatures.has(feature.nativeVal)) {
                results += feature
            }
        }
        results.toList()
    }

    actual val limits: Limits by lazy {
        val jsLimits = delegate.limits
        Limits(
            maxTextureDimension1D = jsLimits.maxTextureDimension1D,
            maxTextureDimension2D = jsLimits.maxTextureDimension2D,
            maxTextureDimension3D = jsLimits.maxTextureDimension3D,
            maxTextureArrayLayers = jsLimits.maxTextureArrayLayers,
            maxBindGroups = jsLimits.maxBindGroups,
            maxBindGroupsPlusVertexBuffers = jsLimits.maxBindGroupsPlusVertexBuffers,
            maxBindingsPerBindGroup = jsLimits.maxBindingsPerBindGroup,
            maxDynamicUniformBuffersPerPipelineLayout =
                jsLimits.maxDynamicUniformBuffersPerPipelineLayout,
            maxDynamicStorageBuffersPerPipelineLayout =
                jsLimits.maxDynamicStorageBuffersPerPipelineLayout,
            maxSampledTexturesPerShaderStage = jsLimits.maxSampledTexturesPerShaderStage,
            maxSamplersPerShaderStage = jsLimits.maxSamplersPerShaderStage,
            maxStorageBuffersPerShaderStage = jsLimits.maxStorageBuffersPerShaderStage,
            maxStorageTexturesPerShaderStage = jsLimits.maxStorageTexturesPerShaderStage,
            maxUniformBuffersPerShaderStage = jsLimits.maxUniformBuffersPerShaderStage,
            maxUniformBufferBindingSize = jsLimits.maxUniformBufferBindingSize.toLong(),
            maxStorageBufferBindingSize = jsLimits.maxStorageBufferBindingSize.toLong(),
            minUniformBufferOffsetAlignment = jsLimits.minUniformBufferOffsetAlignment,
            minStorageBufferOffsetAlignment = jsLimits.minStorageBufferOffsetAlignment,
            maxVertexBuffers = jsLimits.maxVertexBuffers,
            maxBufferSize = jsLimits.maxBufferSize.toLong(),
            maxVertexAttributes = jsLimits.maxVertexAttributes,
            maxVertexBufferArrayStride = jsLimits.maxVertexBufferArrayStride,
            maxInterStageShaderVariables = jsLimits.maxInterStageShaderVariables,
            maxColorAttachments = jsLimits.maxColorAttachments,
            maxColorAttachmentBytesPerSample = jsLimits.maxColorAttachmentBytesPerSample,
            maxComputeWorkgroupStorageSize = jsLimits.maxComputeWorkgroupStorageSize,
            maxComputeInvocationsPerWorkgroup = jsLimits.maxComputeInvocationsPerWorkgroup,
            maxComputeWorkgroupSizeX = jsLimits.maxComputeWorkgroupSizeX,
            maxComputeWorkgroupSizeY = jsLimits.maxComputeWorkgroupSizeY,
            maxComputeWorkgroupSizeZ = jsLimits.maxComputeWorkgroupSizeZ,
            maxComputeWorkgroupsPerDimension = jsLimits.maxComputeWorkgroupsPerDimension,
        )
    }

    actual fun createShaderModule(src: String): ShaderModule {
        return ShaderModule(delegate.createShaderModule(GPUShaderModuleDescriptor().apply { code = src }))
    }

    actual fun createRenderPipeline(desc: RenderPipelineDescriptor): RenderPipeline {
        return RenderPipeline(delegate.createRenderPipeline(desc.toNative()))
    }

    actual fun createComputePipeline(desc: ComputePipelineDescriptor): ComputePipeline {
        return ComputePipeline(delegate.createComputePipeline(desc.toNative()))
    }

    actual fun createPipelineLayout(desc: PipelineLayoutDescriptor): PipelineLayout {
        return PipelineLayout(delegate.createPipelineLayout(desc.toNative()))
    }

    actual fun createCommandEncoder(label: String?): CommandEncoder {
        return CommandEncoder(delegate.createCommandEncoder(GPUObjectBase().apply { this.label = label }))
    }

    actual fun createBuffer(desc: BufferDescriptor): GPUBuffer {
        return GPUBuffer(delegate.createBuffer(desc.toNative()), desc.size)
    }

    actual fun createBindGroupLayout(desc: BindGroupLayoutDescriptor): BindGroupLayout {
        return BindGroupLayout(delegate.createBindGroupLayout(desc.toNative()))
    }

    actual fun createBindGroup(desc: BindGroupDescriptor): BindGroup {
        return BindGroup(delegate.createBindGroup(desc.toNative()))
    }

    actual fun createSampler(desc: SamplerDescriptor): Sampler {
        return Sampler(delegate.createSampler(desc.toNative()))
    }

    actual fun createTexture(desc: TextureDescriptor): WebGPUTexture {
        val textureSize =
            (desc.size.width * desc.size.height * desc.size.depth * desc.format.bytes).toLong()
        return WebGPUTexture(delegate.createTexture(desc.toNative()), textureSize)
    }

    actual fun createGPUShortBuffer(
        label: String,
        data: ShortArray,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, nativeSizeOf(data).toLong() * Short.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange(0, buffer.size).putShort(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUShortBuffer(
        label: String,
        data: ShortBuffer,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.capacity.toLong() * Short.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange(0, buffer.size).putShort(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatArray,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, nativeSizeOf(data).toLong() * Float.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange(0, buffer.size).putFloat(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatBuffer,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.capacity.toLong() * Float.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange(0, buffer.size).putFloat(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer {
        val buffer =
            createBuffer(BufferDescriptor(label, nativeSizeOf(data).toLong() * Int.SIZE_BYTES, usage, true))
        buffer.getMappedRange(0, buffer.size).putInt(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUIntBuffer(label: String, data: IntBuffer, usage: BufferUsage): GPUBuffer {
        val buffer =
            createBuffer(BufferDescriptor(label, data.capacity.toLong() * Int.SIZE_BYTES, usage, true))
        buffer.getMappedRange(0, buffer.size).putInt(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer {
        val buffer = createBuffer(BufferDescriptor(label, nativeSizeOf(data).toLong(), usage, true))
        buffer.getMappedRange(0, buffer.size).putByte(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUByteBuffer(label: String, data: ByteBuffer, usage: BufferUsage): GPUBuffer {
        val buffer = createBuffer(BufferDescriptor(label, data.capacity.toLong(), usage, true))
        buffer.getMappedRange(0, buffer.size).putByte(data)
        buffer.unmap()

        return buffer
    }

    actual override fun release() {}

    actual fun poll(): Boolean = true
}

actual class Adapter(val delegate: GPUAdapter) : Releasable {

    actual val features: List<Feature> by lazy {
        val results = mutableListOf<Feature>()
        val jsFeatures = delegate.features
        Feature.entries.forEach { feature ->
            if (jsFeatures.has(feature.nativeVal)) {
                results += feature
            }
        }
        results.toList()
    }

    actual val limits: Limits by lazy {
        val jsLimits = delegate.limits
        Limits(
            maxTextureDimension1D = jsLimits.maxTextureDimension1D,
            maxTextureDimension2D = jsLimits.maxTextureDimension2D,
            maxTextureDimension3D = jsLimits.maxTextureDimension3D,
            maxTextureArrayLayers = jsLimits.maxTextureArrayLayers,
            maxBindGroups = jsLimits.maxBindGroups,
            maxBindGroupsPlusVertexBuffers = jsLimits.maxBindGroupsPlusVertexBuffers,
            maxBindingsPerBindGroup = jsLimits.maxBindingsPerBindGroup,
            maxDynamicUniformBuffersPerPipelineLayout =
                jsLimits.maxDynamicUniformBuffersPerPipelineLayout,
            maxDynamicStorageBuffersPerPipelineLayout =
                jsLimits.maxDynamicStorageBuffersPerPipelineLayout,
            maxSampledTexturesPerShaderStage = jsLimits.maxSampledTexturesPerShaderStage,
            maxSamplersPerShaderStage = jsLimits.maxSamplersPerShaderStage,
            maxStorageBuffersPerShaderStage = jsLimits.maxStorageBuffersPerShaderStage,
            maxStorageTexturesPerShaderStage = jsLimits.maxStorageTexturesPerShaderStage,
            maxUniformBuffersPerShaderStage = jsLimits.maxUniformBuffersPerShaderStage,
            maxUniformBufferBindingSize = jsLimits.maxUniformBufferBindingSize.toLong(),
            maxStorageBufferBindingSize = jsLimits.maxStorageBufferBindingSize.toLong(),
            minUniformBufferOffsetAlignment = jsLimits.minUniformBufferOffsetAlignment,
            minStorageBufferOffsetAlignment = jsLimits.minStorageBufferOffsetAlignment,
            maxVertexBuffers = jsLimits.maxVertexBuffers,
            maxBufferSize = jsLimits.maxBufferSize.toLong(),
            maxVertexAttributes = jsLimits.maxVertexAttributes,
            maxVertexBufferArrayStride = jsLimits.maxVertexBufferArrayStride,
            maxInterStageShaderVariables = jsLimits.maxInterStageShaderVariables,
            maxColorAttachments = jsLimits.maxColorAttachments,
            maxColorAttachmentBytesPerSample = jsLimits.maxColorAttachmentBytesPerSample,
            maxComputeWorkgroupStorageSize = jsLimits.maxComputeWorkgroupStorageSize,
            maxComputeInvocationsPerWorkgroup = jsLimits.maxComputeInvocationsPerWorkgroup,
            maxComputeWorkgroupSizeX = jsLimits.maxComputeWorkgroupSizeX,
            maxComputeWorkgroupSizeY = jsLimits.maxComputeWorkgroupSizeY,
            maxComputeWorkgroupSizeZ = jsLimits.maxComputeWorkgroupSizeZ,
            maxComputeWorkgroupsPerDimension = jsLimits.maxComputeWorkgroupsPerDimension,
        )
    }

    actual suspend fun requestDevice(descriptor: DeviceDescriptor?): Device {
        return Device(delegate.requestDevice(descriptor?.toNative()).await())
    }

    actual override fun release() {}
}

expect fun nativeSubmit(delegate: GPUQueue, vararg cmdBuffers: CommandBuffer)

expect fun nativeWriteTexture(
    delegate: GPUQueue,
    data: ByteArray,
    destination: TextureCopyView,
    layout: TextureDataLayout,
    copySize: Extent3D,
    size: Long,
)
expect fun nativeSizeOf(array: ByteArray): Int
expect fun nativeSizeOf(array: IntArray): Int
expect fun nativeSizeOf(array: FloatArray): Int
expect fun nativeSizeOf(array: ShortArray): Int

actual class Queue(val delegate: GPUQueue) : Releasable {

    actual fun submit(vararg cmdBuffers: CommandBuffer) {
        nativeSubmit(delegate,*cmdBuffers)
    }

    actual fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        data as GenericBuffer<*>
        delegate.writeTexture(
            destination.toNative(),
            data.buffer,
            layout.toNative(),
            copySize.toNative(),
        )
    }

    actual fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) = nativeWriteTexture(delegate,data,destination,layout,copySize,size)

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset.toDouble(), data.buffer, dataOffset.toDouble(), size.toDouble())
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset.toDouble(), data.buffer, dataOffset.toDouble(), size.toDouble())
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset.toDouble(), data.buffer, dataOffset.toDouble(), size.toDouble())
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset.toDouble(), data.buffer, dataOffset.toDouble(), size.toDouble())
    }

    fun copyExternalImageToTexture(
        data: ImageBitmap,
        destination: TextureCopyView,
        copySize: Extent3D,
    ) {
        delegate.copyExternalImageToTexture(
            GPUImageCopyExternalImage().apply { source = data },
            destination.toNative(),
            copySize.toNative(),
        )
    }

    actual override fun release() {}
}

actual class ShaderModule(val delegate: GPUShaderModule) : Releasable {
    actual override fun release() {}
}

actual class Surface(val delegate: GPU, val canvas: GPUCanvasContext) : Releasable {
    actual fun configure(configuration: SurfaceConfiguration) {
        canvas.configure(configuration.toNative())
    }

    actual fun getCapabilities(adapter: Adapter): SurfaceCapabilities {
        return SURFACE_CAPABILITIES
    }

    actual fun getPreferredFormat(adapter: Adapter): TextureFormat {
        val format = delegate.getPreferredCanvasFormat()
        return TextureFormat.from(format) ?: error("Unsupported canvas format: $format")
    }

    actual fun getCurrentTexture(): SurfaceTexture {
        return SurfaceTexture(WebGPUTexture(canvas.getCurrentTexture(), 0L))
    }

    actual fun present() {}

    actual override fun release() {}

    companion object {
        private val SURFACE_CAPABILITIES = SurfaceCapabilities()
    }
}

actual class SurfaceCapabilities {
    actual val formats: List<TextureFormat> =
        listOf(TextureFormat.BGRA8_UNORM, TextureFormat.RGBA8_UNORM, TextureFormat.RGBA16_FLOAT)

    actual val alphaModes: List<AlphaMode> = listOf(AlphaMode.OPAQUE, AlphaMode.PREMULTIPLIED)
}

actual class SurfaceTexture(actual val texture: WebGPUTexture?) {
    actual val status: TextureStatus = TextureStatus.SUCCESS
}

actual class WebGPUTexture(val delegate: GPUTexture, size: Long) : Releasable {

    private val info = TextureResourceInfo(this, size)

    actual fun createView(desc: TextureViewDescriptor?): TextureView {
        return TextureView(delegate.createView(desc?.toNative()))
    }

    actual override fun release() {
        delegate.destroy()
        info.delete()
    }

}

actual class TextureView(val delegate: GPUTextureView) : IntoBindingResource {
    actual fun release() {}

    override fun toNative(): GPUBindingResource {
        return delegate
    }
}

actual class GPUBuffer(val delegate: GPUBufferJs, actual val size: Long) : Releasable {

    private val info = BufferResourceInfo(this, size)

    actual fun getMappedRange(offset: Long, size: Long): ByteBuffer {
        return ByteBufferImpl(Uint8Array(delegate.getMappedRange(offset.toDouble(), size.toDouble())))
    }

    actual fun getMappedRange(): ByteBuffer = getMappedRange(0, size)

    actual fun unmap() {
        delegate.unmap()
    }

    actual override fun release() {
        delegate.destroy()
        info.delete()
    }

}

actual class Sampler(val delegate: GPUSampler) : IntoBindingResource, Releasable {
    actual override fun release() {}

    override fun toNative(): GPUBindingResource {
        return delegate
    }
}

/**
 * View of a texture which can be used to copy to/from a buffer/texture.
 *
 * @param texture the texture to be copied to/from.
 * @param mipLevel the target mip level of the texture.
 * @param origin the base texel of the texture in the select [mipLevel].
 */
data class ExternalTextureCopyView(
    val texture: WebGPUTexture,
    val mipLevel: Int = 0,
    val origin: Origin3D = Origin3D(0, 0, 0),
)