package com.littlekt.graphics

import com.littlekt.graphics.util.CommonVertexView
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout.VertexAttribute
import io.ygdrasil.webgpu.VertexFormat

/**
 * Wraps a [VertexAttribute] and tracks a [VertexAttrUsage].
 *
 * @param format the format of the input
 * @param offset byte offset of the start of the input
 * @param shaderLocation location for this input. Must match the location in the shader.
 * @param usage the usage of the attribute. Used in certain situations where certain logic requires
 *   checking if an attribute exists in order to provide support. (E.g [CommonVertexView.position]
 *   for [VertexAttrUsage.POSITION]).
 * @author Colton Daily
 * @date 4/10/2024
 */
data class VertexAttributeView(
    val format: VertexFormat,
    val offset: Long,
    val shaderLocation: Int,
    val usage: VertexAttrUsage
) {
    private val usageIndex = usage.usage.countTrailingZeroBits()
    val gpuVertexAttribute =
        VertexAttribute(format, offset.toULong(), shaderLocation.toUInt())

    val key: Int = (usageIndex shl 8) + (shaderLocation and 0xFF)
}
