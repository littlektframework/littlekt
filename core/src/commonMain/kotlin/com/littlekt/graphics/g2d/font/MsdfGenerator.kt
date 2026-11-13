package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.g2d.font.GlyphPath.CommandType.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates multi-channel signed distance fields (MTSDF) from glyph outlines. The red, green & blue
 * channels hold a multi-channel distance that keeps corners sharp at any scale, and alpha holds
 * the true distance for soft effects such as glow. A value of `0.5` is the outline, higher is
 * inside.
 *
 * Based on the approach of msdfgen by Viktor Chlumský.
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
class MsdfGenerator {
    private val rasterizer = GlyphRasterizer()
    private var coverage = ByteArray(0)
    private var field = FloatArray(0)

    /** The width of the last generated field. */
    var width: Int = 0
        private set

    /** The height of the last generated field. */
    var height: Int = 0
        private set

    /**
     * Generates the field of [path] into a [width] by [height] area. A path point `(x, y)` lands on
     * pixel `(x * scale + offsetX, offsetY - y * scale)`. Read the result with [write] or [valueAt].
     *
     * @param range the distance, in pixels, from the outline where the field reaches `0` or `1`
     */
    fun generate(
        path: GlyphPath,
        width: Int,
        height: Int,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        range: Float,
    ) {
        this.width = width
        this.height = height
        val size = width * height * 4
        if (field.size < size) field = FloatArray(size)

        val contours = buildContours(path, scale, offsetX, offsetY)
        contours.forEach { colorEdges(it, ANGLE_THRESHOLD) }
        val edges = contours.flatMap { it }

        rasterizer.rasterize(path, width, height, scale, offsetX, offsetY)
        if (coverage.size < width * height) coverage = ByteArray(width * height)
        rasterizer.coverage(coverage)

        val point = Vec()
        val r = Closest()
        val g = Closest()
        val b = Closest()
        val all = Closest()
        val distance = SignedDistance()
        for (y in 0 until height) {
            for (x in 0 until width) {
                point.set(x + 0.5f, y + 0.5f)
                r.reset()
                g.reset()
                b.reset()
                all.reset()
                for (i in edges.indices) {
                    val edge = edges[i]
                    val param = edge.signedDistance(point, distance)
                    if (edge.color and RED != 0) r.offer(edge, distance, param)
                    if (edge.color and GREEN != 0) g.offer(edge, distance, param)
                    if (edge.color and BLUE != 0) b.offer(edge, distance, param)
                    all.offer(edge, distance, param)
                }
                var rv = r.pseudoDistance(point) / range + 0.5f
                var gv = g.pseudoDistance(point) / range + 0.5f
                var bv = b.pseudoDistance(point) / range + 0.5f
                var av = all.trueDistance() / range + 0.5f
                val inside = (coverage[y * width + x].toInt() and 0xFF) >= 128
                if ((median(rv, gv, bv) > 0.5f) != inside) {
                    rv = 1f - rv
                    gv = 1f - gv
                    bv = 1f - bv
                }
                if ((av > 0.5f) != inside) av = 1f - av
                val i = (y * width + x) * 4
                field[i] = rv
                field[i + 1] = gv
                field[i + 2] = bv
                field[i + 3] = av
            }
        }
        if (edges.isEmpty()) return
        correctClashes(CLASH_THRESHOLD / range)
    }

    /** @return channel [channel] (`0` red to `3` alpha) of the last field at pixel ([x], [y]). */
    fun valueAt(x: Int, y: Int, channel: Int): Float = field[(y * width + x) * 4 + channel]

    /** @return the median of the red, green & blue channels at pixel ([x], [y]). */
    fun medianAt(x: Int, y: Int): Float {
        val i = (y * width + x) * 4
        return median(field[i], field[i + 1], field[i + 2])
    }

    /**
     * Writes the last field as RGBA bytes into [out].
     *
     * @param stride the number of bytes between rows in [out]
     */
    fun write(out: ByteArray, offset: Int = 0, stride: Int = width * 4) {
        for (y in 0 until height) {
            val row = offset + y * stride
            for (x in 0 until width * 4) {
                val v = field[y * width * 4 + x].coerceIn(0f, 1f)
                out[row + x] = (v * 255f + 0.5f).toInt().toByte()
            }
        }
    }

    private fun correctClashes(threshold: Float) {
        val clashes = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (
                    (x > 0 && clash(x, y, x - 1, y, threshold)) ||
                        (x < width - 1 && clash(x, y, x + 1, y, threshold)) ||
                        (y > 0 && clash(x, y, x, y - 1, threshold)) ||
                        (y < height - 1 && clash(x, y, x, y + 1, threshold))
                ) {
                    clashes[y * width + x] = true
                }
            }
        }
        for (i in clashes.indices) {
            if (!clashes[i]) continue
            val p = i * 4
            val m = median(field[p], field[p + 1], field[p + 2])
            field[p] = m
            field[p + 1] = m
            field[p + 2] = m
        }
    }

    private fun clash(ax: Int, ay: Int, bx: Int, by: Int, threshold: Float): Boolean {
        val ai = (ay * width + ax) * 4
        val bi = (by * width + bx) * 4
        var a0 = field[ai]
        var a1 = field[ai + 1]
        var a2 = field[ai + 2]
        var b0 = field[bi]
        var b1 = field[bi + 1]
        var b2 = field[bi + 2]
        var t: Float
        if (abs(b0 - a0) < abs(b1 - a1)) {
            t = a0; a0 = a1; a1 = t
            t = b0; b0 = b1; b1 = t
        }
        if (abs(b1 - a1) < abs(b2 - a2)) {
            t = a1; a1 = a2; a2 = t
            t = b1; b1 = b2; b2 = t
            if (abs(b0 - a0) < abs(b1 - a1)) {
                t = a0; a0 = a1; a1 = t
                t = b0; b0 = b1; b1 = t
            }
        }
        return abs(b1 - a1) >= threshold &&
            !(b0 == b1 && b0 == b2) &&
            abs(a2 - 0.5f) >= abs(b2 - 0.5f)
    }

    private fun buildContours(
        path: GlyphPath,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ): List<MutableList<Edge>> {
        val contours = mutableListOf<MutableList<Edge>>()
        var current: MutableList<Edge>? = null
        var startX = 0f
        var startY = 0f
        var lastX = 0f
        var lastY = 0f

        fun px(x: Float) = x * scale + offsetX
        fun py(y: Float) = offsetY - y * scale

        fun close() {
            val contour = current ?: return
            if (lastX != startX || lastY != startY) {
                contour += Edge.line(lastX, lastY, startX, startY)
            }
            contour.removeAll { it.isDegenerate() }
            if (contour.isNotEmpty()) contours += contour
            current = null
        }

        path.commands.forEach { cmd ->
            when (cmd.type) {
                MOVE_TO -> {
                    close()
                    current = mutableListOf()
                    startX = px(cmd.x)
                    startY = py(cmd.y)
                    lastX = startX
                    lastY = startY
                }
                LINE_TO -> {
                    val x = px(cmd.x)
                    val y = py(cmd.y)
                    current?.add(Edge.line(lastX, lastY, x, y))
                    lastX = x
                    lastY = y
                }
                QUADRATIC_CURVE_TO -> {
                    val x = px(cmd.x)
                    val y = py(cmd.y)
                    current?.add(Edge.quad(lastX, lastY, px(cmd.x1), py(cmd.y1), x, y))
                    lastX = x
                    lastY = y
                }
                CURVE_TO -> {
                    val x = px(cmd.x)
                    val y = py(cmd.y)
                    current?.addAll(
                        cubicToQuads(lastX, lastY, px(cmd.x1), py(cmd.y1), px(cmd.x2), py(cmd.y2), x, y)
                    )
                    lastX = x
                    lastY = y
                }
                CLOSE -> close()
            }
        }
        close()
        return contours
    }

    private fun cubicToQuads(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ): List<Edge> {
        val parts = CUBIC_SPLITS
        val result = ArrayList<Edge>(parts)
        var prevX = x0
        var prevY = y0
        for (i in 1..parts) {
            val t0 = (i - 1).toFloat() / parts
            val t1 = i.toFloat() / parts
            val ex = cubic(x0, x1, x2, x3, t1)
            val ey = cubic(y0, y1, y2, y3, t1)
            val tm = (t0 + t1) * 0.5f
            val mx = cubic(x0, x1, x2, x3, tm)
            val my = cubic(y0, y1, y2, y3, tm)
            val cx = 2f * mx - (prevX + ex) * 0.5f
            val cy = 2f * my - (prevY + ey) * 0.5f
            result += Edge.quad(prevX, prevY, cx, cy, ex, ey)
            prevX = ex
            prevY = ey
        }
        return result
    }

    private fun cubic(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val mt = 1f - t
        return mt * mt * mt * p0 + 3f * mt * mt * t * p1 + 3f * mt * t * t * p2 + t * t * t * p3
    }

    private class Vec(var x: Float = 0f, var y: Float = 0f) {
        fun set(x: Float, y: Float) = apply {
            this.x = x
            this.y = y
        }
    }

    private class SignedDistance(var distance: Float = -Float.MAX_VALUE, var dot: Float = 1f) {
        fun set(other: SignedDistance) {
            distance = other.distance
            dot = other.dot
        }

        fun lessThan(other: SignedDistance): Boolean =
            abs(distance) < abs(other.distance) ||
                (abs(distance) == abs(other.distance) && dot < other.dot)
    }

    private class Closest {
        val distance = SignedDistance()
        var edge: Edge? = null
        var param = 0f

        fun reset() {
            distance.distance = -Float.MAX_VALUE
            distance.dot = 1f
            edge = null
            param = 0f
        }

        fun offer(edge: Edge, candidate: SignedDistance, param: Float) {
            if (candidate.lessThan(distance)) {
                distance.set(candidate)
                this.edge = edge
                this.param = param
            }
        }

        fun pseudoDistance(point: Vec): Float {
            val edge = edge ?: return -Float.MAX_VALUE
            return edge.pseudoDistance(distance.distance, point, param)
        }

        fun trueDistance(): Float = if (edge == null) -Float.MAX_VALUE else distance.distance
    }

    private class Edge(
        val type: Int,
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
    ) {
        var color = WHITE

        fun isDegenerate(): Boolean =
            if (type == LINE) x0 == x1 && y0 == y1
            else x0 == x2 && y0 == y2 && x0 == x1 && y0 == y1

        fun endX() = if (type == LINE) x1 else x2

        fun endY() = if (type == LINE) y1 else y2

        fun directionX(t: Float): Float {
            if (type == LINE) return x1 - x0
            val dx = (x1 - x0) * (1f - t) + (x2 - x1) * t
            val dy = (y1 - y0) * (1f - t) + (y2 - y1) * t
            return if (dx == 0f && dy == 0f) x2 - x0 else dx
        }

        fun directionY(t: Float): Float {
            if (type == LINE) return y1 - y0
            val dx = (x1 - x0) * (1f - t) + (x2 - x1) * t
            val dy = (y1 - y0) * (1f - t) + (y2 - y1) * t
            return if (dx == 0f && dy == 0f) y2 - y0 else dy
        }

        /** Stores the signed distance from [p] in [out] and @return the closest param. */
        fun signedDistance(p: Vec, out: SignedDistance): Float =
            if (type == LINE) lineDistance(p, out) else quadDistance(p, out)

        private fun lineDistance(p: Vec, out: SignedDistance): Float {
            val aqx = p.x - x0
            val aqy = p.y - y0
            val abx = x1 - x0
            val aby = y1 - y0
            val param = (aqx * abx + aqy * aby) / (abx * abx + aby * aby)
            val eqx = (if (param > 0.5f) x1 else x0) - p.x
            val eqy = (if (param > 0.5f) y1 else y0) - p.y
            val endpointDistance = length(eqx, eqy)
            if (param > 0f && param < 1f) {
                val len = length(abx, aby)
                val ortho = (aby / len) * aqx + (-abx / len) * aqy
                if (abs(ortho) < endpointDistance) {
                    out.distance = ortho
                    out.dot = 0f
                    return param
                }
            }
            out.distance = nonZeroSign(cross(aqx, aqy, abx, aby)) * endpointDistance
            out.dot = abs(dotNormalized(abx, aby, eqx, eqy))
            return param
        }

        private fun quadDistance(p: Vec, out: SignedDistance): Float {
            val qax = x0 - p.x
            val qay = y0 - p.y
            val abx = x1 - x0
            val aby = y1 - y0
            val brx = x2 - x1 - abx
            val bry = y2 - y1 - aby
            val a = brx * brx + bry * bry
            val b = 3f * (abx * brx + aby * bry)
            val c = 2f * (abx * abx + aby * aby) + (qax * brx + qay * bry)
            val d = qax * abx + qay * aby

            var epDirX = directionX(0f)
            var epDirY = directionY(0f)
            var minDistance = nonZeroSign(cross(epDirX, epDirY, qax, qay)) * length(qax, qay)
            var param = -(qax * epDirX + qay * epDirY) / (epDirX * epDirX + epDirY * epDirY)
            run {
                epDirX = directionX(1f)
                epDirY = directionY(1f)
                val ex = x2 - p.x
                val ey = y2 - p.y
                val distance = length(ex, ey)
                if (distance < abs(minDistance)) {
                    minDistance = nonZeroSign(cross(epDirX, epDirY, ex, ey)) * distance
                    param =
                        ((p.x - x1) * epDirX + (p.y - y1) * epDirY) /
                            (epDirX * epDirX + epDirY * epDirY)
                }
            }
            val count = solveCubic(a, b, c, d)
            for (i in 0 until count) {
                val t = roots[i]
                if (t > 0f && t < 1f) {
                    val qex = qax + 2f * t * abx + t * t * brx
                    val qey = qay + 2f * t * aby + t * t * bry
                    val distance = length(qex, qey)
                    if (distance <= abs(minDistance)) {
                        minDistance =
                            nonZeroSign(cross(abx + t * brx, aby + t * bry, qex, qey)) * distance
                        param = t
                    }
                }
            }
            out.distance = minDistance
            out.dot =
                when {
                    param in 0f..1f -> 0f
                    param < 0.5f -> abs(dotNormalized(directionX(0f), directionY(0f), qax, qay))
                    else ->
                        abs(dotNormalized(directionX(1f), directionY(1f), x2 - p.x, y2 - p.y))
                }
            return param
        }

        fun pseudoDistance(distance: Float, p: Vec, param: Float): Float {
            if (param < 0f) {
                val len = length(directionX(0f), directionY(0f))
                val dx = directionX(0f) / len
                val dy = directionY(0f) / len
                val aqx = p.x - x0
                val aqy = p.y - y0
                if (aqx * dx + aqy * dy < 0f) {
                    val pseudo = cross(aqx, aqy, dx, dy)
                    if (abs(pseudo) <= abs(distance)) return pseudo
                }
            } else if (param > 1f) {
                val len = length(directionX(1f), directionY(1f))
                val dx = directionX(1f) / len
                val dy = directionY(1f) / len
                val bqx = p.x - endX()
                val bqy = p.y - endY()
                if (bqx * dx + bqy * dy > 0f) {
                    val pseudo = cross(bqx, bqy, dx, dy)
                    if (abs(pseudo) <= abs(distance)) return pseudo
                }
            }
            return distance
        }

        fun splitInThirds(): List<Edge> =
            if (type == LINE) {
                val ax = x0 + (x1 - x0) / 3f
                val ay = y0 + (y1 - y0) / 3f
                val bx = x0 + (x1 - x0) * 2f / 3f
                val by = y0 + (y1 - y0) * 2f / 3f
                listOf(line(x0, y0, ax, ay), line(ax, ay, bx, by), line(bx, by, x1, y1))
            } else {
                val third = 1f / 3f
                val twoThirds = 2f / 3f
                val p1x = quadPoint(x0, x1, x2, third)
                val p1y = quadPoint(y0, y1, y2, third)
                val p2x = quadPoint(x0, x1, x2, twoThirds)
                val p2y = quadPoint(y0, y1, y2, twoThirds)
                listOf(
                    quad(x0, y0, lerp(x0, x1, third), lerp(y0, y1, third), p1x, p1y),
                    quad(
                        p1x,
                        p1y,
                        lerp(lerp(x0, x1, 5f / 9f), lerp(x1, x2, 4f / 9f), 0.5f),
                        lerp(lerp(y0, y1, 5f / 9f), lerp(y1, y2, 4f / 9f), 0.5f),
                        p2x,
                        p2y,
                    ),
                    quad(p2x, p2y, lerp(x1, x2, twoThirds), lerp(y1, y2, twoThirds), x2, y2),
                ).onEach { it.color = color }
            }

        companion object {
            const val LINE = 0
            const val QUAD = 1

            fun line(x0: Float, y0: Float, x1: Float, y1: Float) = Edge(LINE, x0, y0, x1, y1, 0f, 0f)

            fun quad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float): Edge {
                if ((x1 == x0 && y1 == y0) || (x1 == x2 && y1 == y2)) {
                    return Edge(QUAD, x0, y0, (x0 + x2) * 0.5f, (y0 + y2) * 0.5f, x2, y2)
                }
                return Edge(QUAD, x0, y0, x1, y1, x2, y2)
            }

            private val roots = FloatArray(3)

            private fun solveCubic(a: Float, b: Float, c: Float, d: Float): Int {
                if (a != 0f) {
                    val bn = b.toDouble() / a
                    if (abs(bn) < 1e6) return solveCubicNormed(bn, c.toDouble() / a, d.toDouble() / a)
                }
                return solveQuadratic(b.toDouble(), c.toDouble(), d.toDouble())
            }

            private fun solveCubicNormed(a: Double, b: Double, c: Double): Int {
                val a2 = a * a
                var q = (a2 - 3.0 * b) / 9.0
                val r = (a * (2.0 * a2 - 9.0 * b) + 27.0 * c) / 54.0
                val r2 = r * r
                val q3 = q * q * q
                val third = a / 3.0
                if (r2 < q3) {
                    val t = acos((r / sqrt(q3)).coerceIn(-1.0, 1.0))
                    q = -2.0 * sqrt(q)
                    roots[0] = (q * cos(t / 3.0) - third).toFloat()
                    roots[1] = (q * cos((t + 2.0 * PI) / 3.0) - third).toFloat()
                    roots[2] = (q * cos((t - 2.0 * PI) / 3.0) - third).toFloat()
                    return 3
                }
                val u = (if (r < 0.0) 1.0 else -1.0) * cbrt(abs(r) + sqrt(r2 - q3))
                val v = if (u == 0.0) 0.0 else q / u
                roots[0] = ((u + v) - third).toFloat()
                if (u == v || abs(u - v) < 1e-12 * abs(u + v)) {
                    roots[1] = (-0.5 * (u + v) - third).toFloat()
                    return 2
                }
                return 1
            }

            private fun solveQuadratic(a: Double, b: Double, c: Double): Int {
                if (a == 0.0 || abs(b) > 1e12 * abs(a)) {
                    if (b == 0.0) return 0
                    roots[0] = (-c / b).toFloat()
                    return 1
                }
                var disc = b * b - 4.0 * a * c
                if (disc > 0.0) {
                    disc = sqrt(disc)
                    roots[0] = ((-b + disc) / (2.0 * a)).toFloat()
                    roots[1] = ((-b - disc) / (2.0 * a)).toFloat()
                    return 2
                }
                if (disc == 0.0) {
                    roots[0] = (-b / (2.0 * a)).toFloat()
                    return 1
                }
                return 0
            }
        }
    }

    private fun colorEdges(contour: MutableList<Edge>, angleThreshold: Float) {
        if (contour.isEmpty()) return
        val crossThreshold = sin(angleThreshold)
        val corners = mutableListOf<Int>()
        var prevX = contour.last().directionX(1f)
        var prevY = contour.last().directionY(1f)
        contour.forEachIndexed { i, edge ->
            if (isCorner(prevX, prevY, edge.directionX(0f), edge.directionY(0f), crossThreshold)) {
                corners += i
            }
            prevX = edge.directionX(1f)
            prevY = edge.directionY(1f)
        }
        seed = 0L
        when {
            corners.isEmpty() -> contour.forEach { it.color = WHITE }
            corners.size == 1 -> {
                val c0 = switchColor(WHITE, 0)
                val c2 = switchColor(c0, 0)
                val colors = intArrayOf(c0, WHITE, c2)
                val corner = corners[0]
                if (contour.size >= 3) {
                    val m = contour.size
                    for (i in 0 until m) {
                        contour[(corner + i) % m].color =
                            colors[(3 + 2.875f * i / (m - 1) - 1.4375f + 0.5f).toInt() - 2]
                    }
                } else {
                    val parts = mutableListOf<Edge>()
                    if (contour.size == 2 && corner == 1) {
                        parts += contour[1].splitInThirds()
                        parts += contour[0].splitInThirds()
                    } else {
                        contour.forEach { parts += it.splitInThirds() }
                    }
                    if (contour.size == 1) {
                        parts[0].color = colors[0]
                        parts[1].color = colors[1]
                        parts[2].color = colors[2]
                    } else {
                        val order = intArrayOf(0, 0, 1, 1, 2, 2)
                        parts.forEachIndexed { i, part -> part.color = colors[order[i]] }
                    }
                    contour.clear()
                    contour += parts
                }
            }
            else -> {
                val cornerCount = corners.size
                var spline = 0
                val start = corners[0]
                val m = contour.size
                var color = switchColor(WHITE, 0)
                val initialColor = color
                for (i in 0 until m) {
                    val index = (start + i) % m
                    if (spline + 1 < cornerCount && corners[spline + 1] == index) {
                        spline++
                        color =
                            switchColor(color, if (spline == cornerCount - 1) initialColor else BLACK)
                    }
                    contour[index].color = color
                }
            }
        }
    }

    private var seed = 0L

    private fun switchColor(color: Int, banned: Int): Int {
        val combined = color and banned
        if (combined == RED || combined == GREEN || combined == BLUE) return combined xor WHITE
        if (color == BLACK || color == WHITE) {
            val start = intArrayOf(CYAN, MAGENTA, YELLOW)
            val result = start[(seed % 3).toInt()]
            seed /= 3
            return result
        }
        val shifted = color shl (1 + (seed and 1L).toInt())
        seed = seed shr 1
        return (shifted or (shifted shr 3)) and WHITE
    }

    private fun isCorner(ax: Float, ay: Float, bx: Float, by: Float, crossThreshold: Float): Boolean {
        val al = length(ax, ay)
        val bl = length(bx, by)
        if (al == 0f || bl == 0f) return false
        val nax = ax / al
        val nay = ay / al
        val nbx = bx / bl
        val nby = by / bl
        return nax * nbx + nay * nby <= 0f || abs(cross(nax, nay, nbx, nby)) > crossThreshold
    }

    private companion object {
        const val BLACK = 0
        const val RED = 1
        const val GREEN = 2
        const val YELLOW = 3
        const val BLUE = 4
        const val MAGENTA = 5
        const val CYAN = 6
        const val WHITE = 7
        const val ANGLE_THRESHOLD = 3f
        const val CLASH_THRESHOLD = 1.001f
        const val CUBIC_SPLITS = 4

        fun median(a: Float, b: Float, c: Float): Float = max(min(a, b), min(max(a, b), c))

        fun length(x: Float, y: Float): Float = sqrt(x * x + y * y)

        fun cross(ax: Float, ay: Float, bx: Float, by: Float): Float = ax * by - ay * bx

        fun nonZeroSign(v: Float): Float = if (v > 0f) 1f else -1f


        fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        fun quadPoint(p0: Float, p1: Float, p2: Float, t: Float): Float {
            val mt = 1f - t
            return mt * mt * p0 + 2f * mt * t * p1 + t * t * p2
        }

        fun dotNormalized(ax: Float, ay: Float, bx: Float, by: Float): Float {
            val al = length(ax, ay)
            val bl = length(bx, by)
            if (al == 0f || bl == 0f) return 0f
            return (ax * bx + ay * by) / (al * bl)
        }
    }
}
