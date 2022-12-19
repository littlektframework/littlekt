package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.Vec4f
import com.lehaine.littlekt.util.datastructure.ShortArrayList

/**
 * @author Colton Daily
 * @date 12/19/2022
 */
class MeshGeometry(val size: Int, val attributes: VertexAttributes) {
    val vertexSize = attributes.sumOf { if (it == VertexAttribute.COLOR_PACKED) 1 else it.numComponents }
    val vertices = FloatArray(size * vertexSize)
    val indices = ShortArrayList(1000)
    var lastCount = 0
        private set
    var count = 0
        private set
    var offset = 0
        private set

    fun setVertex(props: VertexProps) {
        attributes.forEach { vertexAttribute ->
            when (vertexAttribute.usage) {
                VertexAttrUsage.POSITION -> {
                    vertices[offset++] = props.x
                    vertices[offset++] = props.y
                    if (vertexAttribute.numComponents >= 3) {
                        vertices[offset++] = props.z
                    }
                    if (vertexAttribute.numComponents == 4) {
                        vertices[offset++] = props.w
                    }
                }

                VertexAttrUsage.COLOR_UNPACKED -> {
                    vertices[offset++] = props.color.r
                    vertices[offset++] = props.color.g
                    vertices[offset++] = props.color.b
                    vertices[offset++] = props.color.a
                }

                VertexAttrUsage.COLOR_PACKED -> {
                    vertices[offset++] = props.colorPacked
                }

                VertexAttrUsage.TEX_COORDS -> {
                    vertices[offset++] = props.u
                    vertices[offset++] = props.v
                }

                VertexAttrUsage.GENERIC -> {
                    for (i in 0 until vertexAttribute.numComponents) {
                        vertices[offset++] = props.generic[i]
                    }
                }
            }
        }
        count++
    }

    fun add(newVertices: FloatArray, srcOffset: Int, dstOffset: Int, count: Int) {
        newVertices.copyInto(vertices, dstOffset, srcOffset, srcOffset + count)
        this.offset = dstOffset + count
        this.count = offset / vertexSize
    }

    fun add(data: Vec2f) {
        vertices[offset++] = data.x
        vertices[offset++] = data.y
    }

    fun add(data: Vec3f) {
        vertices[offset++] = data.x
        vertices[offset++] = data.y
        vertices[offset++] = data.z
    }

    fun add(data: Vec4f) {
        vertices[offset++] = data.x
        vertices[offset++] = data.y
        vertices[offset++] = data.z
        vertices[offset++] = data.w
    }

    fun add(vertex: Float) {
        vertices[offset++] = vertex
    }

    fun set(offset: Int, vertex: Float) {
        vertices[offset] = vertex
    }

    fun get(offset: Int) = vertices[offset]

    fun addIndex(idx: Int) {
        indices += idx.toShort()
    }

    fun addIndices(vararg indices: Int) {
        for (idx in indices.indices) {
            addIndex(indices[idx])
        }
    }

    fun addIndices(indices: List<Int>) {
        for (idx in indices.indices) {
            addIndex(indices[idx])
        }
    }

    fun addTriIndices(i0: Int, i1: Int, i2: Int) {
        addIndex(i0)
        addIndex(i1)
        addIndex(i2)
    }


    fun skip(totalVertices: Int) {
        offset += totalVertices * vertexSize
        count += totalVertices
    }

    fun reset() {
        lastCount = count
        offset = 0
        count = 0
    }
}