package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.*
import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

actual class Device(val delegate: GPUDevice) : Releasable {
    actual val queue: Queue by lazy { Queue(delegate.queue) }

    actual fun createShaderModule(src: String): ShaderModule {
        return ShaderModule(delegate.createShaderModule(GPUShaderModuleDescriptor { code = src }))
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
        return CommandEncoder(delegate.createCommandEncoder(GPUObjectBase { this.label = label }))
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
        usage: BufferUsage
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.size.toLong() * Short.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange(0, buffer.size).putShort(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatArray,
        usage: BufferUsage
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.size.toLong() * Float.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange(0, buffer.size).putFloat(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer {
        val buffer =
            createBuffer(BufferDescriptor(label, data.size.toLong() * Int.SIZE_BYTES, usage, true))
        buffer.getMappedRange(0, buffer.size).putInt(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer {
        val buffer = createBuffer(BufferDescriptor(label, data.size.toLong(), usage, true))
        buffer.getMappedRange(0, buffer.size).putByte(data)
        buffer.unmap()

        return buffer
    }

    actual override fun release() {}
}

actual class Adapter(val delegate: GPUAdapter) : Releasable {

    /** The features which can be used to create devices on this adapter. */
    actual val features: List<Feature> by lazy {
        val results = mutableListOf<Feature>()
        delegate.features.map { jsFeature ->
            Feature.entries.forEach { feature ->
                if (feature.nativeVal == jsFeature) {
                    results += feature
                }
            }
        }
        results.toList()
    }

    actual suspend fun requestDevice(): Device {
        return Device(delegate.requestDevice().await())
    }

    actual override fun release() {}
}

actual class Queue(val delegate: GPUQueue) : Releasable {

    actual fun submit(vararg cmdBuffers: CommandBuffer) {
        delegate.submit(cmdBuffers.map { it.delegate }.toTypedArray())
    }

    actual fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long
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
        size: Long
    ) {
        val arrayBuffer = Uint8Array(ArrayBuffer(data.size)).apply { set(data.toTypedArray()) }

        delegate.writeTexture(
            destination.toNative(),
            arrayBuffer,
            layout.toNative(),
            copySize.toNative()
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset, data.buffer, dataOffset, size)
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset, data.buffer, dataOffset, size)
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset, data.buffer, dataOffset, size)
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long
    ) {
        data as GenericBuffer<*>
        delegate.writeBuffer(buffer.delegate, offset, data.buffer, dataOffset, size)
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
        destroy()
    }

    actual fun destroy() {
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
        return ByteBufferImpl(Uint8Array(delegate.getMappedRange(offset, size)))
    }

    actual fun getMappedRange(): ByteBuffer = getMappedRange(0, size)

    actual fun unmap() {
        delegate.unmap()
    }

    actual override fun release() {
        destroy()
    }

    actual fun destroy() {
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
