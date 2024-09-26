package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.*
import com.littlekt.log.Logger
import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import com.littlekt.wgpu.*
import com.littlekt.wgpu.WGPU.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout

actual class Device(val segment: MemorySegment) : Releasable {

    actual fun createSampler(desc: SamplerDescriptor): Sampler {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUSamplerDescriptor.allocate(scope)

            WGPUSamplerDescriptor.label(descriptor, desc.label?.toNativeString(scope) ?: WGPU_NULL)
            WGPUSamplerDescriptor.addressModeU(descriptor, desc.addressModeU.nativeVal)
            WGPUSamplerDescriptor.addressModeV(descriptor, desc.addressModeV.nativeVal)
            WGPUSamplerDescriptor.addressModeW(descriptor, desc.addressModeW.nativeVal)
            WGPUSamplerDescriptor.magFilter(descriptor, desc.magFilter.nativeVal)
            WGPUSamplerDescriptor.minFilter(descriptor, desc.minFilter.nativeVal)
            WGPUSamplerDescriptor.mipmapFilter(descriptor, desc.mipmapFilter.nativeVal)
            WGPUSamplerDescriptor.lodMinClamp(descriptor, desc.lodMinClamp)
            WGPUSamplerDescriptor.lodMaxClamp(descriptor, desc.lodMaxClamp)
            WGPUSamplerDescriptor.compare(
                descriptor,
                desc.compare?.nativeVal ?: WGPUCompareFunction_Undefined(),
            )
            WGPUSamplerDescriptor.maxAnisotropy(descriptor, desc.maxAnisotropy)

            Sampler(wgpuDeviceCreateSampler(segment, descriptor))
        }
    }

    actual fun createTexture(desc: TextureDescriptor): WebGPUTexture {
        return Arena.ofConfined().use { scope ->
            val descriptor = WGPUTextureDescriptor.allocate(scope)
            val size = WGPUTextureDescriptor.size(descriptor)

            WGPUTextureDescriptor.label(descriptor, desc.label?.toNativeString(scope) ?: WGPU_NULL)
            WGPUTextureDescriptor.usage(descriptor, desc.usage.usageFlag)
            WGPUTextureDescriptor.dimension(descriptor, desc.dimension.nativeVal)
            WGPUTextureDescriptor.format(descriptor, desc.format.nativeVal)
            WGPUTextureDescriptor.mipLevelCount(descriptor, desc.mipLevelCount)
            WGPUTextureDescriptor.sampleCount(descriptor, desc.sampleCount)
            WGPUExtent3D.width(size, desc.size.width)
            WGPUExtent3D.height(size, desc.size.height)
            WGPUExtent3D.depthOrArrayLayers(size, desc.size.depth)

            val textureSize =
                (desc.size.width * desc.size.height * desc.size.depth * desc.format.bytes).toLong()
            WebGPUTexture(wgpuDeviceCreateTexture(segment, descriptor), textureSize)
        }
    }

    actual override fun release() {
        wgpuDeviceRelease(segment)
    }

    override fun toString(): String {
        return "Device"
    }
}

actual class Adapter(var segment: MemorySegment) : Releasable {

    actual override fun release() {
        wgpuAdapterRelease(segment)
    }

    override fun toString(): String {
        return "Adapter"
    }

    companion object {
        private val logger = Logger<Adapter>()
    }
}

actual class Queue(val segment: MemorySegment) : Releasable {

    actual fun submit(vararg cmdBuffers: CommandBuffer) {
        Arena.ofConfined().use { scope ->
            wgpuQueueSubmit(
                segment,
                cmdBuffers.size.toLong(),
                cmdBuffers.map { it.segment }.toNativeArray(scope),
            )
        }
    }

    actual fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        data as ByteBufferImpl
        Arena.ofConfined().use { scope ->
            wgpuQueueWriteTexture(
                segment,
                destination.toNative(scope),
                data.segment,
                size,
                layout.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as ShortBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size * Short.SIZE_BYTES,
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as IntBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size * Int.SIZE_BYTES,
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as FloatBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size * Float.SIZE_BYTES,
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as ByteBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset,
            if (dataOffset > 0) data.segment.asSlice(dataOffset) else data.segment,
            size,
        )
    }

    actual fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        Arena.ofConfined().use { scope ->
            wgpuQueueWriteTexture(
                segment,
                destination.toNative(scope),
                scope.allocateFrom(ValueLayout.JAVA_BYTE, *data),
                size,
                layout.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual override fun release() {
        wgpuQueueRelease(segment)
    }

    override fun toString(): String {
        return "Queue"
    }
}

actual class ShaderModule(val segment: MemorySegment) : Releasable {
    actual override fun release() {
        wgpuShaderModuleRelease(segment)
    }

    override fun toString(): String {
        return "ShaderModule"
    }
}

actual class WebGPUTexture(val segment: MemorySegment, val size: Long) : Releasable {

    private val info = TextureResourceInfo(this, size)

    actual override fun release() {
        wgpuTextureRelease(segment)
        info.delete()
    }

    actual fun destroy() {
        wgpuTextureDestroy(segment)
        info.delete()
    }
}

actual class TextureView(val segment: MemorySegment) : IntoBindingResource {

    override fun intoBindingResource(entry: MemorySegment) {
        WGPUBindGroupEntry.textureView(entry, segment)
    }

    actual fun release() {
        wgpuTextureViewRelease(segment)
    }
}

actual class GPUBuffer(val segment: MemorySegment, actual val size: Long) : Releasable {

    private val info = BufferResourceInfo(this, size)

    actual fun getMappedRange(offset: Long, size: Long): ByteBuffer {
        val mappedRange = wgpuBufferGetMappedRange(segment, offset, size).asSlice(offset, size)
        return ByteBufferImpl(mappedRange.byteSize().toInt(), segment = mappedRange)
    }

    actual fun getMappedRange(): ByteBuffer = getMappedRange(0, size)

    actual fun unmap() {
        wgpuBufferUnmap(segment)
    }

    actual override fun release() {
        wgpuBufferRelease(segment)
    }

    actual fun destroy() {
        wgpuBufferDestroy(segment)
        info.delete()
    }
}

actual class Sampler(val segment: MemorySegment) : IntoBindingResource, Releasable {

    override fun intoBindingResource(entry: MemorySegment) {
        WGPUBindGroupEntry.sampler(entry, segment)
    }

    actual override fun release() {
        wgpuSamplerRelease(segment)
    }
}

fun Extent3D.toNative(scope: SegmentAllocator): MemorySegment {
    val native = WGPUExtent3D.allocate(scope)

    WGPUExtent3D.width(native, width)
    WGPUExtent3D.height(native, height)
    WGPUExtent3D.depthOrArrayLayers(native, depth)

    return native
}


