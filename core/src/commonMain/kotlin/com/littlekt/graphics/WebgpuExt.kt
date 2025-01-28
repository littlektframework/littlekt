package com.littlekt.graphics

import io.ygdrasil.webgpu.BlendFactor
import io.ygdrasil.webgpu.BlendOperation
import io.ygdrasil.webgpu.Buffer
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.BufferUsage
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.RenderPipelineDescriptor.FragmentState.ColorTargetState.BlendState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.FragmentState.ColorTargetState.BlendState.BlendComponent


fun Device.createGPUShortBuffer(label: String, data: ShortArray, usages: Set<BufferUsage>): Buffer {
    val buffer = createBuffer(
        BufferDescriptor(
            label = label,
            size = (data.size * Short.SIZE_BYTES).toULong(),
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
            size = (data.size * Float.SIZE_BYTES).toULong(),
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
            size = (data.size * Byte.SIZE_BYTES).toULong(),
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
            color = BlendComponent(dstFactor = BlendFactor.OneMinusSrcAlpha),
            alpha = BlendComponent(dstFactor = BlendFactor.OneMinusSrcAlpha)
        )

    /** Fully oqaque, no alpha, blending. */
    val Opaque: BlendState
        get() = BlendState()


    /** Non-premultiplied, alpha blending. */
    val NonPreMultiplied: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.SrcAlpha,
                dstFactor = BlendFactor.OneMinusSrcAlpha
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.SrcAlpha,
                dstFactor = BlendFactor.OneMinusSrcAlpha
            )
        )

    val Add: BlendState
        get() = BlendState(
            color =
            BlendComponent(srcFactor = BlendFactor.SrcAlpha, dstFactor = BlendFactor.One),
            alpha =
            BlendComponent(srcFactor = BlendFactor.SrcAlpha, dstFactor = BlendFactor.One)
        )

    val Subtract: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.SrcAlpha,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.ReverseSubtract
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.SrcAlpha,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.ReverseSubtract
            ),
        )

    val Difference: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.OneMinusDst,
                dstFactor = BlendFactor.OneMinusSrc,
                operation = BlendOperation.Add
            ),
        )

    val Multiply: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.Dst,
                dstFactor = BlendFactor.Zero,
                operation = BlendOperation.Add
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.DstAlpha,
                dstFactor = BlendFactor.Zero,
                operation = BlendOperation.Add
            )
        )

    val Lighten: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.One,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.Max
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.One,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.Max
            ),
        )

    val Darken: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.One,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.Min
            ),
            alpha =
            BlendComponent(
                srcFactor = BlendFactor.One,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.Min
            ),
        )

    val Screen: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.OneMinusDst,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.Add
            )
        )

    val LinearDodge: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.One,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.Add
            )
        )

    val LinearBurn: BlendState
        get() = BlendState(
            color =
            BlendComponent(
                srcFactor = BlendFactor.One,
                dstFactor = BlendFactor.One,
                operation = BlendOperation.ReverseSubtract
            )
        )

}
