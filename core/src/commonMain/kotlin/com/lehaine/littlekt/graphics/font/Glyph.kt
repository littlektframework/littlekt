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
    val points: List<Point>,
    val refs: List<GlyphReference>,
    val isComposite: Boolean,
    val unitsPerEm:Int
) {
    override fun toString(): String {
        return "Glyph(name=$name, index=$index, xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax, advanceWidth=$advanceWidth, leftSideBearing=$leftSideBearing, numberOfContours=$numberOfContours, endPointIndices=$endPointIndices, instructionLength=$instructionLength, instructions=$instructions,\npoints=[\n${
            points.joinToString(
                separator = "\n"
            )
        }\n], refs=$refs, isComposite=$isComposite, unicode=$unicode, unicodes=$unicodes)"
    }
}