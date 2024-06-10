package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer

actual class Device : Releasable {
    actual val queue: Queue
        get() = TODO("Not yet implemented")

    actual fun createShaderModule(src: String): ShaderModule {
        TODO("Not yet implemented")
    }

    actual fun createRenderPipeline(desc: RenderPipelineDescriptor): RenderPipeline {
        TODO("Not yet implemented")
    }

    actual fun createPipelineLayout(desc: PipelineLayoutDescriptor): PipelineLayout {
        TODO("Not yet implemented")
    }

    actual fun createCommandEncoder(label: String?): CommandEncoder {
        TODO("Not yet implemented")
    }

    actual fun createBuffer(desc: BufferDescriptor): GPUBuffer {
        TODO("Not yet implemented")
    }

    actual fun createBindGroupLayout(desc: BindGroupLayoutDescriptor): BindGroupLayout {
        TODO("Not yet implemented")
    }

    actual fun createBindGroup(desc: BindGroupDescriptor): BindGroup {
        TODO("Not yet implemented")
    }

    actual fun createSampler(desc: SamplerDescriptor): Sampler {
        TODO("Not yet implemented")
    }

    actual fun createTexture(desc: TextureDescriptor): WebGPUTexture {
        TODO("Not yet implemented")
    }

    actual fun createGPUShortBuffer(
        label: String,
        data: ShortArray,
        usage: BufferUsage
    ): GPUBuffer {
        TODO("Not yet implemented")
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatArray,
        usage: BufferUsage
    ): GPUBuffer {
        TODO("Not yet implemented")
    }

    actual fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer {
        TODO("Not yet implemented")
    }

    actual fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer {
        TODO("Not yet implemented")
    }

    actual override fun release() {}
}

actual class Adapter : Releasable {
    actual suspend fun requestDevice(): Device {
        TODO("Not yet implemented")
    }

    actual override fun release() {}
}

actual class Queue : Releasable {
    actual fun submit(vararg cmdBuffers: CommandBuffer) {}

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteArray,
        offset: Long,
        dataOffset: Long,
        size: Long
    ) {}

    actual fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long
    ) {}

    actual fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long
    ) {}

    actual override fun release() {}
}

actual class ShaderModule : Releasable {
    actual override fun release() {}
}

actual class Surface : Releasable {
    actual fun configure(configuration: SurfaceConfiguration) {}

    actual fun getCapabilities(adapter: Adapter): SurfaceCapabilities {
        TODO("Not yet implemented")
    }

    actual fun getPreferredFormat(adapter: Adapter): TextureFormat {
        TODO("Not yet implemented")
    }

    actual fun getCurrentTexture(): SurfaceTexture {
        TODO("Not yet implemented")
    }

    actual fun present() {}

    actual override fun release() {}
}

actual class SurfaceCapabilities {
    actual val formats: List<TextureFormat>
        get() = TODO("Not yet implemented")

    actual val alphaModes: List<AlphaMode>
        get() = TODO("Not yet implemented")
}

actual class SurfaceTexture {
    actual val texture: WebGPUTexture?
        get() = TODO("Not yet implemented")

    actual val status: TextureStatus
        get() = TODO("Not yet implemented")
}

actual class WebGPUTexture : Releasable {
    actual fun createView(desc: TextureViewDescriptor?): TextureView {
        TODO("Not yet implemented")
    }

    actual override fun release() {}

    actual fun destroy() {}
}

actual class TextureView : IntoBindingResource {
    actual fun release() {}
}

actual class GPUBuffer : Releasable {
    actual val size: Long
        get() = TODO("Not yet implemented")

    actual fun getMappedRange(offset: Long, size: Long): ByteBuffer {
        TODO("Not yet implemented")
    }

    actual fun unmap() {}

    actual override fun release() {}

    actual fun destroy() {}
}

actual class Sampler : IntoBindingResource, Releasable {
    actual override fun release() {}
}
