package com.lehaine.littlekt.graphics.font.internal

import com.lehaine.littlekt.graphics.font.*
import kotlin.math.max

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
internal class GpuGlyphCompiler {

    fun compile(glyph: TtfGlyph): List<Bezier> {
        // Tolerance for error when approximating cubic beziers with quadratics.
        // Too low and many quadratics are generated (slow), too high and not
        // enough are generated (looks bad). 5% works pretty well.
        val c2qResolution = max((((glyph.width + glyph.height) / 2) * 0.05f).toInt(), 1)
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

    private fun decompose(glyph: TtfGlyph, c2qResolution: Int): ArrayList<Bezier> {
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
                        control.set(prevX, prevY)
                        p1.set(cmd.x, cmd.y)
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.CURVE_TO -> {
                    val cubicBezier = CubicBezier(prevX, prevY, cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y)

                    val totalBeziers = 6 * cubicBezier.convertToQuadBezier(c2qResolution, quadBeziers)
                    for (i in 0 until totalBeziers step 6) {
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
