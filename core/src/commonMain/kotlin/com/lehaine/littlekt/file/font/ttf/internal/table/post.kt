package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class PostParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): Post {
        TODO()
    }

}

internal data class Post(
    val version: Int,
    val italicAngle: Float,
    val underlinePosition: Int,
    val underlineThickness: Int,
    val isFixedPitch: Int,
    val minMemType42: Int,
    val maxMemType42: Int,
    val minMemType1: Int,
    val maxMemType1: Int,
    val names: Array<String>,
    val numberOfGlyphs: Int,
    val glyphNameIndex: IntArray,
    val offset: CharArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Post

        if (version != other.version) return false
        if (italicAngle != other.italicAngle) return false
        if (underlinePosition != other.underlinePosition) return false
        if (underlineThickness != other.underlineThickness) return false
        if (isFixedPitch != other.isFixedPitch) return false
        if (minMemType42 != other.minMemType42) return false
        if (maxMemType42 != other.maxMemType42) return false
        if (minMemType1 != other.minMemType1) return false
        if (maxMemType1 != other.maxMemType1) return false
        if (!names.contentEquals(other.names)) return false
        if (numberOfGlyphs != other.numberOfGlyphs) return false
        if (!glyphNameIndex.contentEquals(other.glyphNameIndex)) return false
        if (!offset.contentEquals(other.offset)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + italicAngle.hashCode()
        result = 31 * result + underlinePosition
        result = 31 * result + underlineThickness
        result = 31 * result + isFixedPitch
        result = 31 * result + minMemType42
        result = 31 * result + maxMemType42
        result = 31 * result + minMemType1
        result = 31 * result + maxMemType1
        result = 31 * result + names.contentHashCode()
        result = 31 * result + numberOfGlyphs
        result = 31 * result + glyphNameIndex.contentHashCode()
        result = 31 * result + offset.contentHashCode()
        return result
    }
}