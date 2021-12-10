package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.file.FileHandler
import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.createMixedBuffer
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.isFuzzyEqual
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.time.measureTimedValue

/**
 * @author Colton Daily
 * @date 12/9/2021
 */
class GpuFont(val font: TtfFont, val fileHandler: FileHandler) {

    private val compiler = GlyphCompiler()
    private val atlases = mutableListOf<AtlasGroup>()

    fun glyph(char: Char) {
        // TODO check if it is already in atlas before compiling

        val glyph = font.glyphs[char.code] ?: error("Glyph for $char doesn't exist!")
        val curves =
            measureTimedValue { compiler.compile(glyph) }.also { println("Took ${it.duration} to compile $char glyph.") }.value
        var atlas = getOpenAtlasGroup()

        // Although the data is represented as a 32bit texture, it's actually
        // two 16bit ints per pixel, each with an x and y coordinate for
        // the bezier. Every six 16bit ints (3 pixels) is a full bezier
        // plus two pixels for grid position information
        val bezierPixelLength = 2 + curves.size * 3

        val tooManyCurves = bezierPixelLength > BEZIER_ATLAS_SIZE * BEZIER_ATLAS_SIZE

        if (curves.isEmpty() || tooManyCurves) {
            if (tooManyCurves) {
                logger.warn { "Glyph '$char' has too many curves!" }
            }
            // TODO do what then if its empty or too many curves?
        }

        if (atlas.glyphDataBufOffset + bezierPixelLength > BEZIER_ATLAS_SIZE * BEZIER_ATLAS_SIZE) {
            atlas.full = true
            atlas.uploaded = true
            atlas = getOpenAtlasGroup()
        }

        if (atlas.x + GRID_MAX_SIZE > GRID_ATLAS_SIZE) {
            atlas.y += GRID_MAX_SIZE
            atlas.x = 0
            if (atlas.y >= GRID_ATLAS_SIZE) {
                atlas.full = true
                atlas.uploaded = false
                atlas = getOpenAtlasGroup()
            }
        }

        val buffer = createMixedBuffer(atlas.glyphDataBuf.size + (atlas.glyphDataBufOffset * ATLAS_CHANNELS))
        writeGlyphToBuffer(
            buffer, curves, glyph.width, glyph.height, atlas.x.toShort(), atlas.y.toShort(),
            GRID_MAX_SIZE.toShort(), GRID_MAX_SIZE.toShort()
        )

        writeBMP("bezierAtlas.bmp", BEZIER_ATLAS_SIZE, BEZIER_ATLAS_SIZE, 4, buffer)
    }

    private fun writeBMP(name: String, width: Int, height: Int, channels: Int, buffer: MixedBuffer) {
        val bmpBuffer = createMixedBuffer(60 + buffer.capacity)
        bmpBuffer.run {
            putInt8('B'.code.toByte())
            putInt8('M'.code.toByte())
            putUint32(54 + width * height * channels) // size
            putUint16(0) // res1
            putUint16(0) // res2
            putUint32(54) // offset
            putUint32(40) // biSize
            putUint32(width)
            putUint32(height)
            putUint16(1.toShort()) // planes
            putUint16((8 * channels).toShort()) // bitCount
            putUint32(0) // compression
            putUint32(width * height * channels) // image size bytes
            putUint32(0) // x pixels per meter
            putUint32(0) // y pixels per meter
            putUint32(0) // clr used
            putUint32(0) //clr important
            putInt8(buffer.toArray(), 0, buffer.capacity)
        }
        fileHandler.store("bezierAtlas.bmp", bmpBuffer.toArray())
    }

    private fun writeGlyphToBuffer(
        buffer: MixedBuffer,
        curves: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridX: Short,
        gridY: Short,
        gridWidth: Short,
        gridHeight: Short
    ) {
        buffer.putInt16(gridX).putInt16(gridY).putInt16(gridWidth).putInt16(gridHeight)
        curves.forEach {
            writeBezierToBuffer(buffer, it, glyphWidth, glyphHeight)
        }
    }

    private fun writeBezierToBuffer(buffer: MixedBuffer, bezier: Bezier, glyphWidth: Int, glyphHeight: Int) {
        buffer.apply {
            putFloat32(bezier.p0.x * (Short.MAX_VALUE * 2 + 1) / glyphWidth)
            putFloat32(bezier.p0.y * (Short.MAX_VALUE * 2 + 1) / glyphHeight)
            putFloat32(bezier.control.x * (Short.MAX_VALUE * 2 + 1) / glyphWidth)
            putFloat32(bezier.control.y * (Short.MAX_VALUE * 2 + 1) / glyphHeight)
            putFloat32(bezier.p1.x * (Short.MAX_VALUE * 2 + 1) / glyphWidth)
            putFloat32(bezier.p1.y * (Short.MAX_VALUE * 2 + 1) / glyphHeight)
        }
    }

    private fun getOpenAtlasGroup(): AtlasGroup {
        if (atlases.isEmpty() || atlases.last().full) {
            val atlas = AtlasGroup().apply {
                glyphDataBuf = ByteArray(BEZIER_ATLAS_SIZE * BEZIER_ATLAS_SIZE * ATLAS_CHANNELS)
                gridAtlas = ByteArray(GRID_ATLAS_SIZE * GRID_ATLAS_SIZE * ATLAS_CHANNELS)
                uploaded = true
            }
            atlases += atlas
        }
        return atlases.last()
    }

    companion object {
        private const val GRID_MAX_SIZE = 20
        private const val GRID_ATLAS_SIZE = 256 // fits exactly 1024 8x8 grids
        private const val BEZIER_ATLAS_SIZE = 256 // fits about 700-1k glyphs, depending on their curves
        private const val ATLAS_CHANNELS = 4 // Must be 4 (RGBA)

        private val logger = Logger<GpuFont>()
    }
}

private class AtlasGroup {
    var x = 0
    var y = 0
    var full = false
    var uploaded = false
    var gridAtlas = ByteArray(0)
    var glyphDataBuf = ByteArray(0)
    var glyphDataBufOffset = 0

    override fun toString(): String {
        return "AtlasGroup(x=$x, y=$y, full=$full, uploaded=$uploaded, gridAtlas=$gridAtlas, glyphDataBuf=$glyphDataBuf, glyphDataBufOffset=$glyphDataBufOffset)"
    }
}

private class Bezier {
    val p0 = MutableVec2f()
    val p1 = MutableVec2f()
    val control = MutableVec2f()

    /**
     * Taking a quadratic bezier curve and a horizontal line y=Y, finds the x
     * values of intersection of the line and the curve. Returns 0, 1, or 2,
     * depending on how many intersections were found, and outX is filled with
     * that many x values of intersection.
     *
     * Quadratic bezier curves are represented by the function
     * F(t) = (1-t)^2*A + 2*t*(1-t)*B + t^2*C
     * where F is a vector function, A and C are the endpoint vectors, C is
     * the control point vector, and 0 <= t <= 1.
     * Solving the bezier function for t gives:
     * t = (A - B [+-] sqrt(y*a + B^2 - A*C))/a , where  a = A - 2B + C.
     * http://www.wolframalpha.com/input/?i=y+%3D+(1-t)%5E2a+%2B+2t(1-t)*b+%2B+t%5E2*c+solve+for+t
     */
    fun intersectHorizontal(y: Float, outX: FloatArray): Int {
        val A = p0
        val B = control
        val C = p1
        var i = 0

        //Parts of the bezier function solved for t
        val a = A.y - 2 * B.y + C.y
        if (isFuzzyEqual(a, 0f)) {
            val t = (2 * B.y - C.y - y) / (2 * (B.y - C.y))
            if (t in 0.0..1.0) {
                outX[i++] = ((1 - t) * (1 - t) * A.x + 2 * t * (1 - t) * B.x + t * t * C.x)
                return i
            }
        }
        val sqrtTerm = sqrt(y * a + B.y * B.y - A.y * C.y)
        var t = (A.y - B.y + sqrtTerm) / a
        if (t in 0.0..1.0) {
            outX[i++] = ((1 - t) * (1 - t) * A.x + 2 * t * (1 - t) * B.x + t * t * C.x)
        }
        t = (A.y - B.y - sqrtTerm) / a
        if (t in 0.0..1.0) {
            outX[i++] = ((1 - t) * (1 - t) * A.x + 2 * t * (1 - t) * B.x + t * t * C.x)
        }
        return i
    }

    /**
     * Same as [intersectHorizontal], except finds the y values of an intersection
     * with the vertical line x=X.
     */
    fun intersectVertical(x: Float, outY: FloatArray): Int {
        val inverse = Bezier().apply {
            this@Bezier.p0.set(p0.y, p0.x)
            this@Bezier.p1.set(p1.y, p1.x)
            this@Bezier.control.set(control.y, control.x)
        }
        return inverse.intersectHorizontal(x, outY)
    }
}

private class GlyphCompiler {

    fun compile(glyph: Glyph): List<Bezier> {
        // Tolerance for error when approximating cubic beziers with quadratics.
        // Too low and many quadratics are generated (slow), too high and not
        // enough are generated (looks bad). 5% works pretty well.
        val c2qResolution = max((((glyph.width + glyph.height) / 2) * 0.05f).toInt(), 1)
        println(c2qResolution)
        val beziers = decompose(glyph, c2qResolution)

        if (glyph.xMin != 0 || glyph.yMin != 0) {
            translateBeziers(beziers, glyph.xMin, glyph.yMin)
        }

        // TODO calculate if glyph orientation is clockwise or counter clockwise. If, CCW then we need to flip the beziers
        val counterClockwise = false //glyph.orientation == FILL_LEFT
        if (counterClockwise) {
            flipBeziers(beziers)
        }
        return beziers
    }

    private fun flipBeziers(beziers: ArrayList<Bezier>) {
        beziers.forEach { bezier ->
            bezier.p0.x = bezier.p1.x.also { bezier.p1.x = bezier.p0.x }
            bezier.p0.y = bezier.p1.y.also { bezier.p1.y = bezier.p0.y }
        }
    }

    private fun decompose(glyph: Glyph, c2qResolution: Int): ArrayList<Bezier> {
        if (glyph.path.isEmpty() || glyph.numberOfContours <= 0) {
            return ArrayList()
        }
        val curves = ArrayList<Bezier>(glyph.numberOfContours)
        val quadBeziers = Array(24) { QuadraticBezier(0f, 0f, 0f, 0f, 0f, 0f) }

        var startX = 0f
        var startY = 0f
        var prevX = 0f
        var prevY = 0f
        glyph.path.commands.forEach { cmd ->
            when (cmd.type) {
                GlyphPath.CommandType.MOVE_TO -> {
                    startX = cmd.x
                    startY = cmd.y
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.LINE_TO -> {
                    curves += Bezier().apply {
                        p0.set(prevX, prevY)
                        control.set(cmd.x, cmd.y)
                        p1.set(cmd.x, cmd.y)
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.CURVE_TO -> {
                    val cubicBezier = CubicBezier(prevX, prevY, cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y)

                    val totalBeziers = cubicBezier.convertToQuadBezier(c2qResolution, quadBeziers)
                    for (i in 0 until totalBeziers) {
                        val quadBezier = quadBeziers[i]
                        curves += Bezier().apply {
                            p0.set(quadBezier.p1x, quadBezier.p1y)
                            control.set(quadBezier.c1x, quadBezier.c1y)
                            p1.set(quadBezier.p2x, quadBezier.p2y)
                        }
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.QUADRATIC_CURVE_TO -> {
                    curves += Bezier().apply {
                        p0.set(prevX, prevY)
                        control.set(cmd.x1, cmd.y1)
                        p1.set(cmd.x, cmd.y)
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.CLOSE -> {
                    prevX = startX
                    prevY = startY
                }
            }
        }
        return curves
    }


    private fun translateBeziers(beziers: ArrayList<Bezier>, xMin: Int, yMin: Int) {
        beziers.forEach {
            it.p0.x -= xMin
            it.p0.y -= yMin
            it.p1.x -= xMin
            it.p1.y -= yMin
            it.control.x -= xMin
            it.control.y -= yMin

        }
    }
}




