package com.lehaine.littlekt.graphics.font

data class Glyph(
    val name: String?,
    val index: Int,
    val xMin: Int,
    val yMin: Int,
    val xMax: Int,
    val yMax: Int,
    val advanceWidth: Float,
    val leftSideBearing: Int,
    var numberOfContours: Int,
    val unicode: Int,
    val unicodes: List<Int>,
    val path: GlyphPath,
    val endPointIndices: List<Int>,
    val instructionLength: Int,
    val instructions: List<Byte>,
    val points: List<ContourPoint>,
    val refs: List<GlyphReference>,
    val isComposite: Boolean,
    val unitsPerEm: Int
) {
    val width: Int = xMax - xMin
    val height: Int = yMax - yMin
    val rightSideBearing: Int = advanceWidth.toInt() - leftSideBearing - (xMax - xMin)

    override fun toString(): String {
        return "Glyph(name=$name, index=$index, xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax, advanceWidth=$advanceWidth, leftSideBearing=$leftSideBearing, numberOfContours=$numberOfContours, unicode=$unicode, unicodes=$unicodes, path=$path, endPointIndices=$endPointIndices, instructionLength=$instructionLength, points=$points, refs=$refs, isComposite=$isComposite, unitsPerEm=$unitsPerEm, width=$width, height=$height, rightSideBearing=$rightSideBearing)"
    }


}