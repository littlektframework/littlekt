package com.lehaine.littlekt.graphics.g2d.font.internal

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
internal data class GpuGlyph(
    val width: Int,
    val height: Int,
    val offsetX: Int,
    val offsetY: Int,
    val bezierAtlasPosX: Int,
    val bezierAtlasPosY: Int,
    val atlasIdx: Int,
    val advanceWidth: Int
)