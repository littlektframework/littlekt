package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.*
import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

actual class Device(val delegate: GPUDevice) : Releasable {

    actual fun createSampler(desc: SamplerDescriptor): Sampler {
        return Sampler(delegate.createSampler(desc.toNative()))
    }

    actual fun createTexture(desc: TextureDescriptor): WebGPUTexture {
        val textureSize =
            (desc.size.width * desc.size.height * desc.size.depth * desc.format.bytes).toLong()
        return WebGPUTexture(delegate.createTexture(desc.toNative()), textureSize)
    }

    actual override fun release() {}
}

actual class Adapter : Releasable {

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

actual class WebGPUTexture(val delegate: GPUTexture, size: Long) : Releasable {

    private val info = TextureResourceInfo(this, size)

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
