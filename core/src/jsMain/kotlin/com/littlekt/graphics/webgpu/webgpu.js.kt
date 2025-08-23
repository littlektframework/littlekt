package com.littlekt.graphics.webgpu

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.toUint8Array

actual fun nativeSubmit(delegate: GPUQueue,vararg cmdBuffers: CommandBuffer) {
    delegate.submit(cmdBuffers.map { it.delegate }.toJsArray())
}

actual fun nativeWriteTexture(
    delegate: GPUQueue,
    data: ByteArray,
    destination: TextureCopyView,
    layout: TextureDataLayout,
    copySize: Extent3D,
    size: Long
) {
    val arrayBuffer = Uint8Array(ArrayBuffer(data.size)).apply { set(data.asUByteArray().toUint8Array()) }

    delegate.writeTexture(
        destination.toNative(),
        arrayBuffer,
        layout.toNative(),
        copySize.toNative(),
    )
}

actual fun nativeSizeOf(array: ByteArray): Int = array.size
actual fun nativeSizeOf(array: IntArray): Int = array.size
actual fun nativeSizeOf(array: FloatArray): Int = array.size
actual fun nativeSizeOf(array: ShortArray): Int = array.size