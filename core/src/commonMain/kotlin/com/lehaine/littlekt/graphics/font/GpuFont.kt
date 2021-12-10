package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.isFuzzyEqual
import kotlin.math.max
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 12/9/2021
 */
class GpuFont(val font: TtfFont) {

    private val compiler = GlyphCompiler()

    fun glyph(char: Char) {
        val glyph = font.glyphs[char.code] ?: error("Glyph for $char doesn't exist!")
        val curves = compiler.compile(glyph)
    }

    companion object {
        private const val GRID_MAX_SIZE = 20
        private const val GRID_ATLAS_SIZE = 256 // fits exactly 1024 8x8 grids
        private const val BEZIER_ATLAS_SIZE = 256 // fits about 700-1k glyphs, depending on their curves
        private const val ATLAS_CHANNELS = 4 // Must be 4 (RGBA)
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

    fun compile(glyph: Glyph) {
        // Tolerance for error when approximating cubic beziers with quadratics.
        // Too low and many quadratics are generated (slow), too high and not
        // enough are generated (looks bad). 5% works pretty well.
        val c2qResolution = max((((glyph.width + glyph.height) / 2) * 0.05f).toInt(), 1)
        println(c2qResolution)
        val beziers = decompose(glyph, c2qResolution)

        // TODO calculate if glyph orientation is clockwise or counter clockwise. If, CCW then we need to flip the beziers
        return beziers
    }

    private fun decompose(glyph: Glyph, c2qResolution: Int) {
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
    }
}




