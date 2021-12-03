package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.gl.VertexAttrType
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import kotlin.jvm.JvmInline

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
data class VertexAttribute(
    val usage: VertexAttrUsage,
    val numComponents: Int,
    val alias: String,
    val type: VertexAttrType = if (usage == VertexAttrUsage.COLOR_PACKED) VertexAttrType.UNSIGNED_BYTE else VertexAttrType.FLOAT,
    val normalized: Boolean = usage == VertexAttrUsage.COLOR_PACKED,
    val unit: Int = 0,
) {
    val sizeInBytes: Int
        get() {
            return when (type) {
                VertexAttrType.FLOAT, VertexAttrType.FIXED -> 4 * numComponents
                VertexAttrType.UNSIGNED_BYTE, VertexAttrType.BYTE -> numComponents
                VertexAttrType.UNSIGNED_SHORT, VertexAttrType.SHORT -> 2 * numComponents
                else -> 0
            }
        }

    val key get() = (usageIndex shl 8) + (unit and 0xFF)

    internal var offset: Int = 0

    private val usageIndex = usage.usage.countTrailingZeroBits()

    companion object {
        val POSITION_2D
            get() = VertexAttribute(
                usage = VertexAttrUsage.POSITION,
                numComponents = 2,
                alias = ShaderProgram.POSITION_ATTRIBUTE
            )

        val POSITION_VEC3
            get() = VertexAttribute(
                usage = VertexAttrUsage.POSITION,
                numComponents = 3,
                alias = ShaderProgram.POSITION_ATTRIBUTE
            )

        val POSITION_VEC4
            get() = VertexAttribute(
                usage = VertexAttrUsage.POSITION,
                numComponents = 4,
                alias = ShaderProgram.POSITION_ATTRIBUTE
            )

        fun TEX_COORDS(unit: Int = 0) =
            VertexAttribute(
                usage = VertexAttrUsage.TEX_COORDS,
                numComponents = 2,
                alias = ShaderProgram.TEXCOORD_ATTRIBUTE + unit,
                unit = unit
            )

        val NORMAL
            get() = VertexAttribute(
                usage = VertexAttrUsage.NORMAL,
                numComponents = 3,
                alias = ShaderProgram.NORMAL_ATTRIBUTE
            )
        val COLOR_PACKED
            get() = VertexAttribute(
                usage = VertexAttrUsage.COLOR_PACKED,
                numComponents = 4,
                alias = ShaderProgram.COLOR_ATTRIBUTE,
                type = VertexAttrType.UNSIGNED_BYTE,
                normalized = true
            )
        val COLOR_UNPACKED
            get() = VertexAttribute(
                usage = VertexAttrUsage.COLOR_UNPACKED,
                numComponents = 4,
                alias = ShaderProgram.COLOR_ATTRIBUTE,
                type = VertexAttrType.FLOAT,
                normalized = false
            )
    }
}

class VertexAttributes(private val attributes: List<VertexAttribute>) : Iterable<VertexAttribute>,
    Comparable<VertexAttributes> {
    val vertexSize = calculateOffsets()
    val size get() = attributes.size

    private var _mask: Long = -1

    val mask: Long
        get() {
            if (_mask == -1L) {
                var result = 0L
                attributes.forEach {
                    result = result or it.usage.usage.toLong()
                }
                _mask = result
            }
            return _mask
        }

    val maskWithSizePacked get() = mask or ((attributes.size shl 32).toLong())

    fun getOffsetOrDefault(usage: VertexAttrUsage, defaultOffsetIfNotFound: Int = 0): Int {
        return findByUsage(usage)?.offset?.div(4) ?: defaultOffsetIfNotFound
    }

    fun findByUsage(usage: VertexAttrUsage): VertexAttribute? {
        for (i in 0 until size) {
            if (attributes[i].usage == usage) {
                return attributes[i]
            }
        }
        return null
    }


    operator fun get(index: Int) = attributes[index]

    private fun calculateOffsets(): Int {
        var count = 0
        attributes.forEach {
            it.offset = count
            count += it.sizeInBytes
        }
        return count
    }

    override fun iterator(): Iterator<VertexAttribute> {
        return attributes.iterator()
    }

    override fun compareTo(other: VertexAttributes): Int {
        if (attributes.size != other.attributes.size) return attributes.size - other.attributes.size
        val m1: Long = mask
        val m2: Long = other.mask
        if (m1 != m2) return if (m1 < m2) -1 else 1
        for (i in attributes.size - 1 downTo 0) {
            val (usage, numComponents, _, type, normalized, unit) = attributes[i]
            val (usage1, numComponents1, _, type1, normalized1, unit1) = other.attributes[i]
            if (usage != usage1) return usage.usage - usage1.usage
            if (unit != unit1) return unit - unit1
            if (numComponents != numComponents1) return numComponents - numComponents1
            if (normalized != normalized1) return if (normalized) 1 else -1
            if (type != type1) return type.glFlag - type1.glFlag
        }
        return 0
    }
}

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
@JvmInline
value class VertexAttrUsage(val usage: Int) {
    companion object {
        val POSITION = VertexAttrUsage(1)
        val COLOR_UNPACKED = VertexAttrUsage(2)
        val COLOR_PACKED = VertexAttrUsage(4)
        val NORMAL = VertexAttrUsage(8)
        val TEX_COORDS = VertexAttrUsage(16)
        val GENERIC = VertexAttrUsage(32)
    }
}