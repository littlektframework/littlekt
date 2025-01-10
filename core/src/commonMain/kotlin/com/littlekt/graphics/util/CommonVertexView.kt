package com.littlekt.graphics.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.log.Logger
import com.littlekt.math.MutableVec2f
import com.littlekt.math.MutableVec3f
import com.littlekt.math.MutableVec4f
import com.littlekt.math.MutableVec4i

/**
 * A [VertexView] that contains common attributes such as position, normals, color, texture coords,
 * etc.
 *
 * @param vertexSize the size of the vertex
 * @param vertices the raw vertices
 * @param attributes the attributes of the vertex
 * @param index the initial vertex index to view
 * @author Colton Daily
 * @date 4/10/2024
 */
class CommonVertexView(
    vertexSize: Int,
    vertices: FloatBuffer,
    attributes: List<VertexAttribute>,
    index: Int,
) : VertexView(vertexSize, vertices, attributes, index) {
    /** The position attribute vector view */
    val position: MutableVec3f =
        attributes.firstOrNull { it.usage == VertexAttrUsage.POSITION }?.let(::getVec3fAttribute)
            ?: Vec3fView(-1).also { logger.trace { "position view was not found." } }

    /** The normal attribute vector view. */
    val normal: MutableVec3f =
        attributes.firstOrNull { it.usage == VertexAttrUsage.NORMAL }?.let(::getVec3fAttribute)
            ?: Vec3fView(-1).also { logger.trace { "normal view was not found." } }

    /** The color attribute color view. */
    val color: MutableColor =
        attributes.firstOrNull { it.usage == VertexAttrUsage.COLOR }?.let(::getColorAttribute)
            ?: ColorWrapView(Vec4fView(-1)).also { logger.trace { "color view was not found." } }

    /** The uv texture coords attribute vector view. */
    val uv: MutableVec2f =
        attributes.firstOrNull { it.usage == VertexAttrUsage.UV }?.let(::getVec2fAttribute)
            ?: Vec2fView(-1).also { logger.trace { "texCoords view was not found." } }

    /** The joints attribute vector view. */
    val joints: MutableVec4i =
        attributes.firstOrNull { it.usage == VertexAttrUsage.JOINT }?.let(::getVec4iAttribute)
            ?: Vec4iView(-1).also { logger.trace { "joints view was not found." } }

    /** The weights attribute vector view. */
    val weights: MutableVec4f =
        attributes.firstOrNull { it.usage == VertexAttrUsage.WEIGHT }?.let(::getVec4fAttribute)
            ?: Vec4fView(-1).also { logger.trace { "weights view was not found." } }

    /** The tangent attribute vector view. */
    val tangent: MutableVec4f =
        attributes.firstOrNull { it.usage == VertexAttrUsage.TANGENT }?.let(::getVec4fAttribute)
            ?: Vec4fView(-1).also { logger.trace { "tangent view was not found." } }

    /**
     * Resets all view values to zero except for [color] which is set to all ones: `(1f, 1f, 1f,
     * 1f)`
     */
    override fun resetToZero() {
        super.resetToZero()
        color.set(1f, 1f, 1f, 1f)
    }

    companion object {
        private val logger = Logger<CommonVertexView>()
    }
}
