package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.file.Float32Buffer
import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.createFloat32Buffer
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.RectBuilder

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
class GPUFont(private val data: MixedBuffer) {
    var ascender = 0f
    var descender = 0f

    private var scale = 0f
    private val glyphs = mutableMapOf<Int, Glyph>()
    private val glyphCompiler = GlyphCompiler()

    init {
        scale = 1f / data.readInt16
        ascender = data.readInt16 * scale
        descender = data.readInt16 * scale

        for (i in 0..data.readInt16) {
            val glyph = Glyph()
            val codePoint = data.readInt16.toInt()
            glyph.apply {
                this.codePoint = codePoint and 0x7FFF
                advanceWidth = data.readInt16 * scale
                byteOffset = data.readInt32
                byteLength = data.readInt16.toInt()
            }
            glyphs[glyph.codePoint] = glyph
        }
    }

    fun glyph(codePoint: Int) {
        val glyph = glyphs[codePoint] ?: throw RuntimeException("Glyph does not exist for code point: $codePoint")

        if (glyph.codePoint.toChar() != ' ') {
            val end = glyph.byteOffset + glyph.byteLength
            data.position = glyph.byteOffset
            glyphCompiler.begin(glyph)

            while (data.limit < end) {
                when (data.readUint8) {
                    0.toByte() -> { // MOVE_TO
                        val x = data.readInt16 * scale
                        val y = data.readInt16 * scale + ascender
                        glyphCompiler.moveTo(x, y)
                    }
                    1.toByte() -> { // LINE_TO
                        val x = data.readInt16 * scale
                        val y = data.readInt16 * scale + ascender
                        glyphCompiler.lineTo(x, y)
                    }
                    2.toByte() -> { // CURVE_TO
                        val cx = data.readInt16 * scale
                        val cy = data.readInt16 * scale + ascender
                        val x = data.readInt16 * ascender
                        val y = data.readInt16 * scale + ascender
                        glyphCompiler.curveTo(cx, cy, x, y)
                    }
                    3.toByte() -> { // CLOSE
                        glyphCompiler.close()
                    }
                }
            }
        }

    }
}

data class Glyph(
    var codePoint: Int = -1,
    var advanceWidth: Float = 0f,
    var byteOffset: Int = 0,
    var byteLength: Int = 0,
    var vertices: Float32Buffer? = null,
    var bounds: Rect? = null
)

enum class TriangleType {
    SOLID,
    QUADRATIC_CURVE
}

class GlyphCompiler {
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