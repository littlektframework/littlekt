package com.littlekt.graphics

import io.ygdrasil.wgpu.Buffer
import io.ygdrasil.wgpu.BufferDescriptor
import io.ygdrasil.wgpu.BufferUsage
import io.ygdrasil.wgpu.Device


internal fun Device.createGPUShortBuffer(label: String, data: ShortArray, usages: Set<BufferUsage>): Buffer {
    val buffer = createBuffer(
        BufferDescriptor(
            label = label,
            size = data.size.toLong() * Short.SIZE_BYTES,
            usage = usages,
            mappedAtCreation = true
        )
    )
    buffer.mapFrom(data)
    buffer.unmap()

    return buffer
}


internal fun Device.createGPUFloatBuffer(label: String, data: FloatArray, usages: Set<BufferUsage>): Buffer {
    val buffer = createBuffer(
        BufferDescriptor(
            label = label,
            size = data.size.toLong() * Short.SIZE_BYTES,
            usage = usages,
            mappedAtCreation = true
        )
    )
    buffer.mapFrom(data)
    buffer.unmap()

    return buffer
}
