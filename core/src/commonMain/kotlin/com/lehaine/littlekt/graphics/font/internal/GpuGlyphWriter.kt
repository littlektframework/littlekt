package com.lehaine.littlekt.graphics.font.internal

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.graphics.font.Bezier

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
internal object GpuGlyphWriter {

    fun writeGlyphToBuffer(
        buffer: ByteBuffer,
        curves: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridX: Short,
        gridY: Short,
        gridWidth: Short,
        gridHeight: Short
    ) {
        buffer.putUShort(gridX).putUShort(gridY).putUShort(gridWidth).putUShort(gridHeight)
        curves.forEach {
            writeBezierToBuffer(buffer, it, glyphWidth, glyphHeight)
        }
    }

    /**
     * A [Bezier] is written as 6 16-bit integers (12 bytes). Increments buffer by the number of bytes written (always 12).
     * Coords are scaled from [0, glyphSize] to [o, UShort.MAX_VALUE]
     */
    private fun writeBezierToBuffer(buffer: ByteBuffer, bezier: Bezier, glyphWidth: Int, glyphHeight: Int) {
        buffer.apply {
            putUShort((bezier.p0.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUShort((bezier.p0.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
            putUShort((bezier.control.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUShort((bezier.control.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
            putUShort((bezier.p1.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUShort((bezier.p1.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
        }
    }
}