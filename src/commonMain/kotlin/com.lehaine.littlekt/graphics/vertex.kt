package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.GL
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import kotlin.jvm.JvmInline

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
data class VertexAttribute(
    val usage: Usage,
    val numComponents: Int,
    val alias: String,
    val type: Int = if (usage == Usage.COLOR_PACKED) GL.UNSIGNED_BYTE else GL.FLOAT,
    val normalized: Boolean = usage == Usage.COLOR_PACKED,
    val unit: Int = 0,
) {
    val sizeInBytes: Int
        get() {
            return when (type) {
                GL.FLOAT, GL.FIXED -> 4 * numComponents
                GL.UNSIGNED_BYTE, GL.BYTE -> numComponents
                GL.UNSIGNED_SHORT, GL.SHORT -> 2 * numComponents
                else -> 0
            }
        }

    val key get() = (usageIndex shl 8) + (unit and 0xFF)

    internal var offset: Int = 0

    private val usageIndex = usage.usage.countTrailingZeroBits()

    companion object {
        val POSITION get() = VertexAttribute(Usage.POSITION, 3, alias = ShaderProgram.POSITION_ATTRIBUTE)
        fun TEX_COORDS(unit: Int) = VertexAttribute(Usage.TEX_COORDS, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + unit, unit)
        val NORMAL get() = VertexAttribute(Usage.NORMAL, 3, ShaderProgram.NORMAL_ATTRIBUTE)
        val COLOR_PACKED
            get() = VertexAttribute(
                Usage.COLOR_PACKED,
                4,
                ShaderProgram.COLOR_ATTRIBUTE,
                GL.UNSIGNED_BYTE,
                true
            )
        val COLOR_UNPACKED
            get() = VertexAttribute(
                Usage.COLOR_UNPACKED,
                4,
                ShaderProgram.COLOR_ATTRIBUTE,
                GL.FLOAT,
                false
            )
    }
}

class VertexAttributes(private vararg val attributes: VertexAttribute) : Iterable<VertexAttribute>,
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

    fun getOffsetOrDefault(usage: Usage, defaultOffsetIfNotFound: Int = 0): Int {
        return findByUsage(usage)?.offset?.div(4) ?: defaultOffsetIfNotFound
    }

    fun findByUsage(usage: Usage): VertexAttribute? {
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

    override fun compareTo(o: VertexAttributes): Int {
        if (attributes.size != o.attributes.size) return attributes.size - o.attributes.size
        val m1: Long = mask
        val m2: Long = o.mask
        if (m1 != m2) return if (m1 < m2) -1 else 1
        for (i in attributes.size - 1 downTo 0) {
            val (usage, numComponents, _, type, normalized, unit) = attributes[i]
            val (usage1, numComponents1, _, type1, normalized1, unit1) = o.attributes[i]
            if (usage != usage1) return usage.usage - usage1.usage
            if (unit != unit1) return unit - unit1
            if (numComponents != numComponents1) return numComponents - numComponents1
            if (normalized != normalized1) return if (normalized) 1 else -1
            if (type != type1) return type - type1
        }
        return 0
    }
}

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
@JvmInline
value class Usage(val usage: Int) {
    companion object {
        val POSITION = Usage(1)
        val COLOR_UNPACKED = Usage(2)
        val COLOR_PACKED = Usage(4)
        val NORMAL = Usage(8)
        val TEX_COORDS = Usage(16)
        val GENERIC = Usage(32)
    }
}