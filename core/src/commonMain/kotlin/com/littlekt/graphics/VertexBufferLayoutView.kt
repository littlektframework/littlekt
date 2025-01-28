package com.littlekt.graphics

import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.VertexStepMode


/**
 * A wrapper around a [WebGPUVertexBufferLayout] describes how the vertex buffer is interpreted.
 *
 * For use in [VertexState].
 *
 * @param arrayStride the stride, in bytes, between elements of this buffer.
 * @param stepMode how often this vertex buffer is "stepped" forward.
 * @param attributes the list of attributes which comprise a single vertex.
 * @author Colton Daily
 * @date 4/10/2024
 */
data class VertexBufferLayoutView(
    val arrayStride: Long,
    val stepMode: VertexStepMode,
    val attributes: List<VertexAttributeView>
) {
    /** The underlying GPU vertex buffer layout. */
    val gpuVertexBufferLayout =
        RenderPipelineDescriptor.VertexState.VertexBufferLayout(
            arrayStride.toULong(),
            attributes.map { it.gpuVertexAttribute },
            stepMode)
}
