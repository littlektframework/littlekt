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

/** Standard alpha blending. */
val BlendState.Alpha: BlendState
    get() = BlendState(
        color = BlendComponent(dstFactor = BlendFactor.oneminussrcalpha),
        alpha = BlendComponent(dstFactor = BlendFactor.oneminussrcalpha)
    )

/** Fully oqaque, no alpha, blending. */
val BlendState.Opaque: BlendState
    get() = BlendState()


/** Non-premultiplied, alpha blending. */
val BlendState.NonPreMultiplied: BlendState
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

val BlendState.Add: BlendState
    get() = BlendState(
        color =
        BlendComponent(srcFactor = BlendFactor.srcalpha, dstFactor = BlendFactor.one),
        alpha =
        BlendComponent(srcFactor = BlendFactor.srcalpha, dstFactor = BlendFactor.one)
    )

val BlendState.Subtract: BlendState
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

val BlendState.Difference: BlendState
    get() = BlendState(
        color =
        BlendComponent(
            srcFactor = BlendFactor.ONE_MINUS_DST_COLOR,
            dstFactor = BlendFactor.ONE_MINUS_SRC_COLOR,
            operation = BlendOperation.add
        ),
    )

val BlendState.Multiply: BlendState
    get() = BlendState(
        color =
        BlendComponent(
            srcFactor = BlendFactor.DST_COLOR,
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

val BlendState.Lighten: BlendState
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

val BlendState.Darken: BlendState
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

val BlendState.Screen: BlendState
    get() = BlendState(
        color =
        BlendComponent(
            srcFactor = BlendFactor.ONE_MINUS_DST_COLOR,
            dstFactor = BlendFactor.one,
            operation = BlendOperation.add
        )
    )

val BlendState.LinearDodge: BlendState
    get() = BlendState(
        color =
        BlendComponent(
            srcFactor = BlendFactor.one,
            dstFactor = BlendFactor.one,
            operation = BlendOperation.add
        )
    )

val BlendState.LinearBurn: BlendState
    get() = BlendState(
        color =
        BlendComponent(
            srcFactor = BlendFactor.one,
            dstFactor = BlendFactor.one,
            operation = BlendOperation.reversesubtract
        )
    )