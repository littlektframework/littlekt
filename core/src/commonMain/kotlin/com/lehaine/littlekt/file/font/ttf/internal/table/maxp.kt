package com.lehaine.littlekt.file.font.ttf.internal.table

import com.lehaine.littlekt.file.MixedBuffer

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class MaxpParser(val buffer: MixedBuffer, val start: Int) {

    fun parse(): Maxp {
        TODO()
    }
}

internal data class Maxp(
    val version: Int,
    val numGlyphs: Int,
    val maxPoints: Int,
    val maxContours: Int,
    val maxCompositePoints: Int,
    val maxZones: Int,
    val maxTwilightPoints: Int,
    val maxStorage: Int,
    val maxFunctionsDefs: Int,
    val maxInstructionsDefs: Int,
    val maxStackElements: Int,
    val maxSizeOfInstructions: Int,
    val maxComponentElements: Int,
    val maxComponentDepth: Int
)