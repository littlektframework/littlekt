package com.lehaine.littlekt.file.font.ttf.internal

import com.lehaine.littlekt.file.createFloat32Buffer
import com.lehaine.littlekt.graphics.font.TriangleType
import com.lehaine.littlekt.math.RectBuilder

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class GlyphCompiler {
    private val vertices = mutableListOf<Float>()
    private var firstX = 0f
    private var firstY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var contourCount = 0
    private var glyph: Glyph? = null
    private var rectBuilder = RectBuilder()

    fun begin(glyph: Glyph) {
        this.glyph = glyph
        rectBuilder.reset()
        vertices.clear()
    }

    fun moveTo(x: Float, y: Float) {
        firstX = x
        currentX = x
        firstY = y
        currentY = y
        contourCount = 0
    }

    fun lineTo(x: Float, y: Float) {
        if (++contourCount >= 2) {
            appendTriangle(firstX, firstY, currentX, currentY, x, y, TriangleType.SOLID)
        }
        currentX = x
        currentY = y
    }

    fun curveTo(cx: Float, cy: Float, x: Float, y: Float) {
        if (++contourCount >= 2) {
            appendTriangle(firstX, firstY, currentX, currentX, x, y, TriangleType.SOLID)
        }
        appendTriangle(currentX, currentX, cx, cy, x, y, TriangleType.QUADRATIC_CURVE)
        currentX = x
        currentY = y
    }

    fun close() {
        currentX = firstX
        currentY = firstY
        contourCount = 0
    }

    fun end() {
        glyph?.vertices = createFloat32Buffer(vertices.toFloatArray())
        glyph?.bounds = rectBuilder.build()
    }

    fun appendTriangle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, triangleType: TriangleType) {
        when (triangleType) {
            TriangleType.SOLID -> {
                appendVertex(ax, ay, 0f, 1f)
                appendVertex(bx, by, 0f, 1f)
                appendVertex(cx, cy, 0f, 1f)
            }
            TriangleType.QUADRATIC_CURVE -> {
                appendVertex(ax, ay, 0f, 0f)
                appendVertex(bx, by, 0.5f, 0f)
                appendVertex(cx, cy, 1f, 1f)
            }
        }
    }

    fun appendVertex(x: Float, y: Float, s: Float, t: Float) {
        rectBuilder.include(x, y)
        vertices.run {
            add(x)
            add(y)
            add(s)
            add(t)
        }
    }
}