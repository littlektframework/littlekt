package com.littlekt.graphics.g2d.shape

import com.littlekt.graphics.Color
import com.littlekt.math.isFuzzyEqual
import kotlin.math.sign
import kotlin.math.sqrt

internal class LineDrawer(batchManager: BatchManager) : Drawer(batchManager) {

    fun line(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        snap: Boolean,
        c1: Color = batchManager.color,
        c2: Color = batchManager.color,
    ) {
        pushLine(x1, y1, x2, y2, thickness, snap, c1, c2)
    }

    @Suppress("NAME_SHADOWING")
    fun pushLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        snap: Boolean,
        c1: Color = batchManager.color,
        c2: Color = batchManager.color,
    ) {
        batchManager.ensureSpaceForQuad()

        val dx = x2 - x1
        val dy = y2 - y1

        val offset = batchManager.offset
        val pixelSize = batchManager.pixelSize
        val halfPixelSize = batchManager.halfPixelSize

        val x1 = if (snap) snapPixel(x1, pixelSize, halfPixelSize) - sign(dx) * offset else x1
        val y1 = if (snap) snapPixel(y1, pixelSize, halfPixelSize) - sign(dy) * offset else y1
        val x2 = if (snap) snapPixel(x2, pixelSize, halfPixelSize) - sign(dx) * offset else x2
        val y2 = if (snap) snapPixel(y2, pixelSize, halfPixelSize) - sign(dy) * offset else y2

        val px: Float
        val py: Float

        if (x1.isFuzzyEqual(x2, 0.001f)) {
            px = thickness * 0.5f
            py = px
        } else if (y1.isFuzzyEqual(y2, 0.001f)) {
            py = thickness * 0.5f
            px = py
        } else {
            val scale = 1f / sqrt(dx * dx + dy * dy) * (thickness * 0.5f)

            px = dy * scale
            py = dx * scale
        }

        x1(x1 + px)
        y1(y1 - py)
        x2(x1 - px)
        y2(y1 + py)
        x3(x2 - px)
        y3(y2 + py)
        x4(x2 + px)
        y4(y2 - py)

        color1(c1)
        color2(c1)
        color3(c2)
        color4(c2)

        batchManager.pushQuad()
        if (!batchManager.cachingDraws) {
            batchManager.pushToBatch()
        }
    }
}
