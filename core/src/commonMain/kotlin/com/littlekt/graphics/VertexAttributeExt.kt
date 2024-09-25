package com.littlekt.graphics

/**
 * Calculates the stride of a list [VertexAttribute] by summing up the bytes of
 * [VertexAttribute.format].
 *
 * @see VertexFormat.bytes
 */
fun List<VertexAttribute>.calculateStride(): Int = sumOf { it.format.sizeInByte }

/**
 * Calculates the number of components in the list of [VertexAttribute] by summing up the components
 * of [VertexAttribute.format].
 *
 * @see VertexFormat.components
 */
fun List<VertexAttribute>.calculateComponents(): Int = sumOf { it.format.components }
