package com.lehaine.littlekt.file.font.ttf.internal

import com.lehaine.littlekt.file.font.ttf.TtfFont
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
class Glyph(
    var name: String? = null,
    unicode: Int? = null,
    unicodes: List<Int>? = null,
    val font: TtfFont,
    var index: Int,
    var xMin: Int = 0,
    var yMin: Int = 0,
    var xMax: Int = 0,
    var yMax: Int = 0,
    var advanceWidth: Float = 0f,
    var leftSideBearing: Int = 0
) {
    var numberOfContours: Int = 0
    val endPointIndices = mutableListOf<Int>()
    var instructionLength: Int = 0
    val instructions = mutableListOf<Byte>()
    val points = mutableListOf<Point>()
    val refs = mutableListOf<GlyphReference>()
    var isComposite: Boolean = false

    var codePoint: Int = -1
    var bounds: Rect? = null
    var calcPath: () -> Unit = {}
    private var pathCalculated = false
    var path: Path = Path()
        get() {
            if (!pathCalculated) {
                pathCalculated = true
                calcPath()
            }
            return field
        }

    private val unicodesMut = mutableListOf<Int>().also {
        if (unicodes != null) {
            it.addAll(unicodes)
        }
    }

    var unicode: Int = unicode ?: 0
    val unicodes: List<Int> get() = unicodesMut

    fun addUnicode(unicode: Int) {
        if (unicodes.isEmpty()) {
            this.unicode = unicode
        }

        unicodesMut.add(unicode)
    }

    override fun toString(): String {
        return "Glyph(name=$name, index=$index, xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax, advanceWidth=$advanceWidth, leftSideBearing=$leftSideBearing, numberOfContours=$numberOfContours, endPointIndices=$endPointIndices, instructionLength=$instructionLength, instructions=$instructions,\npoints=[\n${
            points.joinToString(
                separator = "\n"
            )
        }\n], refs=$refs, isComposite=$isComposite, unicode=$unicode, unicodes=$unicodes)"
    }
}

data class Point(
    var x: Int = 0,
    var y: Int = 0,
    var onCurve: Boolean = false,
    var lastPointOfContour: Boolean = false
)

data class GlyphReference(
    val glyphIndex: Int,
    var x: Int,
    var y: Int,
    var scaleX: Float,
    var scale01: Float,
    var scale10: Float,
    var scaleY: Float,
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