package com.lehaine.littlekt.graphics.font

data class GlyphReference(
    val glyphIndex: Int,
    val x: Int,
    val y: Int,
    val scaleX: Float,
    val scale01: Float,
    val scale10: Float,
    val scaleY: Float,
    val matchedPoints: IntArray = IntArray(2)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GlyphReference

        if (glyphIndex != other.glyphIndex) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (scaleX != other.scaleX) return false
        if (scale01 != other.scale01) return false
        if (scale10 != other.scale10) return false
        if (scaleY != other.scaleY) return false
        if (!matchedPoints.contentEquals(other.matchedPoints)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = glyphIndex
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + scaleX.hashCode()
        result = 31 * result + scale01.hashCode()
        result = 31 * result + scale10.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + matchedPoints.contentHashCode()
        return result
    }
}