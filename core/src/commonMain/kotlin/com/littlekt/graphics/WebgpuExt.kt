package com.littlekt.graphics

import io.ygdrasil.wgpu.BlendFactor
import io.ygdrasil.wgpu.BlendOperation
import io.ygdrasil.wgpu.Buffer
import io.ygdrasil.wgpu.BufferDescriptor
import io.ygdrasil.wgpu.BufferUsage
import io.ygdrasil.wgpu.Device
import io.ygdrasil.wgpu.RenderPipelineDescriptor.FragmentState.ColorTargetState.BlendState
import io.ygdrasil.wgpu.RenderPipelineDescriptor.FragmentState.ColorTargetState.BlendState.BlendComponent


fun Device.createGPUShortBuffer(label: String, data: ShortArray, usages: Set<BufferUsage>): Buffer {
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


fun Device.createGPUFloatBuffer(label: String, data: FloatArray, usages: Set<BufferUsage>): Buffer {
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

fun Device.createGPUByteBuffer(label: String, data: ByteArray, usages: Set<BufferUsage>): Buffer {
    val buffer = createBuffer(
        BufferDescriptor(
            label = label,
            size = data.size.toLong() * Byte.SIZE_BYTES,
            usage = usages,
            mappedAtCreation = true
        )
    )
    buffer.mapFrom(data)
    buffer.unmap()

    return buffer
}

object BlendStates {
    /** Standard alpha blending. */
    val Alpha: BlendState
        get() = BlendState(
            color = BlendComponent(dstFactor = BlendFactor.oneminussrcalpha),
            alpha = BlendComponent(dstFactor = BlendFactor.oneminussrcalpha)
        )

    /** Fully oqaque, no alpha, blending. */
    val Opaque: BlendState
        get() = BlendState()


    /** Non-premultiplied, alpha blending. */
    val NonPreMultiplied: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.srcalpha,
                dstFactor = BlendFactor.oneminussrcalpha
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.srcalpha,
                dstFactor = BlendFactor.oneminussrcalpha
            )
        )

    val Add: BlendState
        get() = BlendState(
            color =
            BlendComponent(srcFactor = BlendFactor.srcalpha, dstFactor = BlendFactor.one),
            alpha =
            BlendComponent(srcFactor = BlendFactor.srcalpha, dstFactor = BlendFactor.one)
        )

    val Subtract: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.srcalpha,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.reversesubtract
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.srcalpha,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.reversesubtract
            ),
        )

    val Difference: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.oneminusdst,
                dstFactor = BlendFactor.oneminussrc,
                operation = BlendOperation.add
            ),
        )

    val Multiply: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.dst,
                dstFactor = BlendFactor.zero,
                operation = BlendOperation.add
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.dstalpha,
                dstFactor = BlendFactor.zero,
                operation = BlendOperation.add
            )
        )

    val Lighten: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.one,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.max
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.one,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.max
            ),
        )

    val Darken: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.one,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.min
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.one,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.min
            ),
        )

    val Screen: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.oneminusdst,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.add
            )
        )

    val LinearDodge: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.one,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.add
            )
        )

    val LinearBurn: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.one,
                dstFactor = BlendFactor.one,
                operation = BlendOperation.reversesubtract
            )
        )

}
