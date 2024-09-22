package com.littlekt.graphics

import io.ygdrasil.wgpu.VertexStepMode


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
data class VertexBufferLayout(
    val arrayStride: Long,
    val stepMode: VertexStepMode,
    val attributes: List<VertexAttribute>
) {
    /** The underlying GPU vertex buffer layout. */
    val gpuVertexBufferLayout =
        WebGPUVertexBufferLayout(arrayStride, stepMode, attributes.map { it.gpuVertexAttribute })
}
