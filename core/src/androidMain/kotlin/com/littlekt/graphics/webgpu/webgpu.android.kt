package com.littlekt.graphics.webgpu

import com.littlekt.file.ByteBuffer
import com.littlekt.file.ByteBufferImpl
import com.littlekt.file.FloatBuffer
import com.littlekt.file.FloatBufferImpl
import com.littlekt.file.GenericBuffer
import com.littlekt.file.IntBuffer
import com.littlekt.file.IntBufferImpl
import com.littlekt.file.ShortBuffer
import com.littlekt.file.ShortBufferImpl
import com.sun.jna.Pointer
import java.lang.foreign.MemorySegment
import ffi.NativeAddress
import ffi.memoryScope
import io.ygdrasil.wgpu.WGPUBuffer
import io.ygdrasil.wgpu.WGPUTexture
import io.ygdrasil.wgpu.wgpuBufferGetMappedRange
import io.ygdrasil.wgpu.wgpuQueueWriteBuffer
import io.ygdrasil.wgpu.wgpuQueueWriteTexture

internal actual fun nativeMappedRange(
    segment: WGPUBuffer, offset: Long, size: Long
): ByteBuffer {
    val ptr: NativeAddress =
        wgpuBufferGetMappedRange(segment, offset.toULong(), size.toULong()) ?: error("Failed to get mapped range")
    
    val seg = MemorySegment(ptr, size)
    return ByteBufferImpl(size.toInt(), segment = seg)
}

actual fun Queue.nativeWriteTexture(
    data: ByteBuffer, destination: TextureCopyView, size: Long, layout: TextureDataLayout, copySize: Extent3D
) {
    data as ByteBufferImpl
    memoryScope { scope ->
        wgpuQueueWriteTexture(
            segment,
            destination.toNative(scope),
            data.segment.toPointer(),
            size.toULong(),
            layout.toNative(scope),
            copySize.toNative(scope),
        )
    }
}

private fun MemorySegment.toPointer() = this.pointer

private fun dataBuffer(dataOffset: Long, data: GenericBuffer<*>): NativeAddress =
    if (dataOffset > 0) data.segment.asSlice(dataOffset).pointer else data.segment.pointer

actual fun Queue.nativeWriteIntBuffer(
    buffer: GPUBuffer, offset: Long, dataOffset: Long, data: IntBuffer, size: Long
) {
    data as IntBufferImpl
    wgpuQueueWriteBuffer(
        segment,
        buffer.segment,
        offset.toULong(),
        dataBuffer(dataOffset, data),
        (size * Int.SIZE_BYTES).toULong(),
    )
}

actual fun Queue.nativeWriteFloatBuffer(
    data: FloatBuffer, buffer: GPUBuffer, offset: Long, dataOffset: Long, size: Long
) {
    data as FloatBufferImpl
    wgpuQueueWriteBuffer(
        segment,
        buffer.segment,
        offset.toULong(),
        dataBuffer(dataOffset, data),
        (size * Float.SIZE_BYTES).toULong(),
    )
}

actual fun Queue.nativeWriteByteBuffer(
    data: ByteBuffer, buffer: GPUBuffer, offset: Long, dataOffset: Long, size: Long
) {
    data as ByteBufferImpl
    wgpuQueueWriteBuffer(
        segment,
        buffer.segment,
        offset.toULong(),
        dataBuffer(dataOffset, data),
        size.toULong(),
    )
}

actual fun Queue.nativeWriteShortBuffer(
    data: ShortBuffer, buffer: GPUBuffer, offset: Long, dataOffset: Long, size: Long
) {
    data as ShortBufferImpl

    wgpuQueueWriteBuffer(
        segment,
        buffer.segment,
        offset.toULong(),
        dataBuffer(dataOffset, data),
        (size * Short.SIZE_BYTES).toULong(),
    )
}

actual fun getNativeWebGPUTextureByteSize(texture: WGPUTexture): Long {
    return 0L // same as webMain impl
}