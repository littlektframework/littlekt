package com.lehaine.littlekt.file.font.ttf.table

import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.font.ttf.Encoding
import com.lehaine.littlekt.file.font.ttf.Parser

/**
 * The `post` table stores additional PostScript information, such as glyph names.
 * https://www.microsoft.com/typography/OTSPEC/post.htm
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class PostParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): Post {
        val p = Parser(buffer, start)
        val post = MutablePost().apply {
            version = p.parseVersion()
            italicAngle = p.parseFixed
            underlinePosition = p.parseInt16.toInt()
            underlineThickness = p.parseInt16.toInt()
            isFixedPitch = p.parseUint32
            minMemType42 = p.parseUint32
            maxMemType42 = p.parseUint32
            minMemType1 = p.parseUint32
            maxMemType1 = p.parseUint32
            when (version) {
                1f -> names = Encoding.STANDARD_NAMES.copyOf()
                2f -> {
                    numberOfGlyphs = p.parseUint16
                    glyphNameIndex = IntArray(numberOfGlyphs) { p.parseUint16 }

                    val nameList = mutableListOf<String>()
                    for (i in 0 until numberOfGlyphs) {
                        if (glyphNameIndex[i] >= Encoding.STANDARD_NAMES.size) {
                            val nameLength = p.parseUByte
                            nameList += p.parseString(nameLength)
                        }
                    }
                    names = nameList.toTypedArray()
                }
                2.5f -> {
                    numberOfGlyphs = p.parseUint16
                    offset = CharArray(numberOfGlyphs) { p.parseChar }
                }

            }

        }
        return post.toPost()
    }

}

private class MutablePost {
    var version: Float = 0f
    var italicAngle: Float = 0f
    var underlinePosition: Int = 0
    var underlineThickness: Int = 0
    var isFixedPitch: Int = 0
    var minMemType42: Int = 0
    var maxMemType42: Int = 0
    var minMemType1: Int = 0
    var maxMemType1: Int = 0
    var names: Array<String> = arrayOf()
    var numberOfGlyphs: Int = 0
    var glyphNameIndex: IntArray = intArrayOf()
    var offset: CharArray = charArrayOf()

    fun toPost() = Post(
        version,
        italicAngle,
        underlinePosition,
        underlineThickness,
        isFixedPitch,
        minMemType42,
        maxMemType42,
        minMemType1,
        maxMemType1,
        names,
        numberOfGlyphs,
        glyphNameIndex,
        offset
    )
}

/**
 * The `post` table stores additional PostScript information, such as glyph names.
 * https://www.microsoft.com/typography/OTSPEC/post.htm
 * @author Colton Daily
 * @date 11/30/2021
 */
internal data class Post(
    val version: Float,
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
        var result = version.toInt()
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

    override fun toString(): String {
        return "Post(version=$version, italicAngle=$italicAngle, underlinePosition=$underlinePosition, underlineThickness=$underlineThickness, isFixedPitch=$isFixedPitch, minMemType42=$minMemType42, maxMemType42=$maxMemType42, minMemType1=$minMemType1, maxMemType1=$maxMemType1, numberOfGlyphs=$numberOfGlyphs)"
    }


}