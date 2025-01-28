package com.littlekt.graphics

import io.ygdrasil.webgpu.sizeInBytes

/**
 * Calculates the stride of a list [VertexAttributeView] by summing up the bytes of
 * [VertexAttributeView.format].
 *
 * @see VertexFormat.bytes
 */
fun List<VertexAttributeView>.calculateStride(): Int = sumOf { it.format.sizeInBytes() }

/**
 * Calculates the number of components in the list of [VertexAttributeView] by summing up the components
 * of [VertexAttributeView.format].
 *
 * @see VertexFormat.components
 */
fun List<VertexAttributeView>.calculateComponents(): Int = sumOf { it.format.sizeInBytes() }
