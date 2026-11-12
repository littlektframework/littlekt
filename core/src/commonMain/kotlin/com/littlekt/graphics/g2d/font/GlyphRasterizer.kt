package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.g2d.font.GlyphPath.CommandType.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Rasterizes glyph outlines into anti-aliased coverage. Each pixel gets the exact area of it that
 * is covered by the outline, using the nonzero fill rule. The buffers are reused between calls, so
 * keep one rasterizer around instead of creating one per glyph.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class GlyphRasterizer {
    private var accumulation = FloatArray(0)

    /** The width of the last rasterized coverage. */
    var width: Int = 0
        private set

    /** The height of the last rasterized coverage. */
    var height: Int = 0
        private set

    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    /**
     * Rasterizes [path] into a [width] by [height] area. A path point `(x, y)` lands on pixel
     * `(x * scale + offsetX, offsetY - y * scale)`, so paths are y-up and the coverage is y-down
     * from the top row. Read the result with [coverage].
     */
    fun rasterize(
        path: GlyphPath,
        width: Int,
        height: Int,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ) {
        this.width = width
        this.height = height
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        val size = width * height + 4
        if (accumulation.size < size) accumulation = FloatArray(size)
        accumulation.fill(0f, 0, size)

        val commands = path.commands
        for (i in commands.indices) {
            val cmd = commands[i]
            when (cmd.type) {
                MOVE_TO -> {
                    closeContour()
                    startX = px(cmd.x)
                    startY = py(cmd.y)
                    lastX = startX
                    lastY = startY
                }
                LINE_TO -> lineTo(px(cmd.x), py(cmd.y))
                QUADRATIC_CURVE_TO -> quadTo(px(cmd.x1), py(cmd.y1), px(cmd.x), py(cmd.y))
                CURVE_TO ->
                    cubicTo(px(cmd.x1), py(cmd.y1), px(cmd.x2), py(cmd.y2), px(cmd.x), py(cmd.y))
                CLOSE -> closeContour()
            }
        }
        closeContour()
    }

    /**
     * Writes the last rasterized coverage, from `0` to `1`, as bytes into [out].
     *
     * @param out the destination with room for [width] times [height] bytes from [offset]
     * @param stride the number of bytes between rows in [out]
     * @param gamma values above `1` make thin text heavier
     */
    fun coverage(out: ByteArray, offset: Int = 0, stride: Int = width, gamma: Float = 1f) {
        var acc = 0f
        val invGamma = 1f / gamma
        for (y in 0 until height) {
            val row = y * width
            val dst = offset + y * stride
            for (x in 0 until width) {
                acc += accumulation[row + x]
                var value = min(abs(acc), 1f)
                if (invGamma != 1f && value > 0f) value = value.pow(invGamma)
                out[dst + x] = (value * 255f + 0.5f).toInt().toByte()
            }
        }
    }

    /** @return the last rasterized coverage at pixel ([x], [y]), from `0` to `1`. */
    fun coverageAt(x: Int, y: Int): Float {
        var acc = 0f
        val end = y * width + x
        for (i in 0..end) acc += accumulation[i]
        return min(abs(acc), 1f)
    }

    private fun px(x: Float) = x * scale + offsetX

    private fun py(y: Float) = offsetY - y * scale

    private fun closeContour() {
        if (lastX != startX || lastY != startY) lineTo(startX, startY)
    }

    private fun lineTo(x: Float, y: Float) {
        drawLine(lastX, lastY, x, y)
        lastX = x
        lastY = y
    }

    private fun quadTo(cx: Float, cy: Float, x: Float, y: Float) {
        val x0 = lastX
        val y0 = lastY
        val devX = x0 - 2f * cx + x
        val devY = y0 - 2f * cy + y
        val devSq = devX * devX + devY * devY
        if (devSq < 0.333f) {
            lineTo(x, y)
            return
        }
        val steps = 1 + floor(sqrt(sqrt(TOLERANCE * devSq))).toInt()
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val mt = 1f - t
            lineTo(
                mt * mt * x0 + 2f * mt * t * cx + t * t * x,
                mt * mt * y0 + 2f * mt * t * cy + t * t * y,
            )
        }
    }

    private fun cubicTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        val x0 = lastX
        val y0 = lastY
        val dev1X = x0 - 2f * c1x + c2x
        val dev1Y = y0 - 2f * c1y + c2y
        val dev2X = c1x - 2f * c2x + x
        val dev2Y = c1y - 2f * c2y + y
        val devSq = max(dev1X * dev1X + dev1Y * dev1Y, dev2X * dev2X + dev2Y * dev2Y)
        if (devSq < 0.333f) {
            lineTo(x, y)
            return
        }
        val steps = 1 + floor(sqrt(sqrt(TOLERANCE * devSq * 2.25f))).toInt()
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val mt = 1f - t
            val a = mt * mt * mt
            val b = 3f * mt * mt * t
            val c = 3f * mt * t * t
            val d = t * t * t
            lineTo(a * x0 + b * c1x + c * c2x + d * x, a * y0 + b * c1y + c * c2y + d * y)
        }
    }

    private fun drawLine(ax: Float, ay: Float, bx: Float, by: Float) {
        if (ay == by) return
        val dir: Float
        val x0p: Float
        val y0p: Float
        val x1p: Float
        val y1p: Float
        if (ay < by) {
            dir = 1f
            x0p = ax
            y0p = ay
            x1p = bx
            y1p = by
        } else {
            dir = -1f
            x0p = bx
            y0p = by
            x1p = ax
            y1p = ay
        }
        val dxdy = (x1p - x0p) / (y1p - y0p)
        var x = x0p
        if (y0p < 0f) x -= y0p * dxdy
        val yStart = max(0, y0p.toInt())
        val yEnd = min(height, ceil(y1p).toInt())
        val acc = accumulation
        for (y in yStart until yEnd) {
            val lineStart = y * width
            val dy = min(y + 1f, y1p) - max(y.toFloat(), y0p)
            val xNext = x + dxdy * dy
            val d = dy * dir
            val xa = min(x, xNext)
            val xb = max(x, xNext)
            val xaFloor = floor(xa)
            val xai = xaFloor.toInt()
            val xbCeil = ceil(xb)
            val xbi = xbCeil.toInt()
            val start = lineStart + xai
            if (start < 0) {
                x = xNext
                continue
            }
            if (xbi <= xai + 1) {
                val xmf = 0.5f * (x + xNext) - xaFloor
                acc[start] += d - d * xmf
                acc[start + 1] += d * xmf
            } else {
                val s = 1f / (xb - xa)
                val xaf = xa - xaFloor
                val a0 = 0.5f * s * (1f - xaf) * (1f - xaf)
                val xbf = xb - xbCeil + 1f
                val am = 0.5f * s * xbf * xbf
                if (xbi == xai + 2) {
                    acc[start] += d * a0
                    acc[start + 1] += d * (1f - a0 - am)
                } else {
                    val a1 = s * (1.5f - xaf)
                    acc[start] += d * a0
                    acc[start + 1] += d * (a1 - a0)
                    for (xi in xai + 2 until xbi - 1) acc[lineStart + xi] += d * s
                    val a2 = a1 + (xbi - xai - 3) * s
                    acc[lineStart + xbi - 1] += d * (1f - a2 - am)
                }
                acc[lineStart + xbi] += d * am
            }
            x = xNext
        }
    }

    private fun Float.pow(exponent: Float): Float = kotlin.math.exp(exponent * kotlin.math.ln(this))

    private companion object {
        const val TOLERANCE = 3f
    }
}
